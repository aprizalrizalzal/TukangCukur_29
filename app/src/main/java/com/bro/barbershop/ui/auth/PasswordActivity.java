package com.bro.barbershop.ui.auth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.bro.barbershop.MainActivity;
import com.bro.barbershop.R;
import com.bro.barbershop.databinding.ActivityPasswordBinding;
import com.bro.barbershop.utils.progressBar.CustomProgressDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class PasswordActivity extends AppCompatActivity {
    private ActivityPasswordBinding binding;
    private CustomProgressDialog progressDialog;
    private String email;
    boolean isEmptyFields = false;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog = new CustomProgressDialog(PasswordActivity.this);

        auth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser !=null) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(PasswordActivity.this, R.string.please_login_first, Toast.LENGTH_SHORT).show();

        }

        binding.btnReset.setOnClickListener(v -> {
            email = Objects.requireNonNull(binding.tietEmail.getText()).toString().trim();
            isEmptyFields = validateFields();
        });

        binding.tvBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private boolean validateFields() {
        if (email.isEmpty()) {
            binding.tilEmail.setError(getString(R.string.email_required));
            return false;
        } else {
            binding.tilEmail.setErrorEnabled(false);
        }
        resetPassword();
        return true;
    }

    private void resetPassword() {
        progressDialog.ShowProgressDialog();
        auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                progressDialog.DismissProgressDialog();
            } else {
                progressDialog.DismissProgressDialog();
                Toast.makeText(getApplicationContext(), R.string.unregistered_email, Toast.LENGTH_SHORT).show();
            }
        });
    }
}