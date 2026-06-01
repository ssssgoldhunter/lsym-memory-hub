package com.chinaums.saas.test.dto;

import lombok.Data;

/**
 * 交易明细查询请求 (bizFunc=24/25)
 */
@Data
public class TransDetailQueryRequest {

    /** 环境标识 test/prod */
    private String envId;

    /** 交易日期 YYYYMMDD */
    private String transDate;

    /** J账号 (24接口必填, 25接口不需要) */
    private String acctNo;

    /** 交易类型:
     * 01-入金分账, 02-交易划转, 03-提现, 04-提现手续费,
     * 05-提现退汇, 06-渠道来账, 98-所有(返回明细类型), 99-所有(返回汇总类型)
     */
    private String transType;

    /** 登记簿标识 (24接口使用):
     * 01-公共调账登记簿, 12-自有资金登记薄, 13-担保登记薄, 17-待结算手续费登记簿
     */
    private String registerAttr;

    /** 页码，默认1 */
    private Integer page = 1;
}
