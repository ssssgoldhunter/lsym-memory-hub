package com.chinaums.saas.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String rawResponse;

    public static <T> ApiResponse<T> ok(T data, String rawResponse) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("查询成功")
                .data(data)
                .rawResponse(rawResponse)
                .build();
    }

    public static <T> ApiResponse<T> fail(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
