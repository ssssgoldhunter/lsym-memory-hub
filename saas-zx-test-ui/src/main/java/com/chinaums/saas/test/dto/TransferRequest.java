package com.chinaums.saas.test.dto;

import lombok.Data;

/**
 * 转账请求 (bizFunc=27, appName="transfer")
 * 参考 SaasZxTest#transferNew
 */
@Data
public class TransferRequest {

    /** 环境标识 */
    private String envId;

    /** 付款方J账号 */
    private String outAcctNo;

    /** 付款方名称 */
    private String outAcctNm;

    /** 收款方J账号 */
    private String inAcctNo;

    /** 收款方名称 */
    private String inAcctNm;

    /** 交易金额（分） */
    private String transAmt;

    /** 商户订单号 */
    private String bussId;

    /** 商户子订单号 */
    private String bussSubId;

    /** 资金类型: 测试=001002, 生产=011002 */
    private String fundTp;

    /** 备注 */
    private String memo;

    /** 交易日期 yyyyMMdd */
    private String transDt;

    /** 交易时间 HHmmss */
    private String transTm;
}