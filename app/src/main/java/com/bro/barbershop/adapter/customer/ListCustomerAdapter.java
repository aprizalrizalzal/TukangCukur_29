package com.bro.barbershop.adapter.customer;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bro.barbershop.R;
import com.bro.barbershop.databinding.ListCustomerBinding;
import com.bro.barbershop.model.customer.Customer;
import com.bro.barbershop.utils.textDrawable.ColorGenerator;
import com.bro.barbershop.utils.textDrawable.TextDrawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class ListCustomerAdapter extends RecyclerView.Adapter<ListCustomerAdapter.ViewHolder> {

    private final List<Customer> customer = new ArrayList<>();
    private ListCustomerAdapter.OnItemClickCallbackOpenInNew onItemClickCallbackOpenInNew;
    private ListCustomerAdapter.OnItemClickCallbackEdit onItemClickCallbackEdit;
    private ListCustomerAdapter.OnItemClickCallbackDelete onItemClickCallbackDelete;
    private boolean activate = true;

    @SuppressLint("NotifyDataSetChanged")
    public void setActivateButtons(boolean activate) {
        this.activate = activate;
        notifyDataSetChanged();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void setListCustomer(List<Customer> customer) {
        this.customer.clear();
        this.customer.addAll(customer);
        notifyDataSetChanged();
    }
    public void setOnItemClickCallbackOpenInNew(ListCustomerAdapter.OnItemClickCallbackOpenInNew onItemClickCallbackOpenInNew) {
        this.onItemClickCallbackOpenInNew = onItemClickCallbackOpenInNew;
    }
    public void setOnItemClickCallbackEdit(ListCustomerAdapter.OnItemClickCallbackEdit onItemClickCallbackEdit) {
        this.onItemClickCallbackEdit = onItemClickCallbackEdit;
    }
    public void setOnItemClickCallbackDelete(ListCustomerAdapter.OnItemClickCallbackDelete onItemClickCallbackDelete) {
        this.onItemClickCallbackDelete = onItemClickCallbackDelete;
    }

    @NonNull
    @Override
    public ListCustomerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_customer, parent, false);
        return new ListCustomerAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListCustomerAdapter.ViewHolder holder, int position) {
        holder.bind(customer.get(position));
        holder.itemView.setOnClickListener(v -> onItemClickCallbackOpenInNew.onItemClickedOpenInNew(customer.get(holder.getAdapterPosition())));
        if (activate) {
            holder.binding.imgBtnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.binding.imgBtnDelete.setVisibility(View.INVISIBLE);
        }
        holder.binding.imgBtnEdit.setOnClickListener(v -> onItemClickCallbackEdit.onItemClickedEdit(customer.get(holder.getAdapterPosition())));
        holder.binding.imgBtnDelete.setOnClickListener(v -> onItemClickCallbackDelete.onItemClickedDelete(customer.get(holder.getAdapterPosition())));
    }

    @Override
    public int getItemCount() {
        return customer.size();
    }

    public interface OnItemClickCallbackOpenInNew {
        void onItemClickedOpenInNew(Customer customer);
    }
    public interface OnItemClickCallbackEdit {
        void onItemClickedEdit(Customer customer);
    }
    public interface OnItemClickCallbackDelete {
        void onItemClickedDelete(Customer customer);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ListCustomerBinding binding;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ListCustomerBinding.bind(itemView);
        }

        public void bind(Customer customer) {
            String customerName = customer.getCustomer();
            String initials = getInitials(customer.getCustomer());

            TextDrawable drawable = TextDrawable.builder()
                    .beginConfig()
                    .textColor(Color.WHITE)
                    .toUpperCase()
                    .endConfig()
                    .buildRoundRect(initials, ColorGenerator.MATERIAL.getColor(customerName), 16);

            Glide.with(itemView)
                    .load(customerName)
                    .placeholder(drawable)
                    .apply(new RequestOptions().override(128,128))
                    .into(binding.imgListCustomer);

            binding.tvCustomer.setText(customer.getCustomer());
            binding.tvPhoneNumber.setText(customer.getPhoneNumber());
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
    }
}
