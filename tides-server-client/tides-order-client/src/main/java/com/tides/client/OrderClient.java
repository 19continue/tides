package com.tides.client;

import com.tides.common.ApiResponse;
import com.tides.dto.AccountOrderCountDto;
import com.tides.dto.MovieOrderSalesCountDto;
import com.tides.dto.OrderCreateDto;
import com.tides.vo.AccountOrderCountVo;
import com.tides.vo.MovieOrderSalesCountVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

import static com.tides.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;

/**
 * @description: 订单服务 feign
 * @author: 19continue
 **/
@Component
@FeignClient(value = SPRING_INJECT_PREFIX_DISTINCTION_NAME+"-"+"order-service",fallback = OrderClientFallback.class)
public interface OrderClient {
    
    /**
     * 创建订单
     * @param dto 参数
     * @return 结果
     * */
    @PostMapping("/order/create")
    ApiResponse<String> create(OrderCreateDto dto);
    
    /**
     * 账户下某个节目的订单数量
     * @param dto 参数
     * @return 结果
     * */
    @PostMapping("/order/account/order/count")
    ApiResponse<AccountOrderCountVo> accountOrderCount(AccountOrderCountDto dto);

    /**
     * 电影真实支付销量，按已支付订单购票人明细聚合。
     * @param dto 参数
     * @return 结果
     * */
    @PostMapping("/order/movie/sales/count")
    ApiResponse<List<MovieOrderSalesCountVo>> movieSalesCount(MovieOrderSalesCountDto dto);
    
    /**
     * 重置虚拟分片路由缓存
     * @return 结果
     * */
    @PostMapping(value = "/order/reload/route/mapping/cache")
    ApiResponse<Void> reloadRouteMappingCache();
}
