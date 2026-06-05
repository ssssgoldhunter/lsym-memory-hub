package com.chinaums.saas.test.dto;

import lombok.Data;

/**
 * 文件下载请求参数
 * 支持 bizFunc: 01-SFTP文件下载, 02-电子回执获取, 14-登记簿明细打印
 */
@Data
public class FileDownloadRequest {

    /** 环境ID */
    private String envId;

    /** 业务用途: 01/02/14 */
    private String bizFunc;

    // === bizFunc=01 SFTP文件下载 ===
    /** 文件传输类型 (MSG/SFTP/OSS)，bizFunc=01时使用，默认MSG */
    private String transType;

    // === bizFunc=02 电子回执获取 ===
    /** 中信侧交易流水号 USER_SSN，bizFunc=02必填 */
    private String userSsn;

    /** 中信侧交易日期 USER_TRANS_DT (yyyyMMdd)，bizFunc=02必填 */
    private String userTransDt;

    /** 交易类型 TRANS_TYPE，bizFunc=02必填 */
    private String transType02;

    // === bizFunc=14 登记簿明细打印 ===
    /** 用户编号 acctNo (J开头)，bizFunc=14必填，后端SM2加密 */
    private String acctNo;

    /** 交易起始日期 transStartDate (yyyyMMdd)，bizFunc=14必填 */
    private String transStartDate;

    /** 交易结束日期 transEndDate (yyyyMMdd)，bizFunc=14必填 */
    private String transEndDate;

    /** 资金方向 flag (C-贷/D-借)，bizFunc=14可选 */
    private String flag;
}
