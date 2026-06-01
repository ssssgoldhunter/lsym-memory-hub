package com.chinaums.saas.test.config;

import com.chinaums.saas.test.model.SaasEnvironment;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SaaS 账户配置 - LSYM/MDL 各测试/生产环境
 * 数据来源: SAASConfig.Saas_zx
 */
@Configuration
public class SaasAccountConfig {

    private static final String PUBLIC_KEY = "0448642116f8d232035c11ff02544c0274a702d8e5cb65d5d5c9cafe5b990e9d5701a33ee944762f8c255643ff7d4612619231146f9300e5e5ef60c9e9c8163654";
    private static final String PRIVATE_KEY = "43082797d71107bf0de01fcb471dbd2a0d2bceaa21acaf4fd7b4cd005b44d349";

    private static final Map<String, SaasEnvironment> ENV_MAP = new LinkedHashMap<>();

    static {
        // LSYM 测试环境
        ENV_MAP.put("lsym_test", SaasEnvironment.builder()
                .id("lsym_test")
                .name("LSYM 测试环境")
                .mchntId("000013")
                .mchntMbrId("J04069400000000")
                .chnlNo("0010")
                .busMode("")
                .url("https://test-api-open.chinaums.com/v1/cwap/account/send/")
                .appId("10037e6f956042280195d16435bc0180")
                .appKey("b53a75c928dd45fc9c2bf51d25483b88")
                .publicKey(PUBLIC_KEY)
                .privateKey(PRIVATE_KEY)
                .userRole("011002")
                .build());

        // LSYM 生产环境
        ENV_MAP.put("lsym_prod", SaasEnvironment.builder()
                .id("lsym_prod")
                .name("LSYM 生产环境")
                .mchntId("000013")
                .mchntMbrId("J01068500000000")
                .chnlNo("0010")
                .busMode("")
                .url("https://api-mop.chinaums.com/v1/cwap/account/send/")
                .appId("8a81c1be89b6cc1f018c85d0196d12ad")
                .appKey("4dcea053b7c84a159b0704df33e61c3a")
                .publicKey(PUBLIC_KEY)
                .privateKey(PRIVATE_KEY)
                .userRole("011002")
                .build());

        // MDL 测试环境
        ENV_MAP.put("mdl_test", SaasEnvironment.builder()
                .id("mdl_test")
                .name("MDL 测试环境")
                .mchntId("000013")
                .mchntMbrId("J04101700000000")
                .chnlNo("0010")
                .busMode("")
                .url("https://test-api-open.chinaums.com/v1/cwap/account/send/")
                .appId("10037e6f956042280195d16435bc0180")
                .appKey("b53a75c928dd45fc9c2bf51d25483b88")
                .publicKey(PUBLIC_KEY)
                .privateKey(PRIVATE_KEY)
                .userRole("011002")
                .build());

        // MDL 生产环境
        ENV_MAP.put("mdl_prod", SaasEnvironment.builder()
                .id("mdl_prod")
                .name("MDL 生产环境")
                .mchntId("000013")
                .mchntMbrId("J01109800000000")
                .chnlNo("0010")
                .busMode("")
                .url("https://api-mop.chinaums.com/v1/cwap/account/send/")
                .appId("8a81c1be89b6cc1f018c85d0196d12ad")
                .appKey("4dcea053b7c84a159b0704df33e61c3a")
                .publicKey(PUBLIC_KEY)
                .privateKey(PRIVATE_KEY)
                .userRole("015002")
                .build());
    }

    public List<SaasEnvironment> getAllEnvironments() {
        return new ArrayList<>(ENV_MAP.values());
    }

    public SaasEnvironment getEnvironment(String envId) {
        return ENV_MAP.getOrDefault(envId, ENV_MAP.values().iterator().next());
    }
}
