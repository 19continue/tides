package com.tides.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @description: 后台管理配置属性
 * @author: 19continue
 **/
@Data
@ConfigurationProperties(prefix = BackManageProperties.MANAGE)
public class BackManageProperties {
    
    public static final String MANAGE = "manage";
    
    private String username = "admin";
    
    private String password = "admin";

    private List<String> roleCodeList = List.of("PLATFORM_SUPER_ADMIN", "PROJECT_APPROVER");

    private List<String> roleNameList = List.of("平台超级管理员", "项目审核员");

    private String departmentId = "PLATFORM";

    private String departmentName = "平台运营中心";

    private String dataScopeType = "ALL";
    
    private List<String> loginExcludeApi = List.of("/auth/login");
    
    private Boolean apiPasswordCall = false;
    
    private String apiPassword;
}
