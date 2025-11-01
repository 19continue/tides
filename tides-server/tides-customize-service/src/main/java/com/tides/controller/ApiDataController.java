package com.tides.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tides.common.ApiResponse;
import com.tides.dto.AddApiDataDto;
import com.tides.dto.ApiDataDto;
import com.tides.service.ApiDataService;
import com.tides.vo.ApiDataVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: api调用记录 控制层
 * @author: 19continue
 **/
@RestController
@RequestMapping("/apiData")
@Tag(name = "apiData", description = "api调用记录")
public class ApiDataController {
    
    @Autowired
    private ApiDataService apiDataService;
    
    @Operation(summary  = "分页查询api调用记录")
    @RequestMapping(value = "/pageList",method = RequestMethod.POST)
    public ApiResponse<Page<ApiDataVo>> pageList(@Valid @RequestBody ApiDataDto dto) {
        return ApiResponse.ok(apiDataService.pageList(dto));
    }
    @Operation(summary  = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public ApiResponse<Boolean> add(@Valid @RequestBody AddApiDataDto dto) {
        return ApiResponse.ok(apiDataService.add(dto));
    }
}
