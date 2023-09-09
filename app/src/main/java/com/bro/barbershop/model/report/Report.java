package com.bro.barbershop.model.report;

public class Report {
    private String reportId;
    private Double employeeSalary;
    private String reportDate;
    private String userId;
    private String transactionId;

    public Report() {
    }

    public Report(String reportId, Double employeeSalary, String reportDate, String userId, String transactionId) {
        this.reportId = reportId;
        this.employeeSalary = employeeSalary;
        this.reportDate = reportDate;
        this.userId = userId;
        this.transactionId = transactionId;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public Double getEmployeeSalary() {
        return employeeSalary;
    }

    public void setEmployeeSalary(Double employeeSalary) {
        this.employeeSalary = employeeSalary;
    }

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}


