package io.github.reader.sjsql.model;

import java.time.LocalDateTime;

public class PaymentOrder {

    private Long id;
    private Long accountId;
    private Long tenantId;
    private String tradeNo;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "paymentOrder{" +
            "id=" + id +
            ", accountId=" + accountId +
            ", tenantId=" + tenantId +
            ", tradeNo='" + tradeNo + '\'' +
            ", createTime=" + createTime +
            '}';
    }
}
