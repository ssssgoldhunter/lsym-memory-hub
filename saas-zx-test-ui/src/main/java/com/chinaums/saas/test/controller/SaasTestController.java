package com.chinaums.saas.test.controller;

import com.alibaba.fastjson2.JSONObject;
import com.chinaums.saas.test.config.SaasAccountConfig;
import com.chinaums.saas.test.dto.AccountQueryRequest;
import com.chinaums.saas.test.dto.ApiResponse;
import com.chinaums.saas.test.dto.FileDownloadRequest;
import com.chinaums.saas.test.dto.TransDetailQueryRequest;
import com.chinaums.saas.test.dto.TransStatusQueryRequest;
import com.chinaums.saas.test.model.SaasEnvironment;
import com.chinaums.saas.test.util.SaasHttpUtil;
import com.chinaums.saas.test.util.SaasSignUtil;
import com.chinaums.saas.test.util.SM2EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SaaS 测试 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class SaasTestController {

    private final SaasAccountConfig accountConfig;

    public SaasTestController(SaasAccountConfig accountConfig) {
        this.accountConfig = accountConfig;
    }

    /**
     * 获取环境列表（脱敏显示）
     */
    @GetMapping("/envs")
    public ApiResponse<List<Map<String, String>>> getEnvironments() {
        List<Map<String, String>> envList = accountConfig.getAllEnvironments().stream().map(env -> {
            Map<String, String> map = new HashMap<>();
            map.put("id", env.getId());
            map.put("name", env.getName());
            map.put("mchntId", env.getMchntId());
            map.put("mchntMbrId", env.getMchntMbrId());
            map.put("chnlNo", env.getChnlNo());
            map.put("url", env.getUrl());
            return map;
        }).collect(Collectors.toList());
        return ApiResponse.ok(envList, null);
    }

    /**
     * 账户查询 (bizFunc=46)
     */
    @PostMapping("/query/acct-info")
    public ApiResponse<JSONObject> queryAcctInfo(@RequestBody AccountQueryRequest request) {
        try {
            SaasEnvironment env = accountConfig.getEnvironment(request.getEnvId());
            String encryptedAcctNo = SM2EncryptUtil.sm2EncryptHex(env.getPublicKey(), request.getAcctNo());

            Map<String, Object> body = SaasSignUtil.buildAcctInfoQueryBody(
                    env.getMchntId(), env.getMchntMbrId(), env.getChnlNo(),
                    encryptedAcctNo, request.getRegisterAttr()
            );

            String bodyJson = JSONObject.toJSONString(body);
            log.info("账户查询请求: {}", bodyJson);

            JSONObject result = SaasHttpUtil.send(env.getAppKey(), env.getAppId(), env.getUrl(), "query-acct-info", bodyJson);
            log.info("账户查询响应: {}", result);

            return ApiResponse.ok(result, result != null ? result.toJSONString() : null);
        } catch (Exception e) {
            log.error("账户查询失败", e);
            return ApiResponse.fail("账户查询失败: " + e.getMessage());
        }
    }

    /**
     * 24交易查询 (bizFunc=24)
     * 需要 acctNo SM2加密, 有 registerAttr 选择
     */
    @PostMapping("/query/trans-detail-24")
    public ApiResponse<JSONObject> queryTransDetail24(@RequestBody TransDetailQueryRequest request) {
        try {
            SaasEnvironment env = accountConfig.getEnvironment(request.getEnvId());
            String encryptedAcctNo = SM2EncryptUtil.sm2EncryptHex(env.getPublicKey(), request.getAcctNo());

            Map<String, Object> body = SaasSignUtil.buildTransDetail24Body(
                    env.getMchntId(), env.getMchntMbrId(), env.getChnlNo(),
                    encryptedAcctNo, request.getTransDate(), request.getTransType(),
                    request.getRegisterAttr(), request.getPage() != null ? request.getPage() : 1
            );

            String bodyJson = JSONObject.toJSONString(body);
            log.info("24交易查询请求: {}", bodyJson);

            JSONObject result = SaasHttpUtil.send(env.getAppKey(), env.getAppId(), env.getUrl(), "query-trans-details", bodyJson);
            log.info("24交易查询响应: {}", result);

            return ApiResponse.ok(result, result != null ? result.toJSONString() : null);
        } catch (Exception e) {
            log.error("24交易查询失败", e);
            return ApiResponse.fail("24交易查询失败: " + e.getMessage());
        }
    }

    /**
     * 25交易查询 (bizFunc=25)
     * 无 registerAttr, mchntMbrId 固定从环境配置获取
     */
    @PostMapping("/query/trans-detail-25")
    public ApiResponse<JSONObject> queryTransDetail25(@RequestBody TransDetailQueryRequest request) {
        try {
            SaasEnvironment env = accountConfig.getEnvironment(request.getEnvId());

            Map<String, Object> body = SaasSignUtil.buildTransDetail25Body(
                    env.getMchntId(), env.getMchntMbrId(), env.getChnlNo(),
                    request.getTransDate(), request.getTransType(),
                    request.getPage() != null ? request.getPage() : 1
            );

            String bodyJson = JSONObject.toJSONString(body);
            log.info("25交易查询请求: {}", bodyJson);

            JSONObject result = SaasHttpUtil.send(env.getAppKey(), env.getAppId(), env.getUrl(), "query-trans-details", bodyJson);
            log.info("25交易查询响应: {}", result);

            return ApiResponse.ok(result, result != null ? result.toJSONString() : null);
        } catch (Exception e) {
            log.error("25交易查询失败", e);
            return ApiResponse.fail("25交易查询失败: " + e.getMessage());
        }
    }

    /**
     * 交易状态查询 (bizFunc=74/87)
     * 74-支付状态查询 chnlNo=0010
     * 87-提现状态查询 chnlNo=A010
     */
    @PostMapping("/query/trans-status")
    public ApiResponse<JSONObject> queryTransStatus(@RequestBody TransStatusQueryRequest request) {
        try {
            SaasEnvironment env = accountConfig.getEnvironment(request.getEnvId());
            String encryptedAcctNo = SM2EncryptUtil.sm2EncryptHex(env.getPublicKey(), request.getAcctNo());

            Map<String, Object> body = SaasSignUtil.buildTransStatusBody(
                    env.getMchntId(), env.getMchntMbrId(),
                    encryptedAcctNo, request.getOriTransDate(),
                    request.getQueryType(), request.getBussId(), request.getBussSubId(),
                    request.getTransType(), request.getOriUserSsn(),
                    request.getOriTransSsn(), request.getAccountType(),
                    request.getTransAmt(), request.getTimeStampe(),
                    env.getAppId(), env.getAppKey(), env.getUrl()
            );

            String bodyJson = JSONObject.toJSONString(body);
            log.info("交易状态查询请求: {}", bodyJson);

            JSONObject result = SaasHttpUtil.send(env.getAppKey(), env.getAppId(), env.getUrl(), "query-trans-status", bodyJson);
            log.info("交易状态查询响应: {}", result);

            return ApiResponse.ok(result, result != null ? result.toJSONString() : null);
        } catch (Exception e) {
            log.error("交易状态查询失败", e);
            return ApiResponse.fail("交易状态查询失败: " + e.getMessage());
        }
    }

    /**
     * 文件下载 (bizFunc=01/02/14)
     * 返回文件流，触发浏览器原生下载
     */
    @PostMapping("/download")
    public ResponseEntity<?> fileDownload(@RequestBody FileDownloadRequest request) {
        try {
            SaasEnvironment env = accountConfig.getEnvironment(request.getEnvId());
            Map<String, Object> body;

            switch (request.getBizFunc()) {
                case "01":
                    body = SaasSignUtil.buildFileDownload01Body(
                            env.getMchntId(), env.getMchntMbrId(), env.getChnlNo(),
                            request.getTransType()
                    );
                    break;
                case "02":
                    if (request.getUserSsn() == null || request.getUserSsn().isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.fail("USER_SSN不能为空"));
                    }
                    if (request.getUserTransDt() == null || request.getUserTransDt().isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.fail("USER_TRANS_DT不能为空"));
                    }
                    if (request.getTransType02() == null || request.getTransType02().isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.fail("TRANS_TYPE不能为空"));
                    }
                    body = SaasSignUtil.buildFileDownload02Body(
                            env.getMchntId(), env.getMchntMbrId(), env.getChnlNo(),
                            request.getUserSsn(), request.getUserTransDt(), request.getTransType02(),
                            env.getAppId(), env.getAppKey(), env.getUrl()
                    );
                    break;
                case "14":
                    if (request.getAcctNo() == null || request.getAcctNo().isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.fail("用户编号不能为空"));
                    }
                    if (request.getTransStartDate() == null || request.getTransStartDate().isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.fail("交易起始日期不能为空"));
                    }
                    if (request.getTransEndDate() == null || request.getTransEndDate().isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.fail("交易结束日期不能为空"));
                    }
                    String encryptedAcctNo = SM2EncryptUtil.sm2EncryptHex(env.getPublicKey(), request.getAcctNo());
                    body = SaasSignUtil.buildFileDownload14Body(
                            env.getMchntId(), env.getMchntMbrId(), env.getChnlNo(),
                            encryptedAcctNo, request.getTransStartDate(), request.getTransEndDate(),
                            request.getFlag(),
                            env.getAppId(), env.getAppKey(), env.getUrl()
                    );
                    break;
                default:
                    return ResponseEntity.ok(ApiResponse.fail("不支持的bizFunc: " + request.getBizFunc()));
            }

            String bodyJson = JSONObject.toJSONString(body);
            log.info("文件下载请求 bizFunc={}: {}", request.getBizFunc(), bodyJson);

            JSONObject result = SaasHttpUtil.send(env.getAppKey(), env.getAppId(), env.getUrl(), "file-download", bodyJson);
            log.info("文件下载完整响应: {}", result != null ? result.toJSONString() : "null");

            if (result == null) {
                return ResponseEntity.ok(ApiResponse.fail("银行接口无返回"));
            }

            String errCode = result.getString("errCode");
            String sysRespCode = result.getString("sysRespCode");

            // 判断成功
            boolean success = "D5000000".equals(errCode) && "00000".equals(sysRespCode);
            // 14 的成功判断可能用 RESULT_CODE
            if (!success && result.getString("RESULT_CODE") != null) {
                success = "00000".equals(result.getString("RESULT_CODE"));
            }

            if (!success) {
                return ResponseEntity.ok(ApiResponse.fail(
                        "下载失败: errCode=" + errCode + ", sysRespCode=" + sysRespCode
                                + ", errInfo=" + result.getString("errInfo")));
            }

            // 获取文件内容
            String fileContentB64 = result.getString("FILE_CONTENT");
            if (fileContentB64 == null || fileContentB64.isEmpty()) {
                // 01 SFTP模式可能不返回内容
                return ResponseEntity.ok(ApiResponse.fail("文件内容为空（可能为SFTP模式，文件已推送到服务器）"));
            }

            byte[] fileBytes = java.util.Base64.getDecoder().decode(fileContentB64);

            // 确定文件名
            String fileName = result.getString("FILE_NAME");
            if (fileName == null || fileName.isEmpty()) {
                fileName = result.getString("fileName");
            }
            if (fileName == null || fileName.isEmpty()) {
                fileName = "download_" + request.getBizFunc() + "_" + System.currentTimeMillis() + ".pdf";
            }

            log.info("文件下载成功: fileName={}, size={}bytes", fileName, fileBytes.length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(fileBytes.length)
                    .body(fileBytes);

        } catch (Exception e) {
            log.error("文件下载失败", e);
            return ResponseEntity.ok(ApiResponse.fail("文件下载失败: " + e.getMessage()));
        }
    }
}
