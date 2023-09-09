package com.bro.barbershop.model.transaction;

public class Transaction {
    private String transactionId;
    private String userId;
    private String shavingId;
    private String transactionDate;
    private Boolean paymentStatus;

    public Transaction() {
    }

    public Transaction(String transactionId, String userId, String shavingId, String transactionDate, Boolean paymentStatus) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.shavingId = shavingId;
        this.transactionDate = transactionDate;
        this.paymentStatus = paymentStatus;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getShavingId() {
        return shavingId;
    }

    public void setShavingId(String shavingId) {
        this.shavingId = shavingId;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Boolean getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(Boolean paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}
