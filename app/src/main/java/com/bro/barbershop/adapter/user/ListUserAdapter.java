package com.bro.barbershop.adapter.user;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bro.barbershop.R;
import com.bro.barbershop.databinding.ListUserBinding;
import com.bro.barbershop.model.user.User;
import com.bro.barbershop.model.role.Role;
import com.bro.barbershop.utils.textDrawable.ColorGenerator;
import com.bro.barbershop.utils.textDrawable.TextDrawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class ListUserAdapter extends RecyclerView.Adapter<ListUserAdapter.ViewHolder> {

    private final List<User> user = new ArrayList<>();
    private static final List<Role> role = new ArrayList<>();
    private OnItemClickCallback onItemClickCallback;
    private OnItemClickCallbackEditRole onItemClickCallbackEditRole;
    private OnItemClickCallbackDelete onItemClickCallbackDelete;
    private boolean activate = true;

    @SuppressLint("NotifyDataSetChanged")
    public void setActivateButtons(boolean activate) {
        this.activate = activate;
        notifyDataSetChanged();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void setListUser(List<User> user) {
        this.user.clear();
        this.user.addAll(user);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setListRole(List<Role> role) {
        ListUserAdapter.role.clear();
        ListUserAdapter.role.addAll(role);
        notifyDataSetChanged();
    }

    public void setOnItemClickCallback(OnItemClickCallback onItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback;
    }

    public void setOnItemClickCallbackEditRole(OnItemClickCallbackEditRole onItemClickCallbackEditRole) {
        this.onItemClickCallbackEditRole = onItemClickCallbackEditRole;
    }

    public void setOnItemClickCallbackDelete(OnItemClickCallbackDelete onItemClickCallbackDelete) {
        this.onItemClickCallbackDelete = onItemClickCallbackDelete;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(user.get(position));
        holder.itemView.setOnClickListener(v -> onItemClickCallback.onItemClicked(user.get(holder.getAdapterPosition())));
        if (activate) {
            holder.binding.imgBtnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.binding.imgBtnDelete.setVisibility(View.INVISIBLE);
        }
        holder.binding.imgBtnEditRole.setOnClickListener(v -> onItemClickCallbackEditRole.onItemClickedEditRole(user.get(holder.getAdapterPosition())));
        holder.binding.imgBtnDelete.setOnClickListener(v -> onItemClickCallbackDelete.onItemClickedDelete(user.get(holder.getAdapterPosition())));

    }

    @Override
    public int getItemCount() {
        return user.size();
    }

    public interface OnItemClickCallback {
        void onItemClicked(User user);
    }

    public interface OnItemClickCallbackEditRole {
        void onItemClickedEditRole(User user);
    }


    public interface OnItemClickCallbackDelete {
        void onItemClickedDelete(User user);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ListUserBinding binding;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ListUserBinding.bind(itemView);
        }

        public void bind(User user) {
            String username = user.getUsername();
            String initials = getInitials(user.getUsername());

            TextDrawable drawable = TextDrawable.builder()
                    .beginConfig()
                    .textColor(Color.WHITE)
                    .toUpperCase()
                    .endConfig()
                    .buildRoundRect(initials, ColorGenerator.MATERIAL.getColor(username), 16);

            Glide.with(itemView)
                    .load(user.getPhotoUrl())
                    .placeholder(drawable)
                    .apply(new RequestOptions().override(128,128))
                    .into(binding.imgListUser);

            binding.tvUsername.setText(username);

            String roleId = user.getRoleId();
            Role role = findRoleById(roleId);

            if (role != null) {
                String roleName = role.getRole();
                binding.tvRole.setText(roleName);
            } else {
                binding.tvRole.setText(R.string.unknown_role);
            }

            binding.tvEmail.setText(user.getEmail());
            binding.tvPhoneNumber.setText(user.getPhoneNumber());
        }

        private Role findRoleById(String roleId) {
            for (Role role : role) {
                if (role.getRoleId().equals(roleId)) {
                    return role;
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
