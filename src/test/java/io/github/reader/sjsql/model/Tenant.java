package io.github.reader.sjsql.model;

public class Tenant extends BaseEntity{

    private Long accountId;
    private String name;
    private Boolean enabled;

    private PaymentOrder paymentOrder;

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