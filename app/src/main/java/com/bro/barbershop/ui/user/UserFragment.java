package com.bro.barbershop.ui.user;

import static android.content.DialogInterface.BUTTON_POSITIVE;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.bro.barbershop.R;
import com.bro.barbershop.adapter.user.ListUserAdapter;
import com.bro.barbershop.databinding.EditRoleBinding;
import com.bro.barbershop.databinding.FragmentUserBinding;
import com.bro.barbershop.model.role.Role;
import com.bro.barbershop.model.user.User;
import com.bro.barbershop.utils.progressBar.CustomProgressDialog;
import com.bro.barbershop.utils.recyclerView.RecyclerViewEmptyData;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserFragment extends Fragment {
    public static final String EXTRA_USER = "extra_user";
    private final ArrayList<User> listUser = new ArrayList<>();
    private final ArrayList<Role> listRole = new ArrayList<>();
    private FragmentUserBinding binding;
    boolean isEmptyFields = false;
    private View view;
    private DatabaseReference databaseReferenceUser, databaseReferenceRole;
    private ListUserAdapter adapter;
    private CustomProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentUserBinding.inflate(getLayoutInflater(), container, false);
        view = binding.getRoot();

        progressDialog = new CustomProgressDialog(getActivity());

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://barbershopbro-55cec-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReferenceUser = database.getReference("user");
        databaseReferenceRole = database.getReference("role");

        adapter = new ListUserAdapter();
        binding.tvEmptyData.setText(getString(R.string.no_data_available_user));
        adapter.registerAdapterDataObserver(new RecyclerViewEmptyData(binding.rvUser, binding.tvEmptyData));

        binding.rvUser.setHasFixedSize(true);
        binding.rvUser.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvUser.setAdapter(adapter);

        binding.refreshLayout.setOnRefreshListener(() -> {
            listRealtimeDatabaseUser();
            binding.refreshLayout.setRefreshing(false);
        });

        return view;
    }

    private void listRealtimeDatabaseUser() {
        listRole.clear();
        progressDialog.ShowProgressDialog();
        databaseReferenceRole.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Role role = dataSnapshot.getValue(Role.class);
                    if (role != null) {
                        listRole.add(role);
                    }
                }
                adapter.setListRole(listRole);

                listUser.clear();
                databaseReferenceUser.orderByChild("username").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressDialog.DismissProgressDialog();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            User user = dataSnapshot.getValue(User.class);
                            if (user != null && !user.getEmail().equals(getString(R.string.default_email))) {
                                listUser.add(user);
                            }
                        }
                        adapter.setListUser(listUser);

                        adapter.setOnItemClickCallback(UserFragment.this::showSelectedUser);
                        adapter.setOnItemClickCallbackEditRole(UserFragment.this::editRoleUserSelectedUser);
                        adapter.setOnItemClickCallbackDelete(UserFragment.this::deleteSelectedUser);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressDialog.DismissProgressDialog();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.DismissProgressDialog();
            }
        });
    }

    private void editRoleUserSelectedUser(User editRoleUser) {
        EditRoleBinding dialogBinding;

        View customView;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        dialogBinding = EditRoleBinding.inflate(LayoutInflater.from(requireContext()), null, false);
        customView = dialogBinding.getRoot();

        builder.setTitle(getString(R.string.edit_role))
                .setView(customView)
                .setCancelable(false)
                .setNegativeButton(getString(R.string.no), ((dialog, id) -> dialog.cancel()))
                .setPositiveButton(getString(R.string.yes), null);

        String[] role = {getString(R.string.administrator), getString(R.string.employee)};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(), R.layout.list_mactv, role);
        dialogBinding.mactvRole.setAdapter(roleAdapter);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(BUTTON_POSITIVE).setOnClickListener(view -> {
            String selectedRole = dialogBinding.mactvRole.getText().toString();

            if (selectedRole.isEmpty()) {
                dialogBinding.tilRole.setError(getString(R.string.role_required));
                isEmptyFields = true;
            } else {
                dialogBinding.tilRole.setErrorEnabled(false);
            }

            if (!isEmptyFields) {
                editSelectedRoleUser(editRoleUser, selectedRole);
                dialog.dismiss();
            } else {
                isEmptyFields = false;
            }
        }));

        dialog.show();
    }

    private void editSelectedRoleUser(User editRoleUser, String selectedRole) {
        progressDialog.ShowProgressDialog();

        String roleId = editRoleUser.getRoleId();

        Map<String, Object> mapRole = new HashMap<>();
        mapRole.put("role", selectedRole);

        databaseReferenceRole.child(roleId).updateChildren(mapRole).addOnSuccessListener(unused -> {
            progressDialog.DismissProgressDialog();
            listRealtimeDatabaseUser();
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(requireActivity(), R.string.failed, Toast.LENGTH_SHORT).show();
        });

    }

    private void showSelectedUser(User detailUser) {
        Bundle bundle = new Bundle();

        bundle.putParcelable(EXTRA_USER, detailUser);
        Navigation.findNavController(view).navigate(R.id.action_nav_user_to_nav_profile, bundle);
    }
    private void deleteSelectedUser(User deleteUser) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(getString(R.string.delete)).setMessage(getString(R.string.f_delete_user, deleteUser.getUsername())).setCancelable(false)
                .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.cancel())
                .setPositiveButton(getString(R.string.yes), (dialog, id) -> deleteUserByUsername());
        builder.show();
    }

    private void deleteUserByUsername() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(getString(R.string.sorry)).setMessage(getString(R.string.f_firebase_billing_plans, getString(R.string.spark), getString(R.string.blaze))).setCancelable(false)
                .setNeutralButton(getString(R.string.yes), (dialog, id) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onStart() {
        listRealtimeDatabaseUser();
        super.onStart();
    }
}