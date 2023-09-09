package com.bro.barbershop.adapter.report;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bro.barbershop.R;
import com.bro.barbershop.databinding.ListReportBinding;
import com.bro.barbershop.model.customer.Customer;
import com.bro.barbershop.model.report.Report;
import com.bro.barbershop.model.shaving.Shaving;
import com.bro.barbershop.model.transaction.Transaction;
import com.bro.barbershop.model.user.User;
import com.bro.barbershop.utils.textDrawable.ColorGenerator;
import com.bro.barbershop.utils.textDrawable.TextDrawable;
import com.bro.barbershop.utils.textWatcher.MoneyTextWatcher;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class ListReportAdapter extends RecyclerView.Adapter<ListReportAdapter.ViewHolder> {
    private static final List<User> user = new ArrayList<>();
    private static final List<Transaction> transaction = new ArrayList<>();
    private static final List<Customer> customer = new ArrayList<>();
    private static final List<Shaving> shaving = new ArrayList<>();
    private final List<Report> report = new ArrayList<>();

    private ListReportAdapter.OnItemClickCallbackDelete onItemClickCallbackDelete;
    private boolean activate = true;

    @SuppressLint("NotifyDataSetChanged")
    public void setListUser(List<User> user) {
        ListReportAdapter.user.clear();
        ListReportAdapter.user.addAll(user);
        notifyDataSetChanged();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void setListTransaction(List<Transaction> transaction) {
        ListReportAdapter.transaction.clear();
        ListReportAdapter.transaction.addAll(transaction);
        notifyDataSetChanged();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void setListShaving(List<Shaving> shaving) {
        ListReportAdapter.shaving.clear();
        ListReportAdapter.shaving.addAll(shaving);
        notifyDataSetChanged();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void setListCustomer(List<Customer> customer) {
        ListReportAdapter.customer.clear();
        ListReportAdapter.customer.addAll(customer);
        notifyDataSetChanged();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void setListReport(List<Report> report) {
        this.report.clear();
        this.report.addAll(report);
        notifyDataSetChanged();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void setActivateButtons(boolean activate) {
        this.activate = activate;
        notifyDataSetChanged();
    }

    public void setOnItemClickCallbackDelete(ListReportAdapter.OnItemClickCallbackDelete onItemClickCallbackDelete) {
        this.onItemClickCallbackDelete = onItemClickCallbackDelete;
    }

    @NonNull
    @Override
    public ListReportAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_report, parent, false);
        return new ListReportAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListReportAdapter.ViewHolder holder, int position) {
        holder.bind(report.get(position));
        if (activate) {
            holder.binding.imgBtnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.binding.imgBtnDelete.setVisibility(View.INVISIBLE);
        }
        holder.binding.imgBtnDelete.setOnClickListener(v -> onItemClickCallbackDelete.onItemClickedDelete(report.get(holder.getAdapterPosition())));
    }

    @Override
    public int getItemCount() {
        return report.size();
    }

    public interface OnItemClickCallbackDelete {
        void onItemClickedDelete(Report report);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ListReportBinding binding;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ListReportBinding.bind(itemView);

        }
        public void bind(Report report) {

            binding.tvReportDate.setText(report.getReportDate());
            binding.tvEmployeeSalary.setText(MoneyTextWatcher.formatCurrency(report.getEmployeeSalary()));

            String userId = report.getUserId();
            User user = findUserById(userId);

            if (user !=null){
                String username = user.getUsername();
                String initials = getInitials(username);

                TextDrawable drawable = TextDrawable.builder()
                        .beginConfig()
                        .textColor(Color.WHITE)
                        .toUpperCase()
                        .endConfig()
                        .buildRoundRect(initials, ColorGenerator.MATERIAL.getColor(username), 16);

                Glide.with(itemView)
                        .load(username)
                        .placeholder(drawable)
                        .apply(new RequestOptions().override(128,128))
                        .into(binding.imgListTransaction);

                binding.tvUsername.setText(username);
            }

            String transactionId = report.getTransactionId();
            Transaction transaction = findTransactionById(transactionId);

            String shavingId = null;
            if (transaction != null){
                shavingId = transaction.getShavingId();
            }

            Shaving shaving = findShavingById(shavingId);

            String customerId = null;
            if (shaving !=null){
                customerId = shaving.getCustomerId();
                binding.tvHairstyle.setText(shaving.getHairstyle());
            }

            Customer customer = findCustomerById(customerId);
            if (customer !=null){
                binding.tvCustomer.setText(customer.getCustomer());
            }

        }

        private User findUserById(String userId) {
            for (User user : user) {
                if (user.getUserId().equals(userId)) {
                    return user;
                }
            }
            return null;
        }
        private Transaction findTransactionById(String transactionId) {
            for (Transaction transaction : transaction) {
                if (transaction.getTransactionId().equals(transactionId)) {
                    return transaction;
                }
            }
            return null;
        }
        private Customer findCustomerById(String customerId) {
            for (Customer customer : customer) {
                if (customer.getCustomerId().equals(customerId)) {
                    return customer;
                }
            }
            return null;
        }
        private Shaving findShavingById(String shavingId) {
            for (Shaving shaving : shaving) {
                if (shaving.getShavingId().equals(shavingId)) {
                    return shaving;
                }
            }
            return null;
        }

        private String getInitials(String username) {
            StringBuilder initials = new StringBuilder();

            String[] words = username.split("\\s+");

            for (String word : words) {
                if (!word.isEmpty()) {
                    initials.append(Character.toUpperCase(word.charAt(0)));
                }
            }

            return initials.toString();
        }
    }
}
