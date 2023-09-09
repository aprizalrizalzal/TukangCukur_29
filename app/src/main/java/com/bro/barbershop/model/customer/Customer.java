package com.bro.barbershop.model.customer;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Customer implements Parcelable {
    private String customerId;
    private String customer;
    private String notes;
    private String phoneNumber;
    private String userId;

    public Customer() {
    }

    public Customer(String customerId, String customer, String notes, String phoneNumber, String userId) {
        this.customerId = customerId;
        this.customer = customer;
        this.notes = notes;
        this.phoneNumber = phoneNumber;
        this.userId = userId;
    }

    protected Customer(Parcel in) {
        customerId = in.readString();
        customer = in.readString();
        notes = in.readString();
        phoneNumber = in.readString();
        userId = in.readString();
    }

    public static final Creator<Customer> CREATOR = new Creator<Customer>() {
        @Override
        public Customer createFromParcel(Parcel in) {
            return new Customer(in);
        }

        @Override
        public Customer[] newArray(int size) {
            return new Customer[size];
        }
    };

    public String getCustomerId() {
        return customerId;
    }

    @SuppressWarnings("unused")
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomer() {
        return customer;
    }

    @SuppressWarnings("unused")
    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getNotes() {
        return notes;
    }

    @SuppressWarnings("unused")
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    @SuppressWarnings("unused")
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUserId() {
        return userId;
    }

    @SuppressWarnings("unused")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(customerId);
        dest.writeString(customer);
        dest.writeString(notes);
        dest.writeString(phoneNumber);
        dest.writeString(userId);
    }
}
