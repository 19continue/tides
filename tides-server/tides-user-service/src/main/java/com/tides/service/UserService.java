package com.tides.service;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tides.client.BaseDataClient;
import com.tides.common.ApiResponse;
import com.tides.core.RedisKeyManage;
import com.tides.dto.GetChannelDataByCodeDto;
import com.tides.dto.UserAuthenticationDto;
import com.tides.dto.UserExistDto;
import com.tides.dto.UserGetAndTicketUserListDto;
import com.tides.dto.UserIdDto;
import com.tides.dto.UserLoginDto;
import com.tides.dto.UserLogoutDto;
import com.tides.dto.UserMobileDto;
import com.tides.dto.UserRegisterDto;
import com.tides.dto.SimulatorUserPrepareDto;
import com.tides.dto.UserUpdateDto;
import com.tides.dto.UserUpdateEmailDto;
import com.tides.dto.UserUpdateMobileDto;
import com.tides.dto.UserUpdatePasswordDto;
import com.tides.entity.TicketUser;
import com.tides.entity.User;
import com.tides.entity.UserEmail;
import com.tides.entity.UserMobile;
import com.tides.enums.BaseCode;
import com.tides.enums.BusinessStatus;
import com.tides.enums.CompositeCheckType;
import com.tides.exception.TidesFrameException;
import com.tides.handler.BloomFilterHandler;
import com.tides.initialize.impl.composite.CompositeContainer;
import com.tides.jwt.TokenUtil;
import com.tides.mapper.TicketUserMapper;
import com.tides.mapper.UserEmailMapper;
import com.tides.mapper.UserMapper;
import com.tides.mapper.UserMobileMapper;
import com.tides.redis.RedisCache;
import com.tides.redis.RedisKeyBuild;
import com.tides.servicelock.LockType;
import com.tides.servicelock.annotion.ServiceLock;
import com.tides.util.StringUtil;
import com.tides.vo.GetChannelDataVo;
import com.tides.vo.SimulatorPreparedUserVo;
import com.tides.vo.SimulatorUserPrepareVo;
import com.tides.vo.TicketUserVo;
import com.tides.vo.UserGetAndTicketUserListVo;
import com.tides.vo.UserLoginVo;
import com.tides.vo.UserVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tides.core.DistributedLockConstants.REGISTER_USER_LOCK;

/**
 * @description: 用户 service
 * @author: 19continue
 **/
@Slf4j
@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private UserMobileMapper userMobileMapper;
    
    @Autowired
    private UserEmailMapper userEmailMapper;
    
    @Autowired
    private UidGenerator uidGenerator;
    
    @Autowired
    private RedisCache redisCache;
    
    @Autowired
    private TicketUserMapper ticketUserMapper;
    
    @Autowired
    private BloomFilterHandler bloomFilterHandler;
    
    @Autowired
    private CompositeContainer compositeContainer;
    
    @Autowired
    private BaseDataClient baseDataClient;
    
    @Value("${token.expire.time:40}")
    private Long tokenExpireTime;

    @Value("${ticket-rush-simulator.user-token:tides-demo-local}")
    private String simulatorUserToken;
    
    private static final Integer ERROR_COUNT_THRESHOLD = 5;

    private static final int MAX_SIMULATOR_USER_COUNT = 20000;

    private static final int MAX_SIMULATOR_TICKET_USER_COUNT = 5;

    private static final DateTimeFormatter BIRTHDAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final int[] ID_CARD_WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    private static final char[] ID_CARD_CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
    
    @Transactional(rollbackFor = Exception.class)
    @ServiceLock(lockType= LockType.Write,name = REGISTER_USER_LOCK,keys = {"#userRegisterDto.mobile"})
    public Boolean register(UserRegisterDto userRegisterDto) {
        compositeContainer.execute(CompositeCheckType.USER_REGISTER_CHECK.getValue(),userRegisterDto);
        log.info("注册手机号:{}",userRegisterDto.getMobile());
        //用户表添加
        User user = new User();
        BeanUtils.copyProperties(userRegisterDto,user);
        user.setId(uidGenerator.getUid());
        userMapper.insert(user);
        //用户手机表添加
        UserMobile userMobile = new UserMobile();
        userMobile.setId(uidGenerator.getUid());
        userMobile.setUserId(user.getId());
        userMobile.setMobile(userRegisterDto.getMobile());
        userMobileMapper.insert(userMobile);
        bloomFilterHandler.add(userMobile.getMobile());
        return true;
    }
    
    @ServiceLock(lockType= LockType.Read,name = REGISTER_USER_LOCK,keys = {"#userExistDto.mobile"})
    public void exist(UserExistDto userExistDto){
        doExist(userExistDto.getMobile());
    }
    
    public void doExist(String mobile){
        boolean contains = bloomFilterHandler.contains(mobile);
        if (contains) {
            LambdaQueryWrapper<UserMobile> queryWrapper = Wrappers.lambdaQuery(UserMobile.class)
                    .eq(UserMobile::getMobile, mobile);
            UserMobile userMobile = userMobileMapper.selectOne(queryWrapper);
            if (Objects.nonNull(userMobile)) {
                throw new TidesFrameException(BaseCode.USER_EXIST);
            }
        }
    }
    
    /**
     * 登录
     * @param userLoginDto 登录入参
     * @return 用户信息
     * */
    public UserLoginVo login(UserLoginDto userLoginDto) {
        UserLoginVo userLoginVo = new UserLoginVo();
        String code = userLoginDto.getCode();
        String mobile = userLoginDto.getMobile();
        String email = userLoginDto.getEmail();
        String password = userLoginDto.getPassword();
        if (StringUtil.isEmpty(mobile) && StringUtil.isEmpty(email)) {
            throw new TidesFrameException(BaseCode.USER_MOBILE_AND_EMAIL_NOT_EXIST);
        }
        Long userId;
        if (StringUtil.isNotEmpty(mobile)) {
            String errorCountStr = 
                    redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.LOGIN_USER_MOBILE_ERROR, mobile), String.class);
            if (StringUtil.isNotEmpty(errorCountStr) && Integer.parseInt(errorCountStr) >= ERROR_COUNT_THRESHOLD) {
                throw new TidesFrameException(BaseCode.MOBILE_ERROR_COUNT_TOO_MANY);
            }
            LambdaQueryWrapper<UserMobile> queryWrapper = Wrappers.lambdaQuery(UserMobile.class)
                    .eq(UserMobile::getMobile, mobile);
            UserMobile userMobile = userMobileMapper.selectOne(queryWrapper);
            if (Objects.isNull(userMobile)) {
                redisCache.incrBy(RedisKeyBuild.createRedisKey(RedisKeyManage.LOGIN_USER_MOBILE_ERROR,mobile),1);
                redisCache.expire(RedisKeyBuild.createRedisKey(RedisKeyManage.LOGIN_USER_MOBILE_ERROR,mobile),1,TimeUnit.MINUTES);
                throw new TidesFrameException(BaseCode.USER_MOBILE_EMPTY);
            }
            userId = userMobile.getUserId();
        }else {
            String errorCountStr = 
                    redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.LOGIN_USER_EMAIL_ERROR, email), String.class);
            if (StringUtil.isNotEmpty(errorCountStr) && Integer.parseInt(errorCountStr) >= ERROR_COUNT_THRESHOLD) {
                throw new TidesFrameException(BaseCode.EMAIL_ERROR_COUNT_TOO_MANY);
            }
            LambdaQueryWrapper<UserEmail> queryWrapper = Wrappers.lambdaQuery(UserEmail.class)
                    .eq(UserEmail::getEmail, email);
            UserEmail userEmail = userEmailMapper.selectOne(queryWrapper);
            if (Objects.isNull(userEmail)) {
                redisCache.incrBy(RedisKeyBuild.createRedisKey(RedisKeyManage.LOGIN_USER_EMAIL_ERROR,email),1);
                redisCache.expire(RedisKeyBuild.createRedisKey(RedisKeyManage.LOGIN_USER_EMAIL_ERROR,email),1,TimeUnit.MINUTES);
                throw new TidesFrameException(BaseCode.USER_EMAIL_NOT_EXIST);
            }
            userId = userEmail.getUserId();
        }
        LambdaQueryWrapper<User> queryUserWrapper = Wrappers.lambdaQuery(User.class)
                .eq(User::getId, userId).eq(User::getPassword, password);
        User user = userMapper.selectOne(queryUserWrapper);
        if (Objects.isNull(user)) {
            throw new TidesFrameException(BaseCode.NAME_PASSWORD_ERROR);
        }
        redisCache.set(RedisKeyBuild.createRedisKey(RedisKeyManage.USER_LOGIN,code,user.getId()),user,
                tokenExpireTime,TimeUnit.MINUTES);
        userLoginVo.setUserId(userId);
        userLoginVo.setToken(createToken(user.getId(),getChannelDataByCode(code).getTokenSecret()));
        return userLoginVo;
    }
    
    private GetChannelDataVo getChannelDataByRedis(String code){
        return redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.CHANNEL_DATA,code),GetChannelDataVo.class);
    }
    
    private GetChannelDataVo getChannelDataByClient(String code){
        GetChannelDataByCodeDto getChannelDataByCodeDto = new GetChannelDataByCodeDto();
        getChannelDataByCodeDto.setCode(code);
        ApiResponse<GetChannelDataVo> getChannelDataApiResponse = baseDataClient.getByCode(getChannelDataByCodeDto);
        if (Objects.equals(getChannelDataApiResponse.getCode(), BaseCode.SUCCESS.getCode())) {
            return getChannelDataApiResponse.getData();
        }
        throw new TidesFrameException("没有找到ChannelData");
    }
    
    public String createToken(Long userId,String tokenSecret){
        Map<String,Object> map = new HashMap<>(4);
        map.put("userId",userId);
        return TokenUtil.createToken(String.valueOf(uidGenerator.getUid()), JSON.toJSONString(map),tokenExpireTime * 60 * 1000,tokenSecret);
    }
    
    public Boolean logout(UserLogoutDto userLogoutDto) {
        String userStr = TokenUtil.parseToken(userLogoutDto.getToken(),getChannelDataByCode(userLogoutDto.getCode())
                .getTokenSecret());
        if (StringUtil.isEmpty(userStr)) {
            throw new TidesFrameException(BaseCode.USER_EMPTY);
        }
        String userId = JSONObject.parseObject(userStr).getString("userId");
        redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.USER_LOGIN,userLogoutDto.getCode(),userId));
        return true;
    }
    
    public GetChannelDataVo getChannelDataByCode(String code){
        GetChannelDataVo channelDataVo = getChannelDataByRedis(code);
        if (Objects.isNull(channelDataVo)) {
            channelDataVo = getChannelDataByClient(code);
        }
        return channelDataVo;
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void update(UserUpdateDto userUpdateDto){
        User user = userMapper.selectById(userUpdateDto.getId());
        if (Objects.isNull(user)) {
            throw new TidesFrameException(BaseCode.USER_EMPTY);
        }
        User updateUser = new User();
        BeanUtil.copyProperties(userUpdateDto,updateUser);
        userMapper.updateById(updateUser);
    }
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(UserUpdatePasswordDto userUpdatePasswordDto){
        User user = userMapper.selectById(userUpdatePasswordDto.getId());
        if (Objects.isNull(user)) {
            throw new TidesFrameException(BaseCode.USER_EMPTY);
        }
        User updateUser = new User();
        BeanUtil.copyProperties(userUpdatePasswordDto,updateUser);
        userMapper.updateById(updateUser);
    }
    @Transactional(rollbackFor = Exception.class)
    public void updateEmail(UserUpdateEmailDto userUpdateEmailDto){
        User user = userMapper.selectById(userUpdateEmailDto.getId());
        if (Objects.isNull(user)) {
            throw new TidesFrameException(BaseCode.USER_EMPTY);
        }
        User updateUser = new User();
        BeanUtil.copyProperties(userUpdateEmailDto,updateUser);
        updateUser.setEmailStatus(BusinessStatus.YES.getCode());
        userMapper.updateById(updateUser);
        
        String oldEmail = user.getEmail();
        LambdaQueryWrapper<UserEmail> userEmailLambdaQueryWrapper = Wrappers.lambdaQuery(UserEmail.class)
                .eq(UserEmail::getEmail, userUpdateEmailDto.getEmail());
        UserEmail userEmail = userEmailMapper.selectOne(userEmailLambdaQueryWrapper);
        if (Objects.isNull(userEmail)) {
            userEmail = new UserEmail();
            userEmail.setId(uidGenerator.getUid());
            userEmail.setUserId(user.getId());
            userEmail.setEmail(userUpdateEmailDto.getEmail());
            userEmailMapper.insert(userEmail);
        }else {
            LambdaUpdateWrapper<UserEmail> userEmailLambdaUpdateWrapper = Wrappers.lambdaUpdate(UserEmail.class)
                    .eq(UserEmail::getEmail, oldEmail);
            UserEmail updateUserEmail = new UserEmail();
            updateUserEmail.setEmail(userUpdateEmailDto.getEmail());
            userEmailMapper.update(updateUserEmail,userEmailLambdaUpdateWrapper);
        }
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void updateMobile(UserUpdateMobileDto userUpdateMobileDto){
        User user = userMapper.selectById(userUpdateMobileDto.getId());
        if (Objects.isNull(user)) {
            throw new TidesFrameException(BaseCode.USER_EMPTY);
        }
        String oldMobile = user.getMobile();
        User updateUser = new User();
        BeanUtil.copyProperties(userUpdateMobileDto,updateUser);
        userMapper.updateById(updateUser);
        LambdaQueryWrapper<UserMobile> userMobileLambdaQueryWrapper = Wrappers.lambdaQuery(UserMobile.class)
                .eq(UserMobile::getMobile, userUpdateMobileDto.getMobile());
        UserMobile userMobile = userMobileMapper.selectOne(userMobileLambdaQueryWrapper);
        if (Objects.isNull(userMobile)) {
            userMobile = new UserMobile();
            userMobile.setId(uidGenerator.getUid());
            userMobile.setUserId(user.getId());
            userMobile.setMobile(userUpdateMobileDto.getMobile());
            userMobileMapper.insert(userMobile);
        }else {
            LambdaUpdateWrapper<UserMobile> userMobileLambdaUpdateWrapper = Wrappers.lambdaUpdate(UserMobile.class)
                    .eq(UserMobile::getMobile, oldMobile);
            UserMobile updateUserMobile = new UserMobile();
            updateUserMobile.setMobile(userUpdateMobileDto.getMobile());
            userMobileMapper.update(updateUserMobile,userMobileLambdaUpdateWrapper);
        }
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void authentication(UserAuthenticationDto userAuthenticationDto){
        User user = userMapper.selectById(userAuthenticationDto.getId());
        if (Objects.isNull(user)) {
            throw new TidesFrameException(BaseCode.USER_EMPTY);
        }
        if (Objects.equals(user.getRelAuthenticationStatus(), BusinessStatus.YES.getCode())) {
            throw new TidesFrameException(BaseCode.USER_AUTHENTICATION);
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setRelName(userAuthenticationDto.getRelName());
        updateUser.setIdNumber(userAuthenticationDto.getIdNumber());
        updateUser.setRelAuthenticationStatus(BusinessStatus.YES.getCode());
        userMapper.updateById(updateUser);
    }
    
    public UserVo getByMobile(UserMobileDto userMobileDto) {
        LambdaQueryWrapper<UserMobile> queryWrapper = Wrappers.lambdaQuery(UserMobile.class)
                .eq(UserMobile::getMobile, userMobileDto.getMobile());
        UserMobile userMobile = userMobileMapper.selectOne(queryWrapper);
        if (Objects.isNull(userMobile)) {
            throw new TidesFrameException(BaseCode.USER_MOBILE_EMPTY);
        }
        User user = userMapper.selectById(userMobile.getUserId());
        if (Objects.isNull(user)) {
            throw new TidesFrameException(BaseCode.USER_EMPTY);
        }
        UserVo userVo = new UserVo();
        BeanUtil.copyProperties(user,userVo);
        userVo.setMobile(userMobile.getMobile());
        return userVo;
    }
    
    public UserVo getById(UserIdDto userIdDto) {
        User user = userMapper.selectById(userIdDto.getId());
        if (Objects.isNull(user)) {
            throw new TidesFrameException(BaseCode.USER_EMPTY);
        }
        UserVo userVo = new UserVo();
        BeanUtil.copyProperties(user,userVo);
        return userVo;
    }
    
    public UserGetAndTicketUserListVo getUserAndTicketUserList(final UserGetAndTicketUserListDto userGetAndTicketUserListDto) {
        UserIdDto userIdDto = new UserIdDto();
        userIdDto.setId(userGetAndTicketUserListDto.getUserId());
        UserVo userVo = getById(userIdDto);
        
        LambdaQueryWrapper<TicketUser> ticketUserLambdaQueryWrapper = Wrappers.lambdaQuery(TicketUser.class)
                .eq(TicketUser::getUserId, userGetAndTicketUserListDto.getUserId());
        List<TicketUser> ticketUserList = ticketUserMapper.selectList(ticketUserLambdaQueryWrapper);
        List<TicketUserVo> ticketUserVoList = BeanUtil.copyToList(ticketUserList, TicketUserVo.class);
        
        UserGetAndTicketUserListVo userGetAndTicketUserListVo = new UserGetAndTicketUserListVo();
        userGetAndTicketUserListVo.setUserVo(userVo);
        userGetAndTicketUserListVo.setTicketUserVoList(ticketUserVoList);
        return userGetAndTicketUserListVo;
    }

    @Transactional(rollbackFor = Exception.class)
    public SimulatorUserPrepareVo prepareSimulatorUsers(String requestToken, SimulatorUserPrepareDto prepareDto) {
        if (!Objects.equals(simulatorUserToken, requestToken)) {
            throw new TidesFrameException(BaseCode.API_CALL_PASSWORD_ERROR);
        }
        SimulatorUserPrepareDto dto = Objects.isNull(prepareDto) ? new SimulatorUserPrepareDto() : prepareDto;
        int count = normalizeRange(dto.getCount(), 100, 1, MAX_SIMULATOR_USER_COUNT, "用户数量");
        int ticketUserCount = normalizeRange(dto.getTicketUserCount(), 1, 1, MAX_SIMULATOR_TICKET_USER_COUNT, "购票人数量");
        long startIndex = Objects.isNull(dto.getStartIndex()) ? 1L : Math.max(0L, dto.getStartIndex());
        String mobilePrefix = normalizeMobilePrefix(dto.getMobilePrefix(), count, startIndex);
        String emailDomain = normalizeEmailDomain(dto.getEmailDomain());
        String password = StringUtil.isEmpty(dto.getPassword()) ? "111111" : dto.getPassword();
        String relNamePrefix = StringUtil.isEmpty(dto.getRelNamePrefix()) ? "压测购票人" : dto.getRelNamePrefix();
        boolean overwritePassword = Boolean.TRUE.equals(dto.getOverwritePassword());

        List<SimulatorPreparedUserVo> preparedUserList = new ArrayList<>(count);
        int createdCount = 0;
        int reusedCount = 0;
        int ticketUserCreatedCount = 0;
        for (int i = 0; i < count; i++) {
            long index = startIndex + i;
            String mobile = buildMobile(mobilePrefix, index);
            String email = buildEmail(emailDomain, index);
            PreparedUserResult preparedUserResult = prepareOneSimulatorUser(
                    index, mobile, email, password, relNamePrefix, ticketUserCount, overwritePassword);
            if (preparedUserResult.created()) {
                createdCount++;
            } else {
                reusedCount++;
            }
            ticketUserCreatedCount += preparedUserResult.ticketUserCreatedCount();
            preparedUserList.add(preparedUserResult.userVo());
        }

        SimulatorUserPrepareVo simulatorUserPrepareVo = new SimulatorUserPrepareVo();
        simulatorUserPrepareVo.setRequestedCount(count);
        simulatorUserPrepareVo.setCreatedCount(createdCount);
        simulatorUserPrepareVo.setReusedCount(reusedCount);
        simulatorUserPrepareVo.setTicketUserCreatedCount(ticketUserCreatedCount);
        simulatorUserPrepareVo.setTicketUserCount(ticketUserCount);
        simulatorUserPrepareVo.setUserList(preparedUserList);
        return simulatorUserPrepareVo;
    }

    private PreparedUserResult prepareOneSimulatorUser(long index, String mobile, String email, String password,
                                                       String relNamePrefix, int ticketUserCount,
                                                       boolean overwritePassword) {
        boolean created = false;
        UserMobile userMobile = selectFirstUserMobile(mobile);
        User user;
        if (Objects.nonNull(userMobile)) {
            user = userMapper.selectById(userMobile.getUserId());
            if (Objects.isNull(user)) {
                throw new TidesFrameException(BaseCode.USER_EMPTY);
            }
            if (overwritePassword) {
                User updateUser = new User();
                updateUser.setId(user.getId());
                updateUser.setPassword(password);
                userMapper.updateById(updateUser);
                user.setPassword(password);
            }
        } else {
            user = createSimulatorUser(index, mobile, email, password, relNamePrefix);
            userMobile = new UserMobile();
            userMobile.setId(uidGenerator.getUid());
            userMobile.setUserId(user.getId());
            userMobile.setMobile(mobile);
            userMobile.setStatus(BusinessStatus.YES.getCode());
            userMobileMapper.insert(userMobile);
            bloomFilterHandler.add(mobile);
            created = true;
        }

        ensureUserEmail(user.getId(), email);
        TicketUserPrepareResult ticketUserPrepareResult = ensureTicketUsers(user.getId(), index, relNamePrefix,
                ticketUserCount);

        SimulatorPreparedUserVo simulatorPreparedUserVo = new SimulatorPreparedUserVo();
        simulatorPreparedUserVo.setUserId(String.valueOf(user.getId()));
        simulatorPreparedUserVo.setMobile(mobile);
        simulatorPreparedUserVo.setEmail(email);
        simulatorPreparedUserVo.setPassword(password);
        simulatorPreparedUserVo.setTicketUserIds(ticketUserPrepareResult.ticketUserIds());
        return new PreparedUserResult(simulatorPreparedUserVo, created, ticketUserPrepareResult.createdCount());
    }

    private User createSimulatorUser(long index, String mobile, String email, String password, String relNamePrefix) {
        User user = new User();
        user.setId(uidGenerator.getUid());
        user.setName("压测用户" + index);
        user.setRelName(relNamePrefix + index);
        user.setMobile(mobile);
        user.setGender(index % 2 == 0 ? 2 : 1);
        user.setPassword(password);
        user.setEmailStatus(BusinessStatus.YES.getCode());
        user.setEmail(email);
        user.setRelAuthenticationStatus(BusinessStatus.YES.getCode());
        user.setIdNumber(buildIdCardNumber(index));
        user.setAddress("演示压测环境");
        user.setStatus(BusinessStatus.YES.getCode());
        userMapper.insert(user);
        return user;
    }

    private void ensureUserEmail(Long userId, String email) {
        UserEmail oldUserEmail = selectFirstUserEmail(email);
        if (Objects.nonNull(oldUserEmail)) {
            return;
        }
        UserEmail userEmail = new UserEmail();
        userEmail.setId(uidGenerator.getUid());
        userEmail.setUserId(userId);
        userEmail.setEmail(email);
        userEmail.setStatus(BusinessStatus.YES.getCode());
        userEmailMapper.insert(userEmail);
    }

    private TicketUserPrepareResult ensureTicketUsers(Long userId, long userIndex, String relNamePrefix,
                                                      int ticketUserCount) {
        List<TicketUser> ticketUserList = ticketUserMapper.selectList(Wrappers.lambdaQuery(TicketUser.class)
                .eq(TicketUser::getUserId, userId)
                .orderByAsc(TicketUser::getId));
        List<String> ticketUserIds = ticketUserList.stream()
                .map(TicketUser::getId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toCollection(ArrayList::new));
        int createdCount = 0;
        for (int i = ticketUserIds.size(); i < ticketUserCount; i++) {
            long ticketIndex = userIndex * 10 + i + 1;
            String idNumber = buildIdCardNumber(ticketIndex);
            TicketUser oldTicketUser = selectFirstTicketUser(userId, idNumber);
            if (Objects.nonNull(oldTicketUser)) {
                ticketUserIds.add(String.valueOf(oldTicketUser.getId()));
                continue;
            }
            TicketUser ticketUser = new TicketUser();
            ticketUser.setId(uidGenerator.getUid());
            ticketUser.setUserId(userId);
            ticketUser.setRelName(relNamePrefix + userIndex + "-" + (i + 1));
            ticketUser.setIdType(1);
            ticketUser.setIdNumber(idNumber);
            ticketUser.setStatus(BusinessStatus.YES.getCode());
            ticketUserMapper.insert(ticketUser);
            ticketUserIds.add(String.valueOf(ticketUser.getId()));
            createdCount++;
        }
        redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.TICKET_USER_LIST, userId));
        return new TicketUserPrepareResult(ticketUserIds.stream().limit(ticketUserCount).toList(), createdCount);
    }

    private UserMobile selectFirstUserMobile(String mobile) {
        List<UserMobile> userMobileList = userMobileMapper.selectList(Wrappers.lambdaQuery(UserMobile.class)
                .eq(UserMobile::getMobile, mobile)
                .last("limit 1"));
        return userMobileList.isEmpty() ? null : userMobileList.get(0);
    }

    private UserEmail selectFirstUserEmail(String email) {
        List<UserEmail> userEmailList = userEmailMapper.selectList(Wrappers.lambdaQuery(UserEmail.class)
                .eq(UserEmail::getEmail, email)
                .last("limit 1"));
        return userEmailList.isEmpty() ? null : userEmailList.get(0);
    }

    private TicketUser selectFirstTicketUser(Long userId, String idNumber) {
        List<TicketUser> ticketUserList = ticketUserMapper.selectList(Wrappers.lambdaQuery(TicketUser.class)
                .eq(TicketUser::getUserId, userId)
                .eq(TicketUser::getIdType, 1)
                .eq(TicketUser::getIdNumber, idNumber)
                .last("limit 1"));
        return ticketUserList.isEmpty() ? null : ticketUserList.get(0);
    }

    private int normalizeRange(Integer value, int defaultValue, int min, int max, String name) {
        int result = Objects.isNull(value) ? defaultValue : value;
        if (result < min || result > max) {
            throw new TidesFrameException(name + "必须在 " + min + " 到 " + max + " 之间");
        }
        return result;
    }

    private String normalizeMobilePrefix(String mobilePrefix, int count, long startIndex) {
        String prefix = StringUtil.isEmpty(mobilePrefix) ? "19099" : mobilePrefix.replaceAll("\\D", "");
        if (StringUtil.isEmpty(prefix)) {
            prefix = "19099";
        }
        if (!prefix.startsWith("1")) {
            throw new TidesFrameException("手机号前缀必须以 1 开头");
        }
        if (prefix.length() >= 11) {
            throw new TidesFrameException("手机号前缀必须少于 11 位");
        }
        int suffixLength = 11 - prefix.length();
        long capacity = 1L;
        for (int i = 0; i < suffixLength; i++) {
            capacity *= 10L;
        }
        if (startIndex + count > capacity) {
            throw new TidesFrameException("手机号前缀可生成的号码数量不足，请缩短前缀或调小起始序号");
        }
        return prefix;
    }

    private String normalizeEmailDomain(String emailDomain) {
        String domain = StringUtil.isEmpty(emailDomain) ? "sandbox.local" : emailDomain.trim();
        return domain.startsWith("@") ? domain.substring(1) : domain;
    }

    private String buildMobile(String mobilePrefix, long index) {
        int suffixLength = 11 - mobilePrefix.length();
        return mobilePrefix + String.format("%0" + suffixLength + "d", index);
    }

    private String buildEmail(String emailDomain, long index) {
        return "tides-rush-" + String.format("%08d", index) + "@" + emailDomain;
    }

    private String buildIdCardNumber(long seed) {
        long normalizedSeed = Math.max(0L, seed);
        LocalDate birthday = LocalDate.of(1970, 1, 1).plusDays(normalizedSeed % 14000);
        int sequence = (int) ((normalizedSeed / 14000) % 900) + 100;
        String body = "330100" + birthday.format(BIRTHDAY_FORMATTER) + sequence;
        int sum = 0;
        for (int i = 0; i < body.length(); i++) {
            sum += (body.charAt(i) - '0') * ID_CARD_WEIGHTS[i];
        }
        return body + ID_CARD_CHECK_CODES[sum % 11];
    }

    private record PreparedUserResult(SimulatorPreparedUserVo userVo, boolean created, int ticketUserCreatedCount) {
    }

    private record TicketUserPrepareResult(List<String> ticketUserIds, int createdCount) {
    }
    
    public List<String> getAllMobile(){
        QueryWrapper<User> lambdaQueryWrapper = Wrappers.emptyWrapper();
        List<User> users = userMapper.selectList(lambdaQueryWrapper);
        return users.stream().map(User::getMobile).collect(Collectors.toList());
    }
}
