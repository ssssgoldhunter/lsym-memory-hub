package com.chinaums.saas.test.dto;

import lombok.Data;

/**
 * 交易状态查询请求 (bizFunc=74/87)
 */
@Data
public class TransStatusQueryRequest {

    /** 环境标识 */
    private String envId;

    /** 查询类型: 74-支付状态, 87-提现状态 */
    private String queryType;

    /** J账号 */
    private String acctNo;

    /** 原始交易日期 YYYYMMDD */
    private String oriTransDate;

    /** 商户业务订单号 (74接口, 三选一) */
    private String bussId;

    /** 商户业务子订单号 (74接口) */
    private String bussSubId;

    /** 交易类型 (74接口):
     * 00-支付, 01-退款, 02-平台补贴, 03-平台扣罚
     */
    private String transType;

    /** 中信侧交易流水号 (74接口, 三选一) */
    private String oriUserSsn;

    /** 原始交易流水号 (87接口) */
    private String oriTransSsn;

    /** 账户类型 (87接口, 默认1) */
    private String accountType = "1";

    /** 交易金额 (87接口) */
    private String transAmt;

    /** 时间戳 (87接口, 格式: 2024-05-15-09.27.56.845086) */
    private String timeStampe;
}
