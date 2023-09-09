package com.bro.barbershop.model.shaving;

public class Shaving {
    private String shavingId;
    private String customerId;
    private String hairstyle;
    private Double price;
    private String shavingDate;

    public Shaving() {
    }

    public Shaving(String shavingId, String customerId, String hairstyle, Double price, String shavingDate) {
        this.shavingId = shavingId;
        this.customerId = customerId;
        this.hairstyle = hairstyle;
        this.price = price;
        this.shavingDate = shavingDate;
    }

    public String getShavingId() {
        return shavingId;
    }

    public void setShavingId(String shavingId) {
        this.shavingId = shavingId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getHairstyle() {
        return hairstyle;
    }

    public void setHairstyle(String hairstyle) {
        this.hairstyle = hairstyle;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getShavingDate() {
        return shavingDate;
    }

    public void setShavingDate(String shavingDate) {
        this.shavingDate = shavingDate;
    }
}
