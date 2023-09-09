package com.bro.barbershop.adapter.shaving;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bro.barbershop.R;
import com.bro.barbershop.databinding.ListShavingBinding;
import com.bro.barbershop.model.shaving.Shaving;
import com.bro.barbershop.utils.textDrawable.ColorGenerator;
import com.bro.barbershop.utils.textWatcher.MoneyTextWatcher;
import com.bro.barbershop.utils.textDrawable.TextDrawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class ListShavingAdapter extends RecyclerView.Adapter<ListShavingAdapter.ViewHolder> {

    private final List<Shaving> shaving = new ArrayList<>();

    private ListShavingAdapter.OnItemClickCallbackSend onItemClickCallbackSend;
    private ListShavingAdapter.OnItemClickCallbackDelete onItemClickCallbackDelete;
    private boolean activate = true;

    @SuppressLint("NotifyDataSetChanged")
    public void setActivateButtons(boolean activate) {
        this.activate = activate;
        notifyDataSetChanged();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void setListShaving(List<Shaving> shaving) {
        this.shaving.clear();
        this.shaving.addAll(shaving);
        notifyDataSetChanged();
    }

    public void setOnItemClickCallbackSend(ListShavingAdapter.OnItemClickCallbackSend onItemClickCallbackSend) {
        this.onItemClickCallbackSend = onItemClickCallbackSend;
    }

    public void setOnItemClickCallbackDelete(ListShavingAdapter.OnItemClickCallbackDelete onItemClickCallbackDelete) {
        this.onItemClickCallbackDelete = onItemClickCallbackDelete;
    }

    @NonNull
    @Override
    public ListShavingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_shaving, parent, false);
        return new ListShavingAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListShavingAdapter.ViewHolder holder, int position) {
        holder.bind(shaving.get(position));
        holder.itemView.setOnClickListener(v -> onItemClickCallbackSend.onItemClickedSend(shaving.get(holder.getAdapterPosition())));
        if (activate) {
            holder.binding.imgBtnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.binding.imgBtnDelete.setVisibility(View.INVISIBLE);
        }
        holder.binding.imgBtnDelete.setOnClickListener(v -> onItemClickCallbackDelete.onItemClickedDelete(shaving.get(holder.getAdapterPosition())));
    }

    @Override
    public int getItemCount() {
        return shaving.size();
    }
    public interface OnItemClickCallbackSend {
        void onItemClickedSend(Shaving shaving);
    }
    public interface OnItemClickCallbackDelete {
        void onItemClickedDelete(Shaving shaving);
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ListShavingBinding binding;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ListShavingBinding.bind(itemView);
        }

        public void bind(Shaving shaving) {
            String shavingName = shaving.getHairstyle();
            String initials = getInitials(shaving.getHairstyle());

            TextDrawable drawable = TextDrawable.builder()
                    .beginConfig()
                    .textColor(Color.WHITE)
                    .toUpperCase()
                    .endConfig()
                    .buildRoundRect(initials, ColorGenerator.MATERIAL.getColor(shavingName), 16);

            Glide.with(itemView)
                    .load(shavingName)
                    .placeholder(drawable)
                    .apply(new RequestOptions().override(128,128))
                    .into(binding.imgListShaving);

            binding.tvShavingDate.setText(shaving.getShavingDate());
            binding.tvHairstyle.setText(shaving.getHairstyle());
            binding.tvPrice.setText(MoneyTextWatcher.formatCurrency(shaving.getPrice()));
        }

        private String getInitials(String shaving) {
            StringBuilder initials = new StringBuilder();

            String[] words = shaving.split("\\s+");

            for (String word : words) {
                if (!word.isEmpty()) {
                    initials.append(Character.toUpperCase(word.charAt(0)));
                }
            }

            return initials.toString();
        }
    }
}
