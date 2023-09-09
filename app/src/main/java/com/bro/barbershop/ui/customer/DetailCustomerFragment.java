package com.bro.barbershop.ui.customer;

import static com.bro.barbershop.ui.customer.CustomerFragment.EXTRA_CUSTOMER;
import static com.bro.barbershop.utils.messagingService.BarberShopMessagingService.NOTIFICATION_URL;
import static com.bro.barbershop.utils.messagingService.BarberShopMessagingService.SERVER_KEY;

import android.graphics.Color;
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
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bro.barbershop.R;
import com.bro.barbershop.adapter.shaving.ListShavingAdapter;
import com.bro.barbershop.databinding.AddTransactionBinding;
import com.bro.barbershop.databinding.FragmentDetailCustomerBinding;
import com.bro.barbershop.model.customer.Customer;
import com.bro.barbershop.model.shaving.Shaving;
import com.bro.barbershop.model.transaction.Transaction;
import com.bro.barbershop.model.user.User;
import com.bro.barbershop.model.role.Role;
import com.bro.barbershop.utils.textDrawable.ColorGenerator;
import com.bro.barbershop.utils.progressBar.CustomProgressDialog;
import com.bro.barbershop.utils.recyclerView.RecyclerViewEmptyData;
import com.bro.barbershop.utils.textDrawable.TextDrawable;
import com.bro.barbershop.utils.textWatcher.MoneyTextWatcher;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class DetailCustomerFragment extends Fragment {

    private static final String TAG = "sendNotification";
    private FragmentDetailCustomerBinding binding;

    private Calendar calendar;
    private SimpleDateFormat simpleDateFormatId;
    private View view;
    boolean isEmptyFields = false;
    private CustomProgressDialog progressDialog;
    private Customer extraCustomer;
    private String currentUserEmail;
    private final ArrayList<Shaving> listShaving = new ArrayList<>();
    private ListShavingAdapter adapter;
    private DatabaseReference databaseReferenceUser, databaseReferenceRole, databaseReferenceCustomer, databaseReferenceShaving, databaseReferenceTransaction;

    public DetailCustomerFragment() {
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
        binding = FragmentDetailCustomerBinding.inflate(getLayoutInflater(), container, false);
        view = binding.getRoot();

        calendar = Calendar.getInstance();
        simpleDateFormatId = new SimpleDateFormat("dd MMMM yyyy - HH:mm", new Locale("id", "ID"));

        progressDialog = new CustomProgressDialog(getActivity());

        if (getArguments() != null) {
            extraCustomer = getArguments().getParcelable(EXTRA_CUSTOMER);
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://barbershopbro-55cec-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReferenceUser = database.getReference("user");
        databaseReferenceRole = database.getReference("role");
        databaseReferenceCustomer = database.getReference("customer");
        databaseReferenceShaving = database.getReference("shaving");
        databaseReferenceTransaction = database.getReference("transaction");

        if (extraCustomer != null) {
            viewExtraCustomer();
        }

        if (currentUser !=null) {
            currentUserEmail = currentUser.getEmail();
        }

        adapter = new ListShavingAdapter();
        binding.tvEmptyData.setText(getString(R.string.no_data_available_shaving));
        adapter.registerAdapterDataObserver(new RecyclerViewEmptyData(binding.rvShaving, binding.tvEmptyData));

        binding.rvShaving.setHasFixedSize(true);
        binding.rvShaving.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvShaving.setAdapter(adapter);

        binding.refreshLayout.setOnRefreshListener(() -> {
            changeRealtimeDatabaseUser(currentUserEmail);
            binding.refreshLayout.setRefreshing(false);
        });

        binding.fabExtendedAdd.setOnClickListener(v -> {
            AddTransactionBinding dialogBinding;

            View customView;
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
            dialogBinding = AddTransactionBinding.inflate(LayoutInflater.from(requireContext()), null, false);
            customView = dialogBinding.getRoot();

            builder.setTitle(getString(R.string.add_shaving))
                    .setView(customView)
                    .setCancelable(false)
                    .setNegativeButton(getString(R.string.no), ((dialog, id) -> dialog.cancel()))
                    .setPositiveButton(getString(R.string.yes), null);

            AlertDialog dialog = builder.create();

            dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String hairstyle = Objects.requireNonNull(dialogBinding.tietHairstyle.getText()).toString();
                String price = Objects.requireNonNull(dialogBinding.tietPrice.getText()).toString();

                if (hairstyle.isEmpty()) {
                    dialogBinding.tilHairstyle.setError(getString(R.string.hairstyle_required));
                    isEmptyFields = true;
                } else {
                    dialogBinding.tilHairstyle.setErrorEnabled(false);
                }

                if (price.isEmpty()) {
                    dialogBinding.tilPrice.setError(getString(R.string.price_required));
                    isEmptyFields = true;
                } else {
                    dialogBinding.tilPrice.setErrorEnabled(false);
                }

                if (!isEmptyFields) {
                    createRealtimeDatabaseShaving(hairstyle, price);
                    dialog.dismiss();
                } else {
                    isEmptyFields = false;
                }
            }));

            dialog.show();
        });

        return view;
    }
    private void createRealtimeDatabaseShaving(String hairstyle, String price) {
        progressDialog.ShowProgressDialog();

        String shavingId = UUID.randomUUID().toString();

        BigDecimal priceValue = MoneyTextWatcher.parseCurrencyValue(price);
        String valuePrice = String.valueOf(priceValue);

        String shavingDate = simpleDateFormatId.format(calendar.getTime());

        String customerId = extraCustomer.getCustomerId();
        Shaving shaving = new Shaving(shavingId, customerId, hairstyle, Double.parseDouble(valuePrice), shavingDate);

        databaseReferenceShaving.child(shavingId).setValue(shaving).addOnSuccessListener(unused -> {
            progressDialog.DismissProgressDialog();
            changeRealtimeDatabaseUser(currentUserEmail);
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
        });
    }
    private void changeRealtimeDatabaseUser(String currentUserEmail) {
        progressDialog.ShowProgressDialog();

        Query query = databaseReferenceUser.orderByChild("email").equalTo(currentUserEmail);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User userChange = null;
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user !=null){
                            userChange = user;
                        }
                    }
                    if (userChange != null) {
                        databaseReferenceRole.child(userChange.getRoleId()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()){
                                    Role role = snapshot.getValue(Role.class);
                                    if (role !=null){
                                        listShaving.clear();
                                        databaseReferenceShaving.orderByChild("shavingDate").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                progressDialog.DismissProgressDialog();
                                                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                                    Shaving shaving = dataSnapshot.getValue(Shaving.class);
                                                    if (shaving != null && role.getRole().equals(getString(R.string.administrator)) && extraCustomer.getCustomerId().equals(shaving.getCustomerId())) {
                                                        listShaving.add(shaving);
                                                        binding.fabExtendedAdd.setVisibility(View.GONE);
                                                    } else if (shaving != null && role.getRole().equals(getString(R.string.employee)) && extraCustomer.getCustomerId().equals(shaving.getCustomerId())){
                                                        listShaving.add(shaving);
                                                        adapter.setActivateButtons(false);
                                                    }
                                                }
                                                adapter.setListShaving(listShaving);
                                                adapter.setOnItemClickCallbackSend(sendShaving -> {
                                                    if (!role.getRole().equals(getString(R.string.administrator))) {
                                                        sendSelectedShaving(sendShaving);
                                                    }
                                                });
                                                adapter.setOnItemClickCallbackDelete(DetailCustomerFragment.this::deleteSelectedShaving);
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
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.DismissProgressDialog();
            }
        });
    }

    private void sendSelectedShaving(Shaving sendShaving) {
        progressDialog.ShowProgressDialog();

        databaseReferenceUser.child(extraCustomer.getUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.DismissProgressDialog();
                if (snapshot.exists()){
                    User user = snapshot.getValue(User.class);
                    if (user !=null) {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                        builder.setTitle(getString(R.string.send_transaction)).setMessage(getString(R.string.f_send_shaving, sendShaving.getShavingDate())).setCancelable(false)
                                .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.cancel())
                                .setPositiveButton(getString(R.string.yes), (dialog, id) -> changeRealtimeDatabaseTransaction(sendShaving));
                        builder.show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.DismissProgressDialog();
                Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void changeRealtimeDatabaseTransaction(Shaving sendShaving) {
        progressDialog.ShowProgressDialog();

        String shavingId = sendShaving.getShavingId();

        databaseReferenceTransaction.orderByChild("shavingId").equalTo(shavingId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressDialog.DismissProgressDialog();
                if (dataSnapshot.exists()) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                    builder.setTitle(getString(R.string.transaction)).setMessage(getString(R.string.f_add_transaction_exists, extraCustomer.getCustomer())).setCancelable(false)
                            .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.cancel())
                            .setPositiveButton(getString(R.string.look), (dialog, id) -> Navigation.findNavController(view).navigate(R.id.action_detail_customer_fragment_to_nav_transaction));
                    builder.show();
                } else {
                    createRealtimeDatabaseTransaction(sendShaving);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialog.DismissProgressDialog();
                Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createRealtimeDatabaseTransaction(Shaving sendShaving) {
        progressDialog.ShowProgressDialog();

        String transactionId = UUID.randomUUID().toString();
        String shavingId = sendShaving.getShavingId();
        Boolean paymentStatus = false;

        String transactionDate = simpleDateFormatId.format(calendar.getTime());

        Transaction transaction = new Transaction(transactionId, extraCustomer.getUserId(), shavingId, transactionDate, paymentStatus);

        databaseReferenceTransaction.child(transactionId).setValue(transaction).addOnSuccessListener(unused -> {
            progressDialog.DismissProgressDialog();
            getTokenForNotification(transaction);
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void getTokenForNotification(Transaction transaction) {
        progressDialog.ShowProgressDialog();

        databaseReferenceUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.DismissProgressDialog();
                if (snapshot.exists()){
                    String roleId = null, tokenId = null;
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                        User user = dataSnapshot.getValue(User.class);
                        if (user !=null) {
                            roleId = user.getRoleId(); tokenId = user.getTokenId();
                        }

                        String finalTokenId = tokenId;
                        databaseReferenceRole.child(roleId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()){
                                    Role role = snapshot.getValue(Role.class);
                                    if (role != null && role.getRole().equals(getString(R.string.administrator))){
                                        sendDataTransaction(transaction, finalTokenId);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                progressDialog.DismissProgressDialog();
                                Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.DismissProgressDialog();
                Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendDataTransaction(Transaction transaction, String tokenId) {
        JSONObject to = new JSONObject();
        JSONObject data = new JSONObject();

        String statusPayment;

        if (!transaction.getPaymentStatus()){
            statusPayment = getString(R.string.not_yet_paid);
        } else {
            statusPayment = getString(R.string.paid_off);
        }

        try {
            data.put("title", transaction.getTransactionDate());
            data.put("message", getString(R.string.f_message_transaction, transaction.getTransactionDate(), statusPayment));

            to.put("to", tokenId);
            to.put("data", data);

            sendNotification(to);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendNotification(JSONObject to) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, NOTIFICATION_URL, to, response -> Log.d(TAG, "sendNotification: " + response), error -> Log.e(TAG, "sendNotification: ", error)) {
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

    private void deleteSelectedShaving(Shaving deleteShaving) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(getString(R.string.delete)).setMessage(getString(R.string.f_delete_shaving, deleteShaving.getShavingDate())).setCancelable(false)
                .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.cancel())
                .setPositiveButton(getString(R.string.yes), (dialog, id) -> deleteShavingByShavingId(deleteShaving));
        builder.show();
    }

    private void deleteShavingByShavingId(Shaving deleteShaving) {
        progressDialog.ShowProgressDialog();

        databaseReferenceCustomer.child(extraCustomer.getCustomerId()).child("shaving").child(deleteShaving.getShavingId()).removeValue().addOnSuccessListener(unused -> {
            progressDialog.DismissProgressDialog();
            changeRealtimeDatabaseUser(currentUserEmail);
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_SHORT).show();
        });
    }

    private void viewExtraCustomer() {
        String customerName = extraCustomer.getCustomer();
        String initials = getInitials(extraCustomer.getCustomer());

        TextDrawable drawable = TextDrawable.builder()
                .beginConfig()
                .textColor(Color.WHITE)
                .toUpperCase()
                .endConfig()
                .buildRoundRect(initials, ColorGenerator.MATERIAL.getColor(customerName), 16);

        Glide.with(requireActivity())
                .load(customerName)
                .placeholder(drawable)
                .apply(new RequestOptions().override(128,128))
                .into(binding.imgCustomer);

        binding.tvCustomer.setText(extraCustomer.getCustomer());
        binding.tietPhoneNumber.setText(String.valueOf(extraCustomer.getPhoneNumber()));
        binding.tietNotes.setText(String.valueOf(extraCustomer.getNotes()));
    }

    private String getInitials(String customer) {
        StringBuilder initials = new StringBuilder();

        String[] words = customer.split("\\s+");

        for (String word : words) {
            if (!word.isEmpty()) {
                initials.append(Character.toUpperCase(word.charAt(0)));
            }
        }

        return initials.toString();
    }

    @Override
    public void onStart() {
        changeRealtimeDatabaseUser(currentUserEmail);
        super.onStart();
    }
}