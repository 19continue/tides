package com.tides;

import com.tides.config.TidesCommonAutoConfig;
import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
/**
 * @description: 监控服务启动
 * @author: 19continue
 **/
@EnableAdminServer
@EnableDiscoveryClient
@SpringBootApplication(exclude = TidesCommonAutoConfig.class)
public class AdminApplication {

    public static void main(String[] args) {
        System.setProperty("nacos.logging.default.config.enabled","false");
        SpringApplication.run(AdminApplication.class, args);
    }

}
