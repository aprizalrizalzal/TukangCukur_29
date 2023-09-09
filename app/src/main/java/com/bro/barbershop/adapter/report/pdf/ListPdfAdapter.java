package com.bro.barbershop.adapter.report.pdf;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bro.barbershop.R;
import com.bro.barbershop.databinding.ListPdfBinding;
import com.bro.barbershop.model.report.pdf.Pdf;
import com.bro.barbershop.model.user.User;
import com.bro.barbershop.utils.textDrawable.ColorGenerator;
import com.bro.barbershop.utils.textDrawable.TextDrawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class ListPdfAdapter extends RecyclerView.Adapter<ListPdfAdapter.ViewHolder> {
    private static final List<User> user = new ArrayList<>();
    private final List<Pdf> pdf = new ArrayList<>();

    private ListPdfAdapter.OnItemClickCallback onItemClickCallback;
    private ListPdfAdapter.OnItemClickCallbackDelete onItemClickCallbackDelete;
    private boolean activate = true;

    @SuppressLint("NotifyDataSetChanged")
    public void setListUser(List<User> user) {
        ListPdfAdapter.user.clear();
        ListPdfAdapter.user.addAll(user);
        notifyDataSetChanged();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void setListPdf(List<Pdf> pdf) {
        this.pdf.clear();
        this.pdf.addAll(pdf);
        notifyDataSetChanged();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void setActivateButtons(boolean activate) {
        this.activate = activate;
        notifyDataSetChanged();
    }

    public void setOnItemClickCallback(ListPdfAdapter.OnItemClickCallback onItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback;
    }
    public void setOnItemClickCallbackDelete(ListPdfAdapter.OnItemClickCallbackDelete onItemClickCallbackDelete) {
        this.onItemClickCallbackDelete = onItemClickCallbackDelete;
    }

    @NonNull
    @Override
    public ListPdfAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_pdf, parent, false);
        return new ListPdfAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListPdfAdapter.ViewHolder holder, int position) {
        holder.bind(pdf.get(position));
        holder.itemView.setOnClickListener(v -> onItemClickCallback.onItemClicked(pdf.get(holder.getAdapterPosition())));
        if (activate) {
            holder.binding.imgBtnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.binding.imgBtnDelete.setVisibility(View.INVISIBLE);
        }
        holder.binding.imgBtnDelete.setOnClickListener(v -> onItemClickCallbackDelete.onItemClickedDelete(pdf.get(holder.getAdapterPosition())));
    }

    @Override
    public int getItemCount() {
        return pdf.size();
    }

    public interface OnItemClickCallback {
        void onItemClicked(Pdf pdf);
    }
    public interface OnItemClickCallbackDelete {
        void onItemClickedDelete(Pdf pdf);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ListPdfBinding binding;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ListPdfBinding.bind(itemView);

        }
        public void bind(Pdf pdf) {

            String userId = pdf.getUserId();
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

            String url = pdf.getPdfUrl();

            try {
                URL fileUrl = new URL(URLDecoder.decode(url, "UTF-8"));
                String fileName = fileUrl.getPath().substring(fileUrl.getPath().lastIndexOf('/') + 1);
                binding.tvViewPdfReport.setText(fileName);
            } catch (UnsupportedEncodingException | MalformedURLException e) {
                throw new RuntimeException(e);
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
