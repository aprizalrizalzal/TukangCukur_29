package com.bro.barbershop.ui.customer;

import static com.bro.barbershop.ui.customer.CustomerFragment.EXTRA_CUSTOMER;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bro.barbershop.R;
import com.bro.barbershop.databinding.FragmentAddOrEditCustomerBinding;
import com.bro.barbershop.model.customer.Customer;
import com.bro.barbershop.model.user.User;
import com.bro.barbershop.utils.progressBar.CustomProgressDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import java.util.UUID;

public class AddOrEditCustomerFragment extends Fragment {

    boolean isEmptyFields = false;
    private FragmentAddOrEditCustomerBinding binding;
    private FirebaseUser currentUser;
    private View view;
    private CustomProgressDialog progressDialog;
    private Customer extraCustomer;
    private String customer,phoneNumber, notes;
    private DatabaseReference databaseReferenceUser, databaseReferenceCustomer;

    public AddOrEditCustomerFragment() {
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
        binding = FragmentAddOrEditCustomerBinding.inflate(getLayoutInflater(), container, false);
        view = binding.getRoot();

        progressDialog = new CustomProgressDialog(getActivity());

        if (getArguments() != null) {
            extraCustomer = getArguments().getParcelable(EXTRA_CUSTOMER);
        }

        if (extraCustomer != null) {
            viewExtraCustomer();
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://barbershopbro-55cec-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReferenceUser = database.getReference("user");
        databaseReferenceCustomer = database.getReference("customer");

        binding.btnSave.setOnClickListener(v -> {
            customer = Objects.requireNonNull(binding.tietCustomer.getText()).toString();
            phoneNumber = Objects.requireNonNull(binding.tietPhoneNumber.getText()).toString();
            notes = Objects.requireNonNull(binding.tietNotes.getText()).toString();

            isEmptyFields = validateFields();
        });

        binding.btnUpdate.setOnClickListener(v -> {
            customer = Objects.requireNonNull(binding.tietCustomer.getText()).toString();
            phoneNumber = Objects.requireNonNull(binding.tietPhoneNumber.getText()).toString();
            notes = Objects.requireNonNull(binding.tietNotes.getText()).toString();

            isEmptyFields = validateUpdateFields();
        });

        return view;
    }

    private void viewExtraCustomer() {
        binding.tietCustomer.setText(extraCustomer.getCustomer());
        binding.tietPhoneNumber.setText(String.valueOf(extraCustomer.getPhoneNumber()));
        binding.tietNotes.setText(String.valueOf(extraCustomer.getNotes()));
        binding.btnSave.setVisibility(View.GONE);
        binding.btnUpdate.setVisibility(View.VISIBLE);
    }

    private boolean validateFields() {
        if (customer.isEmpty()) {
            binding.tilCustomer.setError(getString(R.string.customer_required));
            return false;
        } else {
            binding.tilCustomer.setErrorEnabled(false);
        }

        if (phoneNumber.isEmpty()) {
            binding.tilPhoneNumber.setError(getString(R.string.phone_number_required));
            return false;
        } else {
            binding.tilPhoneNumber.setErrorEnabled(false);
        }

        if (notes.isEmpty()) {
            binding.tilNotes.setError(getString(R.string.notes_required));
            return false;
        } else {
            binding.tilNotes.setErrorEnabled(false);
        }

        changeRealtimeDatabaseUser(currentUser.getEmail());

        return true;
    }

    private void changeRealtimeDatabaseUser(String currentUserEmail) {
        progressDialog.ShowProgressDialog();

        Query query = databaseReferenceUser.orderByChild("email").equalTo(currentUserEmail);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = null;
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        user = dataSnapshot.getValue(User.class);
                    }
                    if (user != null) {
                        Query query = databaseReferenceCustomer.orderByChild("phoneNumber").equalTo(phoneNumber);
                        User finalUser = user;
                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                progressDialog.DismissProgressDialog();
                                if (snapshot.exists()) {
                                    Customer shavingCustomer = null;
                                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                        shavingCustomer = dataSnapshot.getValue(Customer.class);
                                    }

                                    if (shavingCustomer != null) {
                                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                                        Customer finalShavingCustomer = shavingCustomer;
                                        builder.setTitle(getString(R.string.customer)).setMessage(getString(R.string.f_add_customer_exists, shavingCustomer.getPhoneNumber())).setCancelable(false)
                                                .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.cancel())
                                                .setPositiveButton(getString(R.string.look), (dialog, id) -> customerExists(finalShavingCustomer));
                                        builder.show();
                                    }
                                } else {
                                    createRealtimeDatabaseCustomer(finalUser.getUserId());
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                progressDialog.DismissProgressDialog();
                            }
                        });
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.DismissProgressDialog();
            }
        });
    }

    private void createRealtimeDatabaseCustomer(String userId) {
        progressDialog.ShowProgressDialog();

        String customerId = UUID.randomUUID().toString();
        Customer _customer = new Customer(customerId, customer, notes, phoneNumber, userId);

        databaseReferenceCustomer.child(customerId).setValue(_customer).addOnSuccessListener(unused -> {
            progressDialog.DismissProgressDialog();
            Navigation.findNavController(view).navigateUp();
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void customerExists(Customer finalShavingCustomer) {
        Bundle bundle = new Bundle();

        bundle.putParcelable(EXTRA_CUSTOMER, finalShavingCustomer);
        Navigation.findNavController(view).navigate(R.id.action_add_or_edit_customer_fragment_to_detail_customer_fragment, bundle);
    }

    private boolean validateUpdateFields() {
        if (customer.isEmpty()) {
            binding.tilCustomer.setError(getString(R.string.customer_required));
            return false;
        } else {
            binding.tilCustomer.setErrorEnabled(false);
        }

        if (phoneNumber.isEmpty()) {
            binding.tilPhoneNumber.setError(getString(R.string.phone_number_required));
            return false;
        } else {
            binding.tilPhoneNumber.setErrorEnabled(false);
        }

        if (notes.isEmpty()) {
            binding.tilNotes.setError(getString(R.string.notes_required));
            return false;
        } else {
            binding.tilNotes.setErrorEnabled(false);
        }

        updateCustomer();

        return true;
    }

    private void updateCustomer() {
        progressDialog.ShowProgressDialog();

        Map<String, Object> map = new HashMap<>();
        map.put("customer", customer);
        map.put("phoneNumber", phoneNumber);
        map.put("notes", notes);

        databaseReferenceCustomer.child(extraCustomer.getCustomerId()).updateChildren(map).addOnSuccessListener(unused -> {
            progressDialog.DismissProgressDialog();
            Navigation.findNavController(view).navigateUp();
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
        });
    }
}