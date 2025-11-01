package com.tides.service;

import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tides.client.ApiDataClient;
import com.tides.core.RedisKeyManage;
import com.tides.dto.AddApiDataDto;
import com.tides.dto.ChannelDataAddDto;
import com.tides.dto.GetChannelDataByCodeDto;
import com.tides.entity.ChannelTableData;
import com.tides.enums.Status;
import com.tides.mapper.ChannelDataMapper;
import com.tides.redis.RedisCache;
import com.tides.redis.RedisKeyBuild;
import com.tides.util.DateUtils;
import com.tides.vo.GetChannelDataVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * @description: 渠道 service
 * @author: 19continue
 **/
@Service
@Slf4j
public class ChannelDataService {
    
    @Autowired
    private ChannelDataMapper channelDataMapper;
    
    @Autowired
    private UidGenerator uidGenerator;
    
    @Autowired
    private RedisCache redisCache;
    
    @Autowired
    private ApiDataClient apiDataClient;
    
    public GetChannelDataVo getByCode(GetChannelDataByCodeDto dto){
        GetChannelDataVo getChannelDataVo = new GetChannelDataVo();
        LambdaQueryWrapper<ChannelTableData> wrapper = Wrappers.lambdaQuery(ChannelTableData.class)
                .eq(ChannelTableData::getStatus, Status.RUN.getCode())
                .eq(ChannelTableData::getCode,dto.getCode());
        Optional.ofNullable(channelDataMapper.selectOne(wrapper)).ifPresent(channelData -> {
            BeanUtils.copyProperties(channelData,getChannelDataVo);
        });
        return getChannelDataVo;
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void add(ChannelDataAddDto channelDataAddDto) {
        ChannelTableData channelData = new ChannelTableData();
        BeanUtils.copyProperties(channelDataAddDto,channelData);
        channelData.setId(uidGenerator.getUid());
        channelData.setCreateTime(DateUtils.now());
        channelDataMapper.insert(channelData);
        addRedisChannelData(channelData);
    }
    
    private void addRedisChannelData(ChannelTableData channelData){
        GetChannelDataVo getChannelDataVo = new GetChannelDataVo();
        BeanUtils.copyProperties(channelData,getChannelDataVo);
        redisCache.set(RedisKeyBuild.createRedisKey(RedisKeyManage.CHANNEL_DATA,getChannelDataVo.getCode()),getChannelDataVo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void test(final ChannelDataAddDto channelDataAddDto) {
        add(channelDataAddDto);
        AddApiDataDto apiDataDto = new AddApiDataDto();
        apiDataDto.setHeadVersion("1.0");
        apiDataClient.add(apiDataDto);
        if ("2".equals(channelDataAddDto.getCode())) {
            throw new RuntimeException("测试异常");
        }
    }
}
