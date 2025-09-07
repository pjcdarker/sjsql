package com.reader.sjsql.model;

import java.time.LocalDateTime;

public class Tenant {
    private Long id;
    private Long accountId;
    private String name;
    private Boolean enabled;
    private LocalDateTime createTime;

    private PaymentOrder paymentOrder;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public PaymentOrder getPaymentOrder() {
        return paymentOrder;
    }

    public void setPaymentOrder(PaymentOrder paymentOrder) {
        this.paymentOrder = paymentOrder;
    }

    @Override
    public String toString() {
        return "Tenant{" +
            "id=" + id +
            ", accountId=" + accountId +
            ", name='" + name + '\'' +
            ", enabled=" + enabled +
            ", createTime=" + createTime +
            ", paymentOrder=" + paymentOrder +
            '}';
    }
}