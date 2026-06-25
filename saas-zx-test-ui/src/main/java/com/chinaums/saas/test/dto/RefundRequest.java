package com.chinaums.saas.test.dto;

import lombok.Data;

/**
 * 退款请求 (bizFunc=21)
 */
@Data
public class RefundRequest {

    /** 环境标识 */
    private String envId;

    /** 原支付商户订单号 (与 ORI_USER_SSN 二选一) */
    private String oriBussId;

    /** 原支付商户子订单号 */
    private String oriBussSubId;

    /** 原支付中信侧流水号 (与 ORI_BUSS_ID 二选一) */
    private String oriUserSsn;

    /** 原支付中信侧交易日期 yyyyMMdd */
    private String oriUserTransDt;

    /** 退款交易日期 yyyyMMdd */
    private String transDt;

    /** 退款交易时间 HHmmss */
    private String transTm;

    /** 原支付付款方银行用户编号 */
    private String oriUserDId;

    /** 原支付付款方名称 */
    private String oriUserDNm;

    /** 原支付收款方银行用户编号 */
    private String oriUserCId;

    /** 原支付收款方名称 */
    private String oriUserCNm;

    /** 退款金额 */
    private String oriUserCAmt;

    /** 资金类型: 测试=001002, 生产=011002 */
    private String fundTp;
}