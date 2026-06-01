package com.chinaums.saas.test.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SaaS 环境配置模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaasEnvironment {

    /** 环境标识 */
    private String id;

    /** 环境名称 */
    private String name;

    /** 商户编号 */
    private String mchntId;

    /** 商户会员编号 */
    private String mchntMbrId;

    /** 渠道编号 */
    private String chnlNo;

    /** 业务编码 */
    private String busMode;

    /** API地址 */
    private String url;

    /** AppId */
    private String appId;

    /** AppKey */
    private String appKey;

    /** 银行公钥(用于SM2加密) */
    private String publicKey;

    /** 银行私钥(用于SM2解密) */
    private String privateKey;

    /** 用户角色 */
    private String userRole;
}
