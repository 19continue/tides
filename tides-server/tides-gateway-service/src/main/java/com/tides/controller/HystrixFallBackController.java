package com.tides.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 熔断
 * @author: 19continue
 **/
@RestController
public class HystrixFallBackController {

    @RequestMapping(value = "/fallBackHandler")
    public String fallBackHandler(){
        return "熔断器执行";
    }
}
