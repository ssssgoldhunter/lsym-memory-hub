package com.chinaums.saas.test.dto;

import lombok.Data;

/**
 * 转账请求参数
 * 对应 SaasZxTest#transferNew 方法
 */
@Data
public class TransferRequest {

    /** 环境ID */
    private String envId;

    /** 付款账号 (J开头内部账号) */
    private String payAct;

    /** 收款账号 (J开头内部账号) */
    private String recAct;

    /** 付款方名称 */
    private String payName;

    /** 收款方名称 */
    private String recName;

    /** 交易金额（单位：分） */
    private String transAmt;

    /** 商户订单号 BUSS_ID */
    private String bussId;

    /** 商户子订单号 BUSS_SUB_ID */
    private String bussSubId;

    /** 备注 MEMO */
    private String memo;
}
