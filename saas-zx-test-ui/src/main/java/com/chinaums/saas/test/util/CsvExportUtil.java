package com.chinaums.saas.test.util;

import com.alibaba.fastjson2.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * CSV 导出工具
 * <p>
 * 列取所有行 key 的并集(首次出现顺序), 自动适配银行返回的不同字段;
 * 编码 UTF-8 + BOM, Excel 双击打开中文不乱码; 行分隔 \r\n。
 * 超长纯数字(>=16位, 如订单号/流水号) 前缀 \t 强制成文本, 避免 Excel 科学计数法并丢精度。
 */
public class CsvExportUtil {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private CsvExportUtil() {
    }

    /**
     * 将 JSON 行列表转为 UTF-8+BOM 的 CSV 字节
     */
    public static byte[] toCsv(List<JSONObject> rows) {
        // 1. 收集列(并集, 首次出现顺序)
        Set<String> columns = new LinkedHashSet<>();
        if (rows != null) {
            for (JSONObject row : rows) {
                columns.addAll(row.keySet());
            }
        }

        // 2. 拼 CSV 文本
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String col : columns) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(escape(col));
        }
        sb.append("\r\n");

        if (rows != null) {
            for (JSONObject row : rows) {
                first = true;
                for (String col : columns) {
                    if (!first) {
                        sb.append(",");
                    }
                    first = false;
                    Object v = row.get(col);
                    sb.append(escape(forceTextIfLongNumeric(v == null ? "" : v.toString())));
                }
                sb.append("\r\n");
            }
        }

        // 3. UTF-8 + BOM
        byte[] textBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream(textBytes.length + UTF8_BOM.length);
        try {
            out.write(UTF8_BOM);
            out.write(textBytes);
        } catch (IOException e) {
            throw new RuntimeException("生成CSV失败", e);
        }
        return out.toByteArray();
    }

    /**
     * CSV 字段转义: 含逗号/引号/换行 -> 双引号包裹, 内部引号翻倍
     */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needQuote = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needQuote ? "\"" + escaped + "\"" : escaped;
    }

    /**
     * 超长纯数字(>=16位, 如 MCHNT_ORDER_ID/MCHNT_ORDER_SUB_ID/REQ_JRN)
     * Excel 会转科学计数法并只保留15位精度, 前缀 \t 强制成文本完整显示。
     */
    private static String forceTextIfLongNumeric(String value) {
        if (value == null || value.length() < 16) {
            return value;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return value;
            }
        }
        return "\t" + value;
    }
}
