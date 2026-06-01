package com.chinaums.saas.test.util;

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.Security;

/**
 * SM2 加密工具类 - 从 SMUtil/Sm2Util 提取核心方法
 * 直接使用 BouncyCastle 底层 API，避免 BCECPublicKey 构造器兼容问题
 */
public class SM2EncryptUtil {

    private static final String SM2_CURVE = "sm2p256v1";
    private static final X9ECParameters SM2_PARAMS;
    private static final ECDomainParameters DOMAIN;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        SM2_PARAMS = GMNamedCurves.getByName(SM2_CURVE);
        DOMAIN = new ECDomainParameters(
                SM2_PARAMS.getCurve(),
                SM2_PARAMS.getG(),
                SM2_PARAMS.getN(),
                SM2_PARAMS.getH(),
                SM2_PARAMS.getSeed()
        );
    }

    /**
     * SM2 加密，返回十六进制字符串
     *
     * @param pubKeyHex 十六进制公钥串 (04开头，130字符)
     * @param srcData   明文
     * @return 十六进制密文
     */
    public static String sm2EncryptHex(String pubKeyHex, String srcData) {
        try {
            // 解析公钥点
            byte[] pubBytes = Hex.decode(pubKeyHex);
            ECPoint pubPoint = SM2_PARAMS.getCurve().decodePoint(pubBytes);
            ECPublicKeyParameters pubKeyParams = new ECPublicKeyParameters(pubPoint, DOMAIN);

            // SM2 加密 (C1C3C2 模式)
            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            engine.init(true, new ParametersWithRandom(pubKeyParams, new SecureRandom()));
            byte[] srcBytes = srcData.getBytes("UTF-8");
            byte[] encrypted = engine.processBlock(srcBytes, 0, srcBytes.length);
            return Hex.toHexString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("SM2 加密失败", e);
        }
    }

    /**
     * SM2 解密
     *
     * @param priKeyHex     十六进制私钥串
     * @param encDataHex    十六进制密文
     * @return 明文
     */
    public static String sm2DecryptHex(String priKeyHex, String encDataHex) {
        try {
            BigInteger priKeyD = new BigInteger(priKeyHex, 16);
            org.bouncycastle.crypto.params.ECPrivateKeyParameters priKeyParams =
                    new org.bouncycastle.crypto.params.ECPrivateKeyParameters(priKeyD, DOMAIN);

            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            engine.init(false, priKeyParams);
            byte[] encBytes = Hex.decode(encDataHex);
            byte[] decrypted = engine.processBlock(encBytes, 0, encBytes.length);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("SM2 解密失败", e);
        }
    }
}
