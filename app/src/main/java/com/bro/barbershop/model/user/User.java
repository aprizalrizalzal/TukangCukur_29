package com.bro.barbershop.model.user;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class User implements Parcelable {
    private String userId;
    private String email;
    private String phoneNumber;
    private String photoUrl;
    private String roleId;
    private String tokenId;
    private String username;

    public User() {
    }

    public User(String userId, String email, String phoneNumber, String photoUrl, String roleId, String tokenId, String username) {
        this.userId = userId;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.photoUrl = photoUrl;
        this.roleId = roleId;
        this.tokenId = tokenId;
        this.username = username;
    }

    protected User(Parcel in) {
        userId = in.readString();
        email = in.readString();
        phoneNumber = in.readString();
        photoUrl = in.readString();
        roleId = in.readString();
        tokenId = in.readString();
        username = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @NonNull
    @Override
    public String toString() {
        return username;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(email);
        dest.writeString(phoneNumber);
        dest.writeString(photoUrl);
        dest.writeString(roleId);
        dest.writeString(tokenId);
        dest.writeString(username);
    }
}


