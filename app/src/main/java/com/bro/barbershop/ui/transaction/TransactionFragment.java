package com.bro.barbershop.ui.transaction;

import static android.content.DialogInterface.BUTTON_POSITIVE;

import static com.bro.barbershop.utils.messagingService.BarberShopMessagingService.NOTIFICATION_URL;
import static com.bro.barbershop.utils.messagingService.BarberShopMessagingService.SERVER_KEY;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bro.barbershop.R;
import com.bro.barbershop.adapter.transaction.ListTransactionAdapter;
import com.bro.barbershop.databinding.EditTransactionBinding;
import com.bro.barbershop.databinding.FragmentTransactionBinding;
import com.bro.barbershop.model.customer.Customer;
import com.bro.barbershop.model.report.Report;
import com.bro.barbershop.model.shaving.Shaving;
import com.bro.barbershop.model.transaction.Transaction;
import com.bro.barbershop.model.user.User;
import com.bro.barbershop.model.role.Role;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TransactionFragment extends Fragment {
    private static final String TAG = "sendNotification";
    private final ArrayList<Transaction> listTransaction = new ArrayList<>();
    private final ArrayList<User> listUser = new ArrayList<>();
    private final ArrayList<Customer> listCustomer = new ArrayList<>();
    private final ArrayList<Shaving> listShaving = new ArrayList<>();
    private FragmentTransactionBinding binding;
    boolean isEmptyFields = false;
    private String selectedFilterTransactionDate;
    private Calendar calendar;
    private SimpleDateFormat simpleDateFormatId;

    private View view;
    private String currentUserEmail;
    private DatabaseReference databaseReferenceUser, databaseReferenceRole, databaseReferenceReport, databaseReferenceTransaction, databaseReferenceCustomer, databaseReferenceShaving;
    private ListTransactionAdapter adapter;
    private String selectedPrice;
    private CustomProgressDialog progressDialog;

    public TransactionFragment() {
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
        binding = FragmentTransactionBinding.inflate(getLayoutInflater(), container, false);
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
        databaseReferenceReport = database.getReference("report");
        databaseReferenceTransaction = database.getReference("transaction");
        databaseReferenceCustomer = database.getReference("customer");
        databaseReferenceShaving = database.getReference("shaving");

        adapter = new ListTransactionAdapter();
        binding.tvEmptyData.setText(getString(R.string.no_data_available_transaction));
        adapter.registerAdapterDataObserver(new RecyclerViewEmptyData(binding.rvTransaction, binding.tvEmptyData));

        binding.rvTransaction.setHasFixedSize(true);
        binding.rvTransaction.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTransaction.setAdapter(adapter);

        binding.refreshLayout.setOnRefreshListener(() -> {
            changeRealtimeDatabaseUser(currentUserEmail);
            binding.refreshLayout.setRefreshing(false);
        });

        binding.fabFilter.setOnClickListener(v -> showDatePickerDialog());
        
        return view;
    }

    private void showDatePickerDialog() {
        int currentYear  = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, monthOfYear, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, monthOfYear, dayOfMonth);

                    selectedFilterTransactionDate = getDateWithoutTime(simpleDateFormatId.format(selectedCalendar.getTime()));

                    changeRealtimeDatabaseUser(currentUserEmail);
                },
                currentYear, month, day
        );

        datePickerDialog.show();
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
                                        changeRealtimeDatabaseTransaction(finalUser, role);
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

    private void changeRealtimeDatabaseTransaction(User finalUser, Role role) {
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
                                listTransaction.clear();
                                databaseReferenceTransaction.orderByChild("transactionDate").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        progressDialog.DismissProgressDialog();
                                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                            Transaction transaction = dataSnapshot.getValue(Transaction.class);
                                            String transactionDate;
                                            if (transaction != null) {
                                                transactionDate = transaction.getTransactionDate();
                                                String _transactionDate = getDateWithoutTime(transactionDate);
                                                if (selectedFilterTransactionDate != null){
                                                    if (_transactionDate.equals(selectedFilterTransactionDate) && (currentUserEmail.equals(getString(R.string.default_email)) || role.getRole().equals(getString(R.string.administrator)))) {
                                                        listTransaction.add(transaction);
                                                        getShavingData(transaction.getShavingId());
                                                    } else if (_transactionDate.equals(selectedFilterTransactionDate) && finalUser.getUserId().equals(transaction.getUserId())) {
                                                        listTransaction.add(transaction);
                                                        adapter.setActivateButtons(false);
                                                    }
                                                } else {
                                                    if (currentUserEmail.equals(getString(R.string.default_email)) || role.getRole().equals(getString(R.string.administrator))) {
                                                        listTransaction.add(transaction);
                                                        getShavingData(transaction.getShavingId());
                                                    } else if (finalUser.getUserId().equals(transaction.getUserId())) {
                                                        listTransaction.add(transaction);
                                                        adapter.setActivateButtons(false);
                                                    }
                                                }
                                            }
                                        }
                                        adapter.setListTransaction(listTransaction);

                                        adapter.setOnItemClickCallbackEdit(TransactionFragment.this::editSelectedTransaction);
                                        adapter.setOnItemClickCallbackDelete(TransactionFragment.this::deleteSelectedTransaction);
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

    private void getShavingData(String shavingId) {
        databaseReferenceShaving.child(shavingId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Shaving shaving = snapshot.getValue(Shaving.class);
                    if (shaving != null) {
                        selectedPrice = MoneyTextWatcher.formatCurrency(shaving.getPrice());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.DismissProgressDialog();
            }
        });
    }

    private String getDateWithoutTime(String dateTime) {
        return dateTime.substring(0, dateTime.indexOf("-")).trim();
    }

    private void editSelectedTransaction(Transaction editTransaction) {
        if (editTransaction.getPaymentStatus()){
            changeRealtimeDatabaseReport(editTransaction);
        } else {
            editSelectedTransactionPaymentStatus(editTransaction);
        }
    }

    private void editSelectedTransactionPaymentStatus(Transaction editTransaction) {
        EditTransactionBinding dialogBinding;

        View customView;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        dialogBinding = EditTransactionBinding.inflate(LayoutInflater.from(requireContext()), null, false);
        customView = dialogBinding.getRoot();

        builder.setTitle(getString(R.string.edit_payment_status))
                .setView(customView)
                .setCancelable(false)
                .setNegativeButton(getString(R.string.no), ((dialog, id) -> dialog.cancel()))
                .setPositiveButton(getString(R.string.yes), null);

        String[] paymentStatus = {getString(R.string.paid_off), getString(R.string.not_yet_paid)};
        ArrayAdapter<String> paymentStatusAdapter = new ArrayAdapter<>(requireContext(), R.layout.list_mactv, paymentStatus);
        dialogBinding.mactvPaymentStatus.setAdapter(paymentStatusAdapter);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(BUTTON_POSITIVE).setOnClickListener(view -> {
            String selectedPaymentStatus = dialogBinding.mactvPaymentStatus.getText().toString();
            if (selectedPaymentStatus.isEmpty()) {
                dialogBinding.tilPaymentStatus.setError(getString(R.string.payment_status_required));
                isEmptyFields = true;
            } else {
                dialogBinding.tilPaymentStatus.setErrorEnabled(false);
            }

            if (!isEmptyFields) {
                editSelectedPaymentStatus(editTransaction, selectedPaymentStatus);
                dialog.dismiss();
            } else {
                isEmptyFields = false;
            }
        }));

        dialog.show();
    }

    private void changeRealtimeDatabaseReport(Transaction editTransaction) {
        progressDialog.ShowProgressDialog();

        String transactionId = editTransaction.getTransactionId();

        databaseReferenceReport.orderByChild("transactionId").equalTo(transactionId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressDialog.DismissProgressDialog();
                if (dataSnapshot.exists()) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                    builder.setTitle(getString(R.string.report)).setMessage(getString(R.string.f_add_report_exists, editTransaction.getTransactionDate())).setCancelable(false)
                            .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.cancel())
                            .setPositiveButton(getString(R.string.look), (dialog, id) -> Navigation.findNavController(view).navigate(R.id.action_nav_transaction_to_nav_report));
                    builder.show();
                } else {
                    createRealtimeDatabaseReport(editTransaction);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialog.DismissProgressDialog();
                Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createRealtimeDatabaseReport(Transaction editTransaction) {
        progressDialog.ShowProgressDialog();

        String reportId = UUID.randomUUID().toString();

        BigDecimal employeeSalaryValue = MoneyTextWatcher.parseCurrencyValue(selectedPrice);
        BigDecimal halfEmployeeSalaryValue = employeeSalaryValue.divide(BigDecimal.valueOf(2), RoundingMode.HALF_DOWN);

        String valueEmployeeSalary = String.valueOf(halfEmployeeSalaryValue);

        String reportDate = simpleDateFormatId.format(calendar.getTime());

        String userId = editTransaction.getUserId();
        String transactionId = editTransaction.getTransactionId();

        Report report = new Report(reportId, Double.parseDouble(valueEmployeeSalary), reportDate , userId, transactionId);

        databaseReferenceReport.child(reportId).setValue(report).addOnSuccessListener(unused -> {
            progressDialog.DismissProgressDialog();
            getTokenForNotification(editTransaction);
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void getTokenForNotification(Transaction editTransaction) {
        progressDialog.ShowProgressDialog();

        databaseReferenceUser.child(editTransaction.getUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.DismissProgressDialog();
                if (snapshot.exists()){
                    String tokenId = null;
                    User user = snapshot.getValue(User.class);
                    if (user !=null) {
                        tokenId = user.getTokenId();
                    }
                    sendDataEditTransaction(editTransaction, tokenId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.DismissProgressDialog();
                Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendDataEditTransaction(Transaction editTransaction, String tokenId) {
        JSONObject to = new JSONObject();
        JSONObject data = new JSONObject();

        String statusPayment;

        if (!editTransaction.getPaymentStatus()){
            statusPayment = getString(R.string.not_yet_paid);
        } else {
            statusPayment = getString(R.string.paid_off);
        }

        try {
            data.put("title", editTransaction.getTransactionDate());
            data.put("message", getString(R.string.f_message_report, editTransaction.getTransactionDate(), statusPayment));

            to.put("to", tokenId);
            to.put("data", data);

            sendNotification(to);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendNotification(JSONObject to) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, NOTIFICATION_URL, to, response -> {
            Log.d(TAG, "sendNotification: " + response);
            changeRealtimeDatabaseUser(currentUserEmail); }, error -> Log.e(TAG, "sendNotification: ", error)) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> map = new HashMap<>();
                map.put("Authorization", "key=" + SERVER_KEY);
                map.put("Content-type", "application/json");
                return map;
            }

            @Override
            public String getBodyContentType() {

                return "application/json";
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        request.setRetryPolicy(new DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }

    private void editSelectedPaymentStatus(Transaction editTransaction, String selectedPaymentStatus) {
        progressDialog.ShowProgressDialog();

        String transactionId = editTransaction.getTransactionId();
        boolean newPaymentStatus = selectedPaymentStatus.equals(getString(R.string.paid_off));

        Map<String, Object> mapPaymentStatus = new HashMap<>();
        mapPaymentStatus.put("paymentStatus", newPaymentStatus);

        databaseReferenceTransaction.child(transactionId).updateChildren(mapPaymentStatus).addOnSuccessListener(unused -> {
            progressDialog.DismissProgressDialog();
            createRealtimeDatabaseReport(editTransaction);
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(requireActivity(), R.string.failed, Toast.LENGTH_SHORT).show();
        });
    }
    private void deleteSelectedTransaction(Transaction deleteTransaction) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(getString(R.string.delete)).setMessage(getString(R.string.f_delete_transaction, deleteTransaction.getTransactionDate())).setCancelable(false)
                .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.cancel())
                .setPositiveButton(getString(R.string.yes), (dialog, id) -> deleteTransactionByTransactionId(deleteTransaction));
        builder.show();
    }

    private void deleteTransactionByTransactionId(Transaction deleteTransaction) {
        progressDialog.ShowProgressDialog();
        databaseReferenceTransaction.child(deleteTransaction.getTransactionId()).removeValue().addOnSuccessListener(unused -> {
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