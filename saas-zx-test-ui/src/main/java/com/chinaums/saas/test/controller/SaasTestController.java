package com.chinaums.saas.test.controller;

import com.alibaba.fastjson2.JSONObject;
import com.chinaums.saas.test.config.SaasAccountConfig;
import com.chinaums.saas.test.dto.AccountQueryRequest;
import com.chinaums.saas.test.dto.ApiResponse;
import com.chinaums.saas.test.dto.TransDetailQueryRequest;
import com.chinaums.saas.test.dto.TransStatusQueryRequest;
import com.chinaums.saas.test.model.SaasEnvironment;
import com.chinaums.saas.test.util.SaasHttpUtil;
import com.chinaums.saas.test.util.SaasSignUtil;
import com.chinaums.saas.test.util.SM2EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
}
