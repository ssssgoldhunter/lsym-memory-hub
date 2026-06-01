package com.chinaums.saas.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SaaS 中信银行接口 - 本地可视化测试工具
 * 启动后访问 http://localhost:8888
 */
@SpringBootApplication
public class SaasTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaasTestApplication.class, args);
        System.out.println("========================================");
        System.out.println("  SaaS ZX Test UI 已启动");
        System.out.println("  访问 http://localhost:8888");
        System.out.println("========================================");
    }
}
