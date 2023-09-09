package com.bro.barbershop.ui.user.profile;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bro.barbershop.R;
import com.bro.barbershop.databinding.FragmentEmailBinding;
import com.bro.barbershop.model.user.User;
import com.bro.barbershop.utils.progressBar.CustomProgressDialog;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EmailFragment extends Fragment {

    boolean isEmptyFields = false;
    private FragmentEmailBinding binding;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReferenceUser;
    private String currentUserEmail, userId, email, newEmail, password;
    private CustomProgressDialog progressDialog;
    private View view;

    public EmailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentEmailBinding.inflate(getLayoutInflater(), container, false);
        view = binding.getRoot();

        progressDialog = new CustomProgressDialog(requireActivity());

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://barbershopbro-55cec-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReferenceUser = database.getReference("user");

        if (currentUser != null) {
            currentUserEmail = currentUser.getEmail();
        } else {
            requireActivity().finish();
        }

        binding.btnUpdate.setOnClickListener(v -> {
            email = Objects.requireNonNull(binding.tietEmail.getText()).toString().trim();
            newEmail = Objects.requireNonNull(binding.tietNewEmail.getText()).toString().trim();
            password = Objects.requireNonNull(binding.tietPassword.getText()).toString();

            isEmptyFields = validateFields();
        });

        return view;
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

        if (newEmail.isEmpty()) {
            binding.tilNewEmail.setError(getString(R.string.new_email_required));
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.email_format));
            return false;
        } else {
            binding.tilNewEmail.setErrorEnabled(false);
        }

        if (password.isEmpty()) {
            binding.tilPassword.setError(getString(R.string.password_required));
            return false;
        } else {
            binding.tilPassword.setErrorEnabled(false);
        }

        updateEmail();
        return true;
    }

    private void updateEmail() {
        progressDialog.ShowProgressDialog();
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                currentUser.updateEmail(newEmail).addOnSuccessListener(unused -> {
                    progressDialog.DismissProgressDialog();
                    updateNewEmail();
                }).addOnFailureListener(e -> {
                    progressDialog.DismissProgressDialog();
                    Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
                });
            } else {
                progressDialog.DismissProgressDialog();
                Toast.makeText(requireContext(), R.string.authentication_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateNewEmail() {
        progressDialog.ShowProgressDialog();
        Query query = databaseReferenceUser.orderByChild("email").equalTo(currentUserEmail);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user != null) {
                            userId = user.getUserId();
                        }
                    }
                    Map<String, Object> mapUser = new HashMap<>();
                    mapUser.put("email", newEmail);

                    databaseReferenceUser.child(userId).updateChildren(mapUser).addOnSuccessListener(unused -> {
                        progressDialog.DismissProgressDialog();
                        updateTokenId(userId);
                    }).addOnFailureListener(e -> {
                        progressDialog.DismissProgressDialog();
                        Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.DismissProgressDialog();
            }
        });
    }

    private void updateTokenId(String userId) {
        progressDialog.ShowProgressDialog();
        Map<String, Object> mapUsers = new HashMap<>();
        mapUsers.put("tokenId", "");

        databaseReferenceUser.child(userId).updateChildren(mapUsers).addOnSuccessListener(unused -> {
            progressDialog.DismissProgressDialog();
            auth.signOut();
            Navigation.findNavController(view).navigate(R.id.action_nav_email_to_login_activity);
            requireActivity().finish();
        }).addOnFailureListener(e -> progressDialog.DismissProgressDialog());
    }
}