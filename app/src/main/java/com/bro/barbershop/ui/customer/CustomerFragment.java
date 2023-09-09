package com.bro.barbershop.ui.customer;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bro.barbershop.R;
import com.bro.barbershop.adapter.customer.ListCustomerAdapter;
import com.bro.barbershop.databinding.FragmentCustomerBinding;
import com.bro.barbershop.model.customer.Customer;
import com.bro.barbershop.model.user.User;
import com.bro.barbershop.model.role.Role;
import com.bro.barbershop.utils.progressBar.CustomProgressDialog;
import com.bro.barbershop.utils.recyclerView.RecyclerViewEmptyData;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class CustomerFragment extends Fragment {
    public static final String EXTRA_CUSTOMER = "extra_customer";
    private final ArrayList<Customer> listCustomer = new ArrayList<>();
    private FragmentCustomerBinding binding;
    private View view;

    private String currentUserEmail;
    private DatabaseReference databaseReferenceUser, databaseReferenceRole, databaseReferenceCustomer;
    private ListCustomerAdapter adapter;
    private CustomProgressDialog progressDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentCustomerBinding.inflate(getLayoutInflater(), container, false);
        view = binding.getRoot();

        progressDialog = new CustomProgressDialog(getActivity());

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser !=null){
            currentUserEmail = currentUser.getEmail();
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://barbershopbro-55cec-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReferenceUser = database.getReference("user");
        databaseReferenceRole = database.getReference("role");
        databaseReferenceCustomer = database.getReference("customer");

        adapter = new ListCustomerAdapter();
        binding.tvEmptyData.setText(getString(R.string.no_data_available_customer));
        adapter.registerAdapterDataObserver(new RecyclerViewEmptyData(binding.rvCustomer, binding.tvEmptyData));

        binding.rvCustomer.setHasFixedSize(true);
        binding.rvCustomer.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCustomer.setAdapter(adapter);

        binding.refreshLayout.setOnRefreshListener(() -> {
            changeRealtimeDatabaseUser(currentUserEmail);
            binding.refreshLayout.setRefreshing(false);
        });

        binding.fab.setOnClickListener(v -> Navigation.createNavigateOnClickListener(R.id.action_nav_customer_to_add_or_edit_customer_fragment).onClick(v));

        return view;
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
                        User finalUser = user;
                        databaseReferenceRole.child(user.getRoleId()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                progressDialog.DismissProgressDialog();
                                if (snapshot.exists()){
                                    Role role = snapshot.getValue(Role.class);
                                    if (role !=null){
                                        changeRealtimeDatabaseCustomer(finalUser, role);
                                    }
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

    private void changeRealtimeDatabaseCustomer(User finalUser, Role role) {
        progressDialog.ShowProgressDialog();

        listCustomer.clear();
        databaseReferenceCustomer.orderByChild("customer").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.DismissProgressDialog();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Customer customer = dataSnapshot.getValue(Customer.class);
                    if (customer != null && currentUserEmail.equals(getString(R.string.default_email)) || role.getRole().equals(getString(R.string.administrator))) {
                        listCustomer.add(customer);
                        binding.fab.setVisibility(View.INVISIBLE);
                    } else if (customer != null && finalUser.getUserId().equals(customer.getUserId())){
                        listCustomer.add(customer);
                        adapter.setActivateButtons(false);
                    }
                }
                adapter.setListCustomer(listCustomer);
                adapter.setOnItemClickCallbackOpenInNew(CustomerFragment.this::showOpenInNewCustomer);
                adapter.setOnItemClickCallbackEdit(CustomerFragment.this::showSelectedCustomer);
                adapter.setOnItemClickCallbackDelete(CustomerFragment.this::deleteSelectedCustomer);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.DismissProgressDialog();
            }
        });
    }

    private void showSelectedCustomer(Customer editCustomer) {
        Bundle bundle = new Bundle();

        bundle.putParcelable(EXTRA_CUSTOMER, editCustomer);
        Navigation.findNavController(view).navigate(R.id.action_nav_customer_to_add_or_edit_customer_fragment, bundle);
    }
    private void showOpenInNewCustomer(Customer openInNewCustomer) {
        Bundle bundle = new Bundle();

        bundle.putParcelable(EXTRA_CUSTOMER, openInNewCustomer);
        Navigation.findNavController(view).navigate(R.id.action_nav_customer_to_detail_customer_fragment, bundle);
    }
    
    private void deleteSelectedCustomer(Customer deleteCustomer) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(getString(R.string.delete)).setMessage(getString(R.string.f_delete_customer, deleteCustomer.getCustomer())).setCancelable(false)
                .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.cancel())
                .setPositiveButton(getString(R.string.yes), (dialog, id) -> deleteCustomerByCustomerId(deleteCustomer));
        builder.show();
    }

    private void deleteCustomerByCustomerId(Customer deleteCustomer) {
        progressDialog.ShowProgressDialog();
        databaseReferenceCustomer.child(deleteCustomer.getCustomerId()).removeValue().addOnSuccessListener(unused -> {
            progressDialog.DismissProgressDialog();
            changeRealtimeDatabaseUser(currentUserEmail);
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onStart() {
        changeRealtimeDatabaseUser(currentUserEmail);
        super.onStart();
    }
}