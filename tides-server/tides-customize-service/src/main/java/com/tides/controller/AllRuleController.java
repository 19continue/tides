package com.tides.controller;

import com.tides.common.ApiResponse;
import com.tides.dto.AllRuleDto;
import com.tides.service.AllRuleService;
import com.tides.vo.AllDepthRuleVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 所有规则 控制层
 * @author: 19continue
 **/
@RestController
@RequestMapping("/allRule")
@Tag(name = "allRule", description = "所有规则")
public class AllRuleController {

    @Autowired
    private AllRuleService allRuleService;
    
    
    @Operation(summary  = "添加所有规则")
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public ApiResponse<Void> add(@Valid @RequestBody AllRuleDto allRuleDto) {
        allRuleService.add(allRuleDto);
        return ApiResponse.ok();
    }
    
    @Operation(summary  = "查询所有规则")
    @RequestMapping(value = "/get", method = RequestMethod.POST)
    public ApiResponse<AllDepthRuleVo> get() {
        return ApiResponse.ok(allRuleService.get());
    }
}
