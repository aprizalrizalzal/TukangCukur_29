package com.bro.barbershop.model.report.pdf;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Pdf implements Parcelable {
    private String pdfId;
    private String pdfUrl;
    private String userId;

    public Pdf() {
    }

    public Pdf(String pdfId, String pdfUrl, String userId) {
        this.pdfId = pdfId;
        this.pdfUrl = pdfUrl;
        this.userId = userId;
    }

    protected Pdf(Parcel in) {
        pdfId = in.readString();
        pdfUrl = in.readString();
        userId = in.readString();
    }

    public static final Creator<Pdf> CREATOR = new Creator<Pdf>() {
        @Override
        public Pdf createFromParcel(Parcel in) {
            return new Pdf(in);
        }

        @Override
        public Pdf[] newArray(int size) {
            return new Pdf[size];
        }
    };

    public String getPdfId() {
        return pdfId;
    }

    public void setPdfId(String pdfId) {
        this.pdfId = pdfId;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(pdfId);
        dest.writeString(pdfUrl);
        dest.writeString(userId);
    }
}
