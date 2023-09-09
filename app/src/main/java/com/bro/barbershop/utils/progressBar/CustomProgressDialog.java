package com.bro.barbershop.utils.progressBar;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.bro.barbershop.R;

public class CustomProgressDialog {
    private final Activity activity;
    private Dialog progressDialog;

    public CustomProgressDialog(Activity activity) {
        this.activity = activity;
    }

    public void ShowProgressDialog() {
        progressDialog = new Dialog(activity);
        progressDialog.setContentView(R.layout.progress_bar);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setCancelable(true);
        progressDialog.create();
        progressDialog.show();
    }

    public void DismissProgressDialog() {
        progressDialog.dismiss();
    }
}
