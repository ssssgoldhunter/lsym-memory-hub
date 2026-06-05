package com.chinaums.saas.test.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 签名和流水号工具类 - 从 SaasZxTest/SassUtil 提取
 */
public class SaasSignUtil {

    /**
     * 生成交易流水号
     */
    public static String getTransSsn(String mchntId) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        StringBuilder sb = new StringBuilder(mchntId);
        sb.append(timestamp);
        String nonce = String.valueOf(
                (long) (Math.random() * (99999999999L - 1000000000000L)) + 1000000000000L
        ).substring(0, 11);
        sb.append(nonce);
        return sb.toString();
    }

    /**
     * 获取当前交易时间
     */
    public static String getTransTime() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    /**
     * 生成 LaasSsn
     */
    public static String getLaasSsn(String mchntId) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        StringBuilder sb = new StringBuilder(mchntId);
        sb.append(timestamp);
        String nonce = String.valueOf(
                (long) (Math.random() * (999999999999999L - 10000000000000000L)) + 10000000000000000L
        ).substring(0, 16);
        sb.append(nonce);
        return sb.toString();
    }

    /**
     * 构建账户查询请求体 (bizFunc=46)
     */
    public static Map<String, Object> buildAcctInfoQueryBody(String mchntId, String mchntMbrId,
                                                              String chnlNo, String encryptedAcctNo,
                                                              String registerAttr) {
        Map<String, Object> body = new HashMap<>();
        body.put("transSsn", getTransSsn(mchntMbrId));
        body.put("transTime", getTransTime());
        body.put("mchntId", mchntId);
        body.put("mchntMbrId", mchntMbrId);
        body.put("chnlNo", chnlNo);
        body.put("bizFunc", "46");
        body.put("acctNo", encryptedAcctNo);

        Map<String, Object> reserve = new HashMap<>();
        reserve.put("registerAttr", registerAttr);
        reserve.put("laasSsn", getLaasSsn(mchntId));
        body.put("reserve", reserve);

        return body;
    }

    /**
     * 构建交易明细查询请求体 (bizFunc=24)
     * 参考 SaasZxTest#queryTransDetails4 / queryTransDetails5
     *
     * 关键差异:
     * - registerAttr 用小写 key "registerAttr"
     * - beginDate/endDate 格式 YYYYMMDD 补全为 YYYYMMDD000000 (14位)
     * - transSsn 在 reserve 内部每次查询时设置
     * - PAGE 支持外部传入
     */
    public static Map<String, Object> buildTransDetail24Body(String mchntId, String mchntMbrId,
                                                              String chnlNo, String encryptedAcctNo,
                                                              String transDate, String transType,
                                                              String registerAttr, int page) {
        // beginDate/endDate 需要14位: YYYYMMDD000000
        String beginDate = transDate + "000000";
        // endDate 为 beginDate + 1天
        String endDate = transDate + "235959";

        Map<String, Object> body = new HashMap<>();
        body.put("transTime", getTransTime());
        body.put("mchntId", mchntId);
        body.put("mchntMbrId", mchntMbrId);
        body.put("bizFunc", "24");
        body.put("chnlNo", chnlNo);
        body.put("acctNo", encryptedAcctNo);
        body.put("beginDate", beginDate);
        body.put("endDate", endDate);

        Map<String, Object> reserve = new HashMap<>();
        reserve.put("TRANS_TYPE", transType);
        reserve.put("registerAttr", registerAttr);
        reserve.put("TRANS_DATE", endDate);
        reserve.put("PAGE", page);
        reserve.put("laasSsn", getLaasSsn(mchntId));
        body.put("transSsn", getTransSsn(mchntMbrId));
        body.put("reserve", reserve);

        return body;
    }

    /**
     * 构建交易明细查询请求体 (bizFunc=25)
     * 参考 SaasZxTest#queryTransDetails / queryTransDetails2
     *
     * 关键差异:
     * - 无 registerAttr
     * - mchntMbrId 从环境配置获取
     * - PAGE 支持外部传入
     */
    public static Map<String, Object> buildTransDetail25Body(String mchntId, String mchntMbrId,
                                                              String chnlNo, String transDate,
                                                              String transType, int page) {
        Map<String, Object> body = new HashMap<>();
        body.put("transSsn", getTransSsn(mchntMbrId));
        body.put("transTime", getTransTime());
        body.put("mchntId", mchntId);
        body.put("mchntMbrId", mchntMbrId);
        body.put("bizFunc", "25");
        body.put("chnlNo", chnlNo);
        body.put("beginDate", transDate);
        body.put("endDate", transDate);

        Map<String, Object> reserve = new HashMap<>();
        reserve.put("TRANS_DATE", transDate);
        reserve.put("PAGE", page);
        reserve.put("TRANS_TYPE", transType);
        reserve.put("laasSsn", getLaasSsn(mchntId));
        body.put("reserve", reserve);

        return body;
    }

    /**
     * 构建交易状态查询请求体 (bizFunc=74/87)
     * 参考 SaasZxTest#queryTransStatus / queryTransStatus2
     *
     * 74-支付状态查询: chnlNo=0010, 三选一(ORI_USER_SSN / BUSS_ID+BUSS_SUB_ID / BUSS_ID)
     * 87-提现状态查询: chnlNo=A010, 需要 oriTransSsn
     */
    public static Map<String, Object> buildTransStatusBody(String mchntId, String mchntMbrId,
                                                            String encryptedAcctNo, String oriTransDate,
                                                            String queryType, String bussId, String bussSubId,
                                                            String transType, String oriUserSsn,
                                                            String oriTransSsn, String accountType,
                                                            String transAmt, String timeStampe,
                                                            String appId, String appKey, String url) {
        boolean isPay = "74".equals(queryType);
        String chnlNo = isPay ? "0010" : "A010";
        String laasSsn = getLaasSsn(mchntId);

        Map<String, Object> body = new HashMap<>();
        body.put("transSsn", getTransSsn(mchntMbrId));
        body.put("transTime", getTransTime());
        body.put("mchntId", mchntId);
        body.put("mchntMbrId", mchntMbrId);
        body.put("bizFunc", queryType);
        body.put("chnlNo", chnlNo);
        body.put("acctNo", encryptedAcctNo);
        body.put("oriTransDate", oriTransDate);
        // 顶层字段 - 银行接口需要
        body.put("laasSsn", laasSsn);
        body.put("appIdBank", appId);
        body.put("appKeyBank", appKey);
        body.put("urlBank", url);

        Map<String, Object> reserve = new HashMap<>();
        reserve.put("laasSsn", laasSsn);

        if (isPay) {
            // 74-支付状态查询 - 参考 ZxTransQueryHandle#queryTransStatus
            if (bussId != null && !bussId.isEmpty()) {
                reserve.put("BUSS_ID", bussId);
            }
            // 转账/消费查询时需要 BUSS_SUB_ID 和 TRANS_TYPE
            if (bussSubId != null && !bussSubId.isEmpty()) {
                reserve.put("BUSS_SUB_ID", bussSubId);
            }
            if (transType != null && !transType.isEmpty()) {
                reserve.put("TRANS_TYPE", transType);
            }
            if (oriUserSsn != null && !oriUserSsn.isEmpty()) {
                reserve.put("ORI_USER_SSN", oriUserSsn);
            }
            reserve.put("SIGN_INFO", "");
        } else {
            // 87-提现状态查询
            if (oriTransSsn != null && !oriTransSsn.isEmpty()) {
                body.put("oriTransSsn", oriTransSsn);
            }
            if (accountType != null && !accountType.isEmpty()) {
                reserve.put("accountType", accountType);
            }
            if (transAmt != null && !transAmt.isEmpty()) {
                reserve.put("transAmt", transAmt);
            }
            if (timeStampe != null && !timeStampe.isEmpty()) {
                reserve.put("timeStampe", timeStampe);
            }
        }

        body.put("reserve", reserve);
        return body;
    }

    /**
     * 构建余额转账请求体 (bizFunc=27)
     * 参考 SaasZxTest#transferNew
     */
    public static Map<String, Object> buildTransferBody(String mchntId, String mchntMbrId,
                                                         String chnlNo, String encryptedPayAct,
                                                         String encryptedRecAct, String transAmt,
                                                         String payName, String recName,
                                                         String bussId, String bussSubId,
                                                         String memo, String userRole,
                                                         String appId, String appKey, String url) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat stf = new SimpleDateFormat("HHmmss");
        Date now = new Date();

        Map<String, Object> body = new HashMap<>();
        body.put("transSsn", getTransSsn(mchntMbrId));
        body.put("transTime", getTransTime());
        body.put("mchntId", mchntId);
        body.put("mchntMbrId", mchntMbrId);
        body.put("chnlNo", "0010");
        body.put("bizFunc", "27");
        body.put("ccy", "CNY");
        body.put("outAcctNo", encryptedPayAct);
        body.put("inAcctNo", encryptedRecAct);
        body.put("transAmt", transAmt);
        body.put("laasSsn", getLaasSsn(mchntId));
        body.put("appIdBank", appId);
        body.put("appKeyBank", appKey);
        body.put("urlBank", url);

        Map<String, Object> reserve = new HashMap<>();
        reserve.put("USER_D_NM", payName);
        reserve.put("USER_C_NM", recName);
        reserve.put("USER_C_AMT", transAmt);
        reserve.put("P_SELF_FLAG", "N");
        reserve.put("P_SELF_AMT", "0");
        reserve.put("BUSS_ID", bussId);
        reserve.put("BUSS_SUB_ID", bussSubId);
        reserve.put("TRANS_DT", sdf.format(now));
        reserve.put("TRANS_TM", stf.format(now));
        reserve.put("FUND_TP", userRole);
        reserve.put("MEMO", memo != null && !memo.isEmpty() ? memo : "余额转账");
        reserve.put("laasSsn", getLaasSsn(mchntId));
        body.put("reserve", reserve);

        return body;
    }

    /**
     * 构建文件下载请求体 - bizFunc=01 SFTP文件下载
     */
    public static Map<String, Object> buildFileDownload01Body(String mchntId, String mchntMbrId,
                                                               String chnlNo, String transType) {
        Map<String, Object> body = new HashMap<>();
        body.put("transSsn", getTransSsn(mchntMbrId));
        body.put("transTime", getTransTime());
        body.put("mchntId", mchntId);
        body.put("mchntMbrId", mchntMbrId);
        body.put("bizFunc", "01");
        body.put("chnlNo", chnlNo);
        body.put("fileType", "601");
        if (transType != null && !transType.isEmpty()) {
            body.put("transType", transType);
        }

        Map<String, Object> reserve = new HashMap<>();
        reserve.put("laasSsn", getLaasSsn(mchntId));
        body.put("reserve", reserve);

        return body;
    }

    /**
     * 构建文件下载请求体 - bizFunc=02 电子回执获取
     */
    public static Map<String, Object> buildFileDownload02Body(String mchntId, String mchntMbrId,
                                                               String chnlNo, String userSsn,
                                                               String userTransDt, String transType,
                                                               String appId, String appKey, String url) {
        Map<String, Object> body = new HashMap<>();
        body.put("transSsn", getTransSsn(mchntMbrId));
        body.put("transTime", getTransTime());
        body.put("mchntId", mchntId);
        body.put("mchntMbrId", mchntMbrId);
        body.put("bizFunc", "02");
        body.put("chnlNo", chnlNo);
        body.put("fileType", "801");
        body.put("transType", "MSG");
        body.put("laasSsn", getLaasSsn(mchntId));
        body.put("appIdBank", appId);
        body.put("appKeyBank", appKey);
        body.put("urlBank", url);

        Map<String, Object> reserve = new HashMap<>();
        reserve.put("laasSsn", getLaasSsn(mchntId));
        reserve.put("USER_SSN", userSsn);
        reserve.put("USER_TRANS_DT", userTransDt);
        reserve.put("TRANS_TYPE", transType);
        body.put("reserve", reserve);

        return body;
    }

    /**
     * 构建文件下载请求体 - bizFunc=14 登记簿明细打印
     */
    public static Map<String, Object> buildFileDownload14Body(String mchntId, String mchntMbrId,
                                                               String chnlNo, String encryptedAcctNo,
                                                               String transStartDate, String transEndDate,
                                                               String flag,
                                                               String appId, String appKey, String url) {
        Map<String, Object> body = new HashMap<>();
        body.put("transSsn", getTransSsn(mchntMbrId));
        body.put("transTime", getTransTime());
        body.put("mchntId", mchntId);
        body.put("mchntMbrId", mchntMbrId);
        body.put("bizFunc", "14");
        body.put("chnlNo", chnlNo);
        body.put("acctNo", encryptedAcctNo);
        body.put("transStartDate", transStartDate);
        body.put("transEndDate", transEndDate);
        body.put("laasSsn", getLaasSsn(mchntId));
        body.put("appIdBank", appId);
        body.put("appKeyBank", appKey);
        body.put("urlBank", url);

        if (flag != null && !flag.isEmpty()) {
            body.put("flag", flag);
        }

        Map<String, Object> reserve = new HashMap<>();
        reserve.put("laasSsn", getLaasSsn(mchntId));
        body.put("reserve", reserve);

        return body;
    }
}
