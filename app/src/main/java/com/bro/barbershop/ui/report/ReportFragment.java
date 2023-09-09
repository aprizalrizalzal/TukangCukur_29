package com.bro.barbershop.ui.report;

import static android.content.DialogInterface.BUTTON_POSITIVE;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.bro.barbershop.R;
import com.bro.barbershop.adapter.report.ListReportAdapter;
import com.bro.barbershop.databinding.AddReportBinding;
import com.bro.barbershop.databinding.EditReportBinding;
import com.bro.barbershop.databinding.FragmentReportBinding;
import com.bro.barbershop.model.customer.Customer;
import com.bro.barbershop.model.report.Report;
import com.bro.barbershop.model.report.pdf.Pdf;
import com.bro.barbershop.model.role.Role;
import com.bro.barbershop.model.shaving.Shaving;
import com.bro.barbershop.model.transaction.Transaction;
import com.bro.barbershop.model.user.User;
import com.bro.barbershop.utils.progressBar.CustomProgressDialog;
import com.bro.barbershop.utils.recyclerView.RecyclerViewEmptyData;
import com.bro.barbershop.utils.textWatcher.MoneyTextWatcher;
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
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ReportFragment extends Fragment {
    private final String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final ArrayList<Report> listReport = new ArrayList<>();
    private final ArrayList<Transaction> listTransaction = new ArrayList<>();
    private final ArrayList<User> listUser = new ArrayList<>();
    private final ArrayList<Customer> listCustomer = new ArrayList<>();
    private final ArrayList<Shaving> listShaving = new ArrayList<>();
    private ArrayAdapter<User> userAdapter;
    boolean isEmptyFields = false;
    private User selectedUser;
    private String selectedReportDate, selectedFilterReportDate;
    private FragmentReportBinding binding;
    private Calendar calendar;
    private SimpleDateFormat simpleDateFormatId;
    private String currentUserEmail;
    private String reportTitle;
    private String pdfId;
    private DatabaseReference databaseReferenceUser, databaseReferenceRole, databaseReferencePdf, databaseReferenceReport, databaseReferenceTransaction, databaseReferenceCustomer, databaseReferenceShaving;
    private StorageReference storageReference;
    private ListReportAdapter adapter;
    private CustomProgressDialog progressDialog;

    public ReportFragment() {
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
        binding = FragmentReportBinding.inflate(getLayoutInflater(), container, false);
        View view = binding.getRoot();

        pdfId = UUID.randomUUID().toString();

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
        databaseReferenceReport = database.getReference("report");
        databaseReferenceTransaction = database.getReference("transaction");
        databaseReferenceCustomer = database.getReference("customer");
        databaseReferenceShaving = database.getReference("shaving");

        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        adapter = new ListReportAdapter();
        binding.tvEmptyData.setText(getString(R.string.no_data_available_report));
        adapter.registerAdapterDataObserver(new RecyclerViewEmptyData(binding.rvReport, binding.tvEmptyData));

        binding.rvReport.setHasFixedSize(true);
        binding.rvReport.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReport.setAdapter(adapter);

        binding.refreshLayout.setOnRefreshListener(() -> {
            changeRealtimeDatabaseUser(currentUserEmail);
            binding.refreshLayout.setRefreshing(false);
        });

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_main_nav, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_view_pdf_report) {
                    Navigation.findNavController(view).navigate(R.id.action_nav_report_to_nav_pdf);
                } else if (menuItem.getItemId() == R.id.action_create_pdf_report) {
                    if (arePermissionsGranted()) {
                        createReportPdf();
                    } else {
                        requestPermissions();
                    }
                }

                return false;
            }

        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        binding.fabFilter.setOnClickListener(v -> showReportDatePickerDialog(null));

        return view;
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
            createReportPdf();
        } else {
            Toast.makeText(requireContext(), R.string.permission_is_required, Toast.LENGTH_SHORT).show();
        }
    });

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
                                        changeRealtimeDatabaseReport(finalUser, role);
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

    private void changeRealtimeDatabaseReport(User finalUser, Role role) {
        progressDialog.ShowProgressDialog();
        listUser.clear();
        databaseReferenceUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        listUser.add(user);
                    }
                }
                adapter.setListUser(listUser);
                listTransaction.clear();
                databaseReferenceTransaction.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Transaction transaction = dataSnapshot.getValue(Transaction.class);
                            if (transaction != null) {
                                listTransaction.add(transaction);
                            }
                        }
                        adapter.setListTransaction(listTransaction);
                        listShaving.clear();
                        databaseReferenceShaving.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                    Shaving shaving = dataSnapshot.getValue(Shaving.class);
                                    if (shaving != null) {
                                        listShaving.add(shaving);
                                    }
                                }
                                adapter.setListShaving(listShaving);
                                listCustomer.clear();
                                databaseReferenceCustomer.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                            Customer customer = dataSnapshot.getValue(Customer.class);
                                            if (customer != null) {
                                                listCustomer.add(customer);
                                            }
                                        }
                                        adapter.setListCustomer(listCustomer);
                                        listReport.clear();
                                        databaseReferenceReport.orderByChild("reportDate").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                progressDialog.DismissProgressDialog();
                                                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                                    Report report = dataSnapshot.getValue(Report.class);
                                                    String reportDate;
                                                    if (report != null) {
                                                        reportDate = report.getReportDate();
                                                        String _reportDate = getDateWithoutTimeAndDay(reportDate);
                                                        if (selectedFilterReportDate != null){
                                                            if (_reportDate.equals(selectedFilterReportDate) && (currentUserEmail.equals(getString(R.string.default_email)) || role.getRole().equals(getString(R.string.administrator)))) {
                                                                listReport.add(report);
                                                            } else if (_reportDate.equals(selectedFilterReportDate) && finalUser.getUserId().equals(report.getUserId())) {
                                                                listReport.add(report);
                                                                adapter.setActivateButtons(false);
                                                            }
                                                        } else {
                                                            if (currentUserEmail.equals(getString(R.string.default_email)) || role.getRole().equals(getString(R.string.administrator))) {
                                                                listReport.add(report);
                                                            } else if (finalUser.getUserId().equals(report.getUserId())) {
                                                                listReport.add(report);
                                                                adapter.setActivateButtons(false);
                                                            }
                                                        }
                                                    }
                                                }
                                                adapter.setListReport(listReport);
                                                adapter.setOnItemClickCallbackDelete(ReportFragment.this::deleteSelectedReport);
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
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.DismissProgressDialog();
            }
        });
    }

    private void editSelectedReport(Report editReport) {
        EditReportBinding dialogBinding;

        View customView;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        dialogBinding = EditReportBinding.inflate(LayoutInflater.from(requireContext()), null, false);
        customView = dialogBinding.getRoot();

        builder.setTitle(getString(R.string.edit_report_status))
                .setView(customView)
                .setCancelable(false)
                .setNegativeButton(getString(R.string.no), ((dialog, id) -> dialog.cancel()))
                .setPositiveButton(getString(R.string.yes), null);

        String[] reportStatus = {getString(R.string.finished), getString(R.string.not_finished_yet)};
        ArrayAdapter<String> reportStatusAdapter = new ArrayAdapter<>(requireContext(), R.layout.list_mactv, reportStatus);
        dialogBinding.mactvReportStatus.setAdapter(reportStatusAdapter);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(BUTTON_POSITIVE).setOnClickListener(view -> {
            String selectedReportStatus = dialogBinding.mactvReportStatus.getText().toString();

            if (selectedReportStatus.isEmpty()) {
                dialogBinding.tilReportStatus.setError(getString(R.string.report_status_required));
                isEmptyFields = true;
            } else {
                dialogBinding.tilReportStatus.setErrorEnabled(false);
            }

            if (!isEmptyFields) {
                editSelectedReportStatus(editReport, selectedReportStatus);
                dialog.dismiss();
            } else {
                isEmptyFields = false;
            }
        }));

        dialog.show();
    }

    private void editSelectedReportStatus(Report editReport, String selectedReportStatus) {
        progressDialog.ShowProgressDialog();

        String reportId = editReport.getReportId();
        boolean newReportStatus = selectedReportStatus.equals(getString(R.string.finished));

        Map<String, Object> mapReportStatus = new HashMap<>();
        mapReportStatus.put("reportStatus", newReportStatus);

        databaseReferenceReport.child(reportId).updateChildren(mapReportStatus).addOnSuccessListener(unused -> {
            progressDialog.DismissProgressDialog();
            changeRealtimeDatabaseUser(currentUserEmail);
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(requireActivity(), R.string.failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void deleteSelectedReport(Report deleteReport) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(getString(R.string.delete)).setMessage(getString(R.string.f_delete_report, deleteReport.getReportDate())).setCancelable(false)
                .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.cancel())
                .setPositiveButton(getString(R.string.yes), (dialog, id) -> deleteReportByReportId(deleteReport));
        builder.show();
    }

    private void deleteReportByReportId(Report deleteReport) {
        progressDialog.ShowProgressDialog();

        Map<String, Object> mapTransaction = new HashMap<>();
        mapTransaction.put("paymentStatus", false);

        databaseReferenceTransaction.child(deleteReport.getTransactionId()).updateChildren(mapTransaction).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                databaseReferenceReport.child(deleteReport.getReportId()).removeValue().addOnSuccessListener(unused -> {
                    progressDialog.DismissProgressDialog();
                    changeRealtimeDatabaseUser(currentUserEmail);
                }).addOnFailureListener(e -> {
                    progressDialog.DismissProgressDialog();
                    Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_SHORT).show();
                });
            } else {
                progressDialog.DismissProgressDialog();
                Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void createReportPdf() {
        AddReportBinding dialogBinding;

        View customView;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        dialogBinding = AddReportBinding.inflate(LayoutInflater.from(requireContext()), null, false);
        customView = dialogBinding.getRoot();

        builder.setTitle(getString(R.string.create_pdf_report))
                .setView(customView)
                .setCancelable(false)
                .setNegativeButton(getString(R.string.no), ((dialog, id) -> dialog.cancel()))
                .setPositiveButton(getString(R.string.yes), null);

        AlertDialog dialog = builder.create();

        dialogBinding.mactvUsername.setThreshold(1);

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
                                if (snapshot.exists()){
                                    Role role = snapshot.getValue(Role.class);
                                    if (role != null) {
                                        if (!currentUserEmail.equals(getString(R.string.default_email)) && !role.getRole().equals(getString(R.string.administrator))) {
                                            selectedUser = finalUser;
                                            dialogBinding.mactvUsername.setText(finalUser.getUsername());
                                            dialogBinding.mactvUsername.setEnabled(false);
                                        } else {
                                            databaseReferenceUser.orderByChild("username").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    if (snapshot.exists()) {
                                                        listUser.clear();
                                                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                                            User user = dataSnapshot.getValue(User.class);
                                                            if (user != null && !user.getEmail().equals(getString(R.string.default_email))) {
                                                                listUser.add(user);
                                                            }
                                                        }
                                                        userAdapter = new ArrayAdapter<>(requireContext(), R.layout.list_mactv, listUser);
                                                        dialogBinding.mactvUsername.setAdapter(userAdapter);
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    Toast.makeText(requireContext(), getString(R.string.failed), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(requireContext(), getString(R.string.failed), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), getString(R.string.failed), Toast.LENGTH_SHORT).show();
            }
        });

        dialogBinding.mactvUsername.setOnItemClickListener((parent, view, position, id) -> selectedUser = listUser.get(position));
        dialogBinding.tietReportDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showReportDatePickerDialog(dialogBinding);
            }
        });

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            selectedReportDate = Objects.requireNonNull(dialogBinding.tietReportDate.getText()).toString();

            if (selectedUser == null) {
                dialogBinding.tilUsername.setError(getString(R.string.username_required));
                isEmptyFields = true;
            } else {
                dialogBinding.tilUsername.setErrorEnabled(false);
            }

            if (selectedReportDate.isEmpty()) {
                dialogBinding.tilReportDate.setError(getString(R.string.report_date_required));
                isEmptyFields = true;
            } else {
                dialogBinding.tilReportDate.setErrorEnabled(false);
            }

            if (!isEmptyFields) {
                try {
                    generateReportPDF(selectedUser, selectedReportDate);
                } catch (IOException | DocumentException e) {
                    throw new RuntimeException(e);
                }
                dialog.dismiss();
            } else {
                isEmptyFields = false;
            }
        }));

        dialog.show();
    }

    private void showReportDatePickerDialog(AddReportBinding dialogBinding) {
        int currentYear  = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, monthOfYear, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, monthOfYear, dayOfMonth);

                    if (dialogBinding != null){
                        selectedReportDate = getDateWithoutTimeAndDay(simpleDateFormatId.format(selectedCalendar.getTime()));
                        dialogBinding.tietReportDate.setText(selectedReportDate);
                    } else {
                        selectedFilterReportDate = getDateWithoutTimeAndDay(simpleDateFormatId.format(selectedCalendar.getTime()));
                        changeRealtimeDatabaseUser(currentUserEmail);
                    }
                },
                currentYear, month, 1
        );
        datePickerDialog.show();
    }

    private String getDateWithoutTimeAndDay(String dateTime) {
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID"));

        try {
            Date date = simpleDateFormatId.parse(dateTime);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
        return dateTime;
    }

    private void generateReportPDF(User selectedUser, String selectedReportDate) throws IOException, DocumentException {
        reportTitle = String.format("%s %s %s.pdf", selectedUser.getUsername(), selectedReportDate, pdfId);

        String pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + reportTitle;

        File pdfFile = new File(pdfPath);

        FileOutputStream fileOutputStream = new FileOutputStream(pdfFile);

        Document document = new Document();
        document.setPageSize(PageSize.A4);

        document.addAuthor("Program Studi S1 Sistem Informasi");
        document.addCreator("Asrullah");

        document.addCreationDate();
        Font fontNormal = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL, BaseColor.BLACK);
        Font fontBold = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD, BaseColor.BLACK);

        Chunk title = new Chunk("Laporan Pendapatan Karyawan " + selectedUser.getUsername() + "\nBulan " + selectedReportDate, fontBold);
        Paragraph paragraphTitle = new Paragraph(title);
        paragraphTitle.setAlignment(Element.ALIGN_CENTER);

        PdfPTable tableHeader = new PdfPTable(3);
        tableHeader.setWidthPercentage(100);
        tableHeader.setWidths(new float[]{1, 5, 3});

        PdfPCell cellHeader;
        cellHeader = new PdfPCell(new Phrase("No", fontBold));
        cellHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellHeader.setRowspan(2);
        tableHeader.addCell(cellHeader);

        cellHeader = new PdfPCell(new Phrase("Tanggal", fontBold));
        cellHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellHeader.setRowspan(2);
        tableHeader.addCell(cellHeader);

        cellHeader = new PdfPCell(new Phrase("Pendapatan", fontBold));
        cellHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellHeader.setRowspan(2);
        tableHeader.addCell(cellHeader);

        PdfPTable tableContent = new PdfPTable(3);
        tableContent.setWidthPercentage(100);
        tableContent.setWidths(new float[]{1, 5, 3});

        List<Report> filteredReport = new ArrayList<>();

        double totalAmount = 0;

        for (Report report : listReport) {
            String reportDate = report.getReportDate();
            String _reportDate = getDateWithoutTimeAndDay(reportDate);
            if (report.getUserId().equals(selectedUser.getUserId()) && _reportDate.equals(selectedReportDate)) {
                filteredReport.add(report);
                double total = report.getEmployeeSalary();
                totalAmount = totalAmount + total;
            }
        }

        PdfPCell cellContent;
        for (int i = 0; i < filteredReport.size(); i++) {
            Report report = filteredReport.get(i);

            String number = String.valueOf(i + 1);
            String reportDate = report.getReportDate();
            String employeeSalary = MoneyTextWatcher.formatCurrency(report.getEmployeeSalary());

            cellContent = new PdfPCell(new Phrase(number + ".", fontNormal));
            cellContent.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellContent.setVerticalAlignment(Element.ALIGN_MIDDLE);
            tableContent.addCell(cellContent);

            cellContent = new PdfPCell(new Phrase(reportDate, fontNormal));
            cellContent.setHorizontalAlignment(Element.ALIGN_LEFT);
            cellContent.setVerticalAlignment(Element.ALIGN_MIDDLE);
            tableContent.addCell(cellContent);

            cellContent = new PdfPCell(new Phrase(employeeSalary, fontNormal));
            cellContent.setHorizontalAlignment(Element.ALIGN_LEFT);
            cellContent.setVerticalAlignment(Element.ALIGN_MIDDLE);
            tableContent.addCell(cellContent);
        }

        PdfPTable tableTotal = new PdfPTable(2);
        tableTotal.setWidthPercentage(100);
        tableTotal.setWidths(new float[]{6, 3});

        PdfPCell cellTotal;

        cellTotal = new PdfPCell(new Phrase("Total", fontBold));
        cellTotal.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellTotal.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tableTotal.addCell(cellTotal);

        cellTotal = new PdfPCell(new Phrase(MoneyTextWatcher.formatCurrency(totalAmount), fontBold));
        cellTotal.setHorizontalAlignment(Element.ALIGN_LEFT);
        cellTotal.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tableTotal.addCell(cellTotal);

        cellTotal = new PdfPCell(new Phrase("", fontBold));
        cellTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellTotal.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tableTotal.addCell(cellTotal);

        PdfWriter.getInstance(document, fileOutputStream).setPageEvent(new PageFooter());

        if (getActivity() != null) {
            Bitmap bitmap = BitmapFactory.decodeResource(getActivity().getResources(), R.raw.tukang_cukur_29);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            float marginLeftInch = 0.75f;
            float marginRightInch = 0.75f;

            float marginLeft = marginLeftInch * 72;
            float marginRight = marginRightInch * 72;

            float effectiveWidth = PageSize.A4.getWidth() - marginLeft - marginRight;

            Image image = Image.getInstance(byteArray);
            image.scaleToFit(effectiveWidth, PageSize.A4.getHeight());
            image.setAlignment(Element.ALIGN_CENTER);

            document.open();

            document.add(image);
            document.add(new Paragraph("\n"));
            document.add(paragraphTitle);
            document.add(new Paragraph("\n"));
            document.add(tableHeader);
            document.add(tableContent);
            document.add(tableTotal);

            document.close();

            uploadPdf(pdfFile.getPath());

        }
    }

    private void uploadPdf(String path) {
        progressDialog.ShowProgressDialog();
        String pathReport = "/report/" + reportTitle;
        storageReference.child(pathReport).putFile(Uri.fromFile(new File(path))).addOnSuccessListener(taskSnapshot -> {
            progressDialog.DismissProgressDialog();
            downloadUriPdf(pathReport);
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void downloadUriPdf(String pathReport) {
        progressDialog.ShowProgressDialog();
        storageReference.child(pathReport).getDownloadUrl().addOnSuccessListener(uri -> {
            progressDialog.DismissProgressDialog();
            String pdfUrl = uri.toString();
            createRealtimeDatabasePdf(pdfUrl);
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void createRealtimeDatabasePdf(String pdfUrl) {
        progressDialog.ShowProgressDialog();

        Pdf pdf = new Pdf(pdfId, pdfUrl, selectedUser.getUserId());
        databaseReferencePdf.child(pdfId).setValue(pdf).addOnSuccessListener(command -> {
            progressDialog.DismissProgressDialog();
            changeRealtimeDatabaseUser(currentUserEmail);
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onStart() {
        changeRealtimeDatabaseUser(currentUserEmail);
        super.onStart();
    }
}