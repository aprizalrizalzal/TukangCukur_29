package com.bro.barbershop.ui.report.pdf;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bro.barbershop.R;
import com.bro.barbershop.adapter.report.pdf.ListPdfAdapter;
import com.bro.barbershop.databinding.FragmentPdfBinding;
import com.bro.barbershop.model.report.pdf.Pdf;
import com.bro.barbershop.model.role.Role;
import com.bro.barbershop.model.user.User;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class PdfFragment extends Fragment {

    public static final String EXTRA_PDF = "extra_pdf";
    private final String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final ArrayList<Pdf> listPdf = new ArrayList<>();
    private final ArrayList<User> listUser = new ArrayList<>();
    private FragmentPdfBinding binding;
    private View view;
    private Calendar calendar;
    private SimpleDateFormat simpleDateFormatId;
    private String currentUserEmail;
    private DatabaseReference databaseReferenceUser, databaseReferenceRole, databaseReferencePdf;
    private StorageReference storageReference;
    private ListPdfAdapter adapter;
    private CustomProgressDialog progressDialog;
    public PdfFragment() {
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
        binding = FragmentPdfBinding.inflate(getLayoutInflater(), container, false);
        view = binding.getRoot();

        calendar = Calendar.getInstance();
        simpleDateFormatId = new SimpleDateFormat("dd MMMM yyyy - HH:mm", new Locale("id", "ID"));

        progressDialog = new CustomProgressDialog(getActivity());

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser !=null){
            currentUserEmail = currentUser.getEmail();
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://barbershopbro-55cec-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReferenceUser = database.getReference("user");
        databaseReferenceRole = database.getReference("role");
        databaseReferencePdf = database.getReference("pdf");

        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        adapter = new ListPdfAdapter();
        binding.tvEmptyData.setText(getString(R.string.no_data_available_pdf));
        adapter.registerAdapterDataObserver(new RecyclerViewEmptyData(binding.rvPdf, binding.tvEmptyData));

        binding.rvPdf.setHasFixedSize(true);
        binding.rvPdf.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPdf.setAdapter(adapter);

        binding.refreshLayout.setOnRefreshListener(() -> {
            changeRealtimeDatabaseUser(currentUserEmail);
            binding.refreshLayout.setRefreshing(false);
        });

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
                                        changeRealtimeDatabasePdf(finalUser, role);
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

    private void changeRealtimeDatabasePdf(User finalUser, Role role) {
        progressDialog.ShowProgressDialog();
        listUser.clear();
        databaseReferenceUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String username = null;
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        listUser.add(user);
                        username = user.getUsername();
                    }
                }
                adapter.setListUser(listUser);
                listPdf.clear();
                String finalUsername = username;
                databaseReferencePdf.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressDialog.DismissProgressDialog();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Pdf pdf = dataSnapshot.getValue(Pdf.class);
                            if (pdf != null) {
                                if (currentUserEmail.equals(getString(R.string.default_email)) || role.getRole().equals(getString(R.string.administrator))) {
                                    listPdf.add(pdf);
                                } else if (finalUser.getUserId().equals(pdf.getUserId())) {
                                    listPdf.add(pdf);
                                    adapter.setActivateButtons(false);
                                }
                            }
                        }
                        adapter.setListPdf(listPdf);

                        adapter.setOnItemClickCallback(pdf -> {
                            if (arePermissionsGranted()) {
                                selectedPdf(pdf);
                            } else {
                                requestPermissions();
                            }
                        });
                        adapter.setOnItemClickCallbackDelete(deletePdf -> PdfFragment.this.deleteSelectedPdf(deletePdf, finalUsername));
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
    private void requestPermissions() {
        resultLauncher.launch(permissions);
    }
    private boolean arePermissionsGranted() {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    private final ActivityResultLauncher<String[]> resultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissionsResult -> {
        boolean allPermissionsGranted = true;
        for (Boolean isGranted : permissionsResult.values()) {
            if (!isGranted) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            Toast.makeText(requireContext(), getString(R.string.again), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), getString(R.string.failed), Toast.LENGTH_SHORT).show();
        }
    });

    private void selectedPdf(Pdf pdf) {
        Bundle bundle = new Bundle();

        bundle.putParcelable(EXTRA_PDF, pdf);
        Navigation.findNavController(view).navigate(R.id.action_nav_pdf_to_detail_pdf_fragment, bundle);
    }

    private void deleteSelectedPdf(Pdf deletePdf, String finalUsername) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(getString(R.string.delete)).setMessage(getString(R.string.f_delete_pdf, finalUsername)).setCancelable(false)
                .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.cancel())
                .setPositiveButton(getString(R.string.yes), (dialog, id) -> deletePdfByPdfId(deletePdf));
        builder.show();
    }

    private void deletePdfByPdfId(Pdf deletePdf) {
        progressDialog.ShowProgressDialog();
        databaseReferencePdf.child(deletePdf.getPdfId()).removeValue().addOnSuccessListener(unused -> {
            StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(deletePdf.getPdfUrl());
            storageReference.delete().addOnSuccessListener(aVoid -> {
                progressDialog.DismissProgressDialog();
                changeRealtimeDatabaseUser(currentUserEmail);
            }).addOnFailureListener(e -> {
                progressDialog.DismissProgressDialog();
                Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        changeRealtimeDatabaseUser(currentUserEmail);
    }
}