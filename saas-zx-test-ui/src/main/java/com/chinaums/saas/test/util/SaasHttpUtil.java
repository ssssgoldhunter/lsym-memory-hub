package com.chinaums.saas.test.util;

import com.alibaba.fastjson2.JSONObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * SaaS HTTP 请求工具类 - 从 SaasTemplateUtils 提取
 */
public class SaasHttpUtil {

    /**
     * 发送 SaaS 请求
     */
    public static JSONObject send(String appKey, String appId, String url, String appName, String bodyJson) throws Exception {
        String authorization = getOpenBodySig(appId, appKey, bodyJson);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String fullUrl = url + appName;
            HttpPost httpPost = new HttpPost(fullUrl);
            httpPost.addHeader("Authorization", authorization);

            StringEntity entity = new StringEntity(bodyJson, ContentType.APPLICATION_JSON.withCharset("UTF-8"));
            httpPost.setEntity(entity);

            return httpClient.execute(httpPost, response -> {
                String resp = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                return JSONObject.parseObject(resp);
            });
        }
    }

    /**
     * 生成 OPEN-BODY-SIG 签名
     */
    public static String getOpenBodySig(String appId, String appKey, String body) throws Exception {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String bodyDigest = sha256Hex(body);
        String strToSign = appId + timestamp + nonce + bodyDigest;

        byte[] signature = hmacSHA256(strToSign.getBytes(StandardCharsets.UTF_8), appKey.getBytes(StandardCharsets.UTF_8));
        String signatureStr = Base64.getEncoder().encodeToString(signature);

        return "OPEN-BODY-SIG AppId=\"" + appId + "\", Timestamp=\"" + timestamp
                + "\", Nonce=\"" + nonce + "\", Signature=\"" + signatureStr + "\"";
    }

    private static byte[] hmacSHA256(byte[] data, byte[] key) throws Exception {
        String algorithm = "HmacSHA256";
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(data);
    }

    /**
     * SHA-256 哈希，返回十六进制字符串（替代 commons-codec DigestUtils.sha256Hex）
     */
    private static String sha256Hex(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
