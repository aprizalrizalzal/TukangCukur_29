package com.bro.barbershop.ui.auth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import com.bro.barbershop.MainActivity;
import com.bro.barbershop.R;
import com.bro.barbershop.databinding.ActivityRegisterBinding;
import com.bro.barbershop.utils.progressBar.CustomProgressDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private CustomProgressDialog progressDialog;
    private String email, password, confirmPassword;
    boolean isEmptyFields = false;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog = new CustomProgressDialog(RegisterActivity.this);

        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser !=null) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(RegisterActivity.this, R.string.please_login_first, Toast.LENGTH_SHORT).show();
        }

        binding.btnRegister.setOnClickListener(v -> {
            email = Objects.requireNonNull(binding.tietEmail.getText()).toString().trim();
            password = Objects.requireNonNull(binding.tietPassword.getText()).toString();
            confirmPassword = Objects.requireNonNull(binding.tietConfirmPassword.getText()).toString();

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
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.email_format));
            return false;
        } else {
            binding.tilEmail.setErrorEnabled(false);
        }

        if (password.isEmpty()) {
            binding.tilPassword.setError(getString(R.string.password_required));
            return false;
        } else if (password.length() < 8) {
            binding.tilPassword.setError(getString(R.string.password_length));
            return false;
        } else {
            binding.tilPassword.setErrorEnabled(false);
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.setError(getString(R.string.password_confirm_required));
            return false;
        } else if (!confirmPassword.equals(password)) {
            binding.tilConfirmPassword.setError(getString(R.string.invalid_confirm_password));
            return false;
        } else {
            binding.tilConfirmPassword.setErrorEnabled(false);
        }

        register();
        return true;
    }

    private void register() {
        progressDialog.ShowProgressDialog();
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                progressDialog.DismissProgressDialog();
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                progressDialog.DismissProgressDialog();
                Toast.makeText(getApplicationContext(), R.string.registration_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }
}