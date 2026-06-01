package com.chinaums.saas.test.dto;

import lombok.Data;

/**
 * 账户查询请求 (bizFunc=46)
 */
@Data
public class AccountQueryRequest {

    /** 环境标识 test/prod */
    private String envId;

    /** J账号 */
    private String acctNo;

    /** 登记簿标识: 01-公共调账登记簿, 12-自有资金登记薄, 14-担保登记薄, 00-默认 */
    private String registerAttr;
}
