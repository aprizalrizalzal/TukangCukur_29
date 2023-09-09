package com.bro.barbershop.adapter.transaction;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bro.barbershop.R;
import com.bro.barbershop.databinding.ListTransactionBinding;
import com.bro.barbershop.model.customer.Customer;
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

public class ListTransactionAdapter extends RecyclerView.Adapter<ListTransactionAdapter.ViewHolder> {

    private static final List<User> user = new ArrayList<>();
    private static final List<Customer> customer = new ArrayList<>();
    private static final List<Shaving> shaving = new ArrayList<>();
    private final List<Transaction> transaction = new ArrayList<>();

    private ListTransactionAdapter.OnItemClickCallbackEdit onItemClickCallbackEdit;
    private ListTransactionAdapter.OnItemClickCallbackDelete onItemClickCallbackDelete;
    private boolean activate = true;

    @SuppressLint("NotifyDataSetChanged")
    public void setListUser(List<User> user) {
        ListTransactionAdapter.user.clear();
        ListTransactionAdapter.user.addAll(user);
        notifyDataSetChanged();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void setListCustomer(List<Customer> customer) {
        ListTransactionAdapter.customer.clear();
        ListTransactionAdapter.customer.addAll(customer);
        notifyDataSetChanged();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void setListShaving(List<Shaving> shaving) {
        ListTransactionAdapter.shaving.clear();
        ListTransactionAdapter.shaving.addAll(shaving);
        notifyDataSetChanged();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void setListTransaction(List<Transaction> transaction) {
        this.transaction.clear();
        this.transaction.addAll(transaction);
        notifyDataSetChanged();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void setActivateButtons(boolean activate) {
        this.activate = activate;
        notifyDataSetChanged();
    }

    public void setOnItemClickCallbackEdit(ListTransactionAdapter.OnItemClickCallbackEdit onItemClickCallbackEdit) {
        this.onItemClickCallbackEdit = onItemClickCallbackEdit;
    }
    public void setOnItemClickCallbackDelete(ListTransactionAdapter.OnItemClickCallbackDelete onItemClickCallbackDelete) {
        this.onItemClickCallbackDelete = onItemClickCallbackDelete;
    }

    @NonNull
    @Override
    public ListTransactionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_transaction, parent, false);
        return new ListTransactionAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListTransactionAdapter.ViewHolder holder, int position) {
        holder.bind(transaction.get(position));
        if (activate) {
            holder.itemView.setOnClickListener(v -> onItemClickCallbackEdit.onItemClickedEdit(transaction.get(holder.getAdapterPosition())));
            holder.binding.imgBtnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setOnClickListener(v -> Toast.makeText(v.getContext(), R.string.only_admin_can_change_transaction, Toast.LENGTH_SHORT).show());
            holder.binding.imgBtnDelete.setVisibility(View.INVISIBLE);
        }
        holder.binding.imgBtnDelete.setOnClickListener(v -> onItemClickCallbackDelete.onItemClickedDelete(transaction.get(holder.getAdapterPosition())));
    }

    @Override
    public int getItemCount() {
        return transaction.size();
    }

    public interface OnItemClickCallbackEdit {
        void onItemClickedEdit(Transaction transaction);
    }
    public interface OnItemClickCallbackDelete {
        void onItemClickedDelete(Transaction transaction);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ListTransactionBinding binding;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ListTransactionBinding.bind(itemView);

        }
        public void bind(Transaction transaction) {
            String shavingId = transaction.getShavingId();
            Shaving shaving = findShavingById(shavingId);

            if (shaving !=null){
                binding.tvHairstyle.setText(shaving.getHairstyle());
                binding.tvPrice.setText(MoneyTextWatcher.formatCurrency(shaving.getPrice()));
            }

            if (!transaction.getPaymentStatus()){
                binding.tvPaymentStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.red));
                binding.tvPaymentStatus.setText(R.string.not_yet_paid);
            } else {
                binding.tvPaymentStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.dark_blue));
                binding.tvPaymentStatus.setText(R.string.paid_off);
            }

            binding.tvTransactionDate.setText(transaction.getTransactionDate());

            String customerId = null;
            if (shaving != null) {
                customerId = shaving.getCustomerId();
            }

            Customer customer = findCustomerById(customerId);
            if (customer !=null){
                String _customer = customer.getCustomer();
                String initials = getInitials(customer.getCustomer());

                TextDrawable drawable = TextDrawable.builder()
                        .beginConfig()
                        .textColor(Color.WHITE)
                        .toUpperCase()
                        .endConfig()
                        .buildRoundRect(initials, ColorGenerator.MATERIAL.getColor(_customer), 16);

                Glide.with(itemView)
                        .load(_customer)
                        .placeholder(drawable)
                        .apply(new RequestOptions().override(128,128))
                        .into(binding.imgListTransaction);

                binding.tvCustomer.setText(_customer);
            }

            String userId = transaction.getUserId();
            User user = findUserById(userId);
            if (user !=null){
                binding.tvUsername.setText(user.getUsername());
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

        private String getInitials(String _customer) {
            StringBuilder initials = new StringBuilder();

            String[] words = _customer.split("\\s+");

            for (String word : words) {
                if (!word.isEmpty()) {
                    initials.append(Character.toUpperCase(word.charAt(0)));
                }
            }

            return initials.toString();
        }
    }
}
