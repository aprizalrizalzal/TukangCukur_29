package com.bro.barbershop.ui.user.profile;

import static com.bro.barbershop.ui.user.UserFragment.EXTRA_USER;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.Navigation;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bro.barbershop.R;
import com.bro.barbershop.databinding.FragmentProfileBinding;
import com.bro.barbershop.model.user.User;
import com.bro.barbershop.model.role.Role;
import com.bro.barbershop.ui.auth.LoginActivity;
import com.bro.barbershop.utils.progressBar.CustomProgressDialog;
import com.bro.barbershop.utils.textDrawable.ColorGenerator;
import com.bro.barbershop.utils.textDrawable.TextDrawable;
import com.bumptech.glide.Glide;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private View view;
    private User extraUser;
    private ActivityResultLauncher<String> resultLauncher;
    private Uri imageUrl;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseStorage storage;
    private DatabaseReference databaseReferenceUser, databaseReferenceRole;
    private StorageReference storageReference;
    private String currentUserEmail, userId, username, phoneNumber, initials;
    private boolean emailVerified;
    private CustomProgressDialog progressDialog;

    public ProfileFragment() {
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
        binding = FragmentProfileBinding.inflate(getLayoutInflater(), container, false);
        view = binding.getRoot();

        progressDialog = new CustomProgressDialog(requireActivity());

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://barbershopbro-55cec-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReferenceUser = database.getReference("user");
        databaseReferenceRole = database.getReference("role");

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        if (currentUser != null) {
            currentUserEmail = currentUser.getEmail();
            emailVerified = currentUser.isEmailVerified();
        } else {
            requireActivity().finish();
        }

        if (getArguments() != null) {
            extraUser = getArguments().getParcelable(EXTRA_USER);
        }

        if (extraUser != null) {
            viewExtraUser(extraUser);
        } else {
            changeRealtimeDatabaseUser(currentUserEmail);
        }

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_out_nav, menu);
                if (extraUser != null) {
                    menu.findItem(R.id.action_update_password).setVisible(false);
                    menu.findItem(R.id.action_sign_out).setVisible(false);
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_update_password) {
                    Navigation.findNavController(view).navigate(R.id.action_nav_profile_to_nav_password);

                } else if (menuItem.getItemId() == R.id.action_sign_out) {
                    if (userId !=null){
                        updateTokenId(userId);
                    } else {
                        Toast.makeText(requireActivity(), R.string.failed, Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }

        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        resultLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
            imageUrl = result;
            if (result != null) {
                Glide.with(view).load(imageUrl)
                        .into(binding.imgUser);
                binding.fabAddImage.setVisibility(View.GONE);
                binding.fabUploadImage.setVisibility(View.VISIBLE);
            } else {
                binding.fabAddImage.setVisibility(View.VISIBLE);
                binding.fabUploadImage.setVisibility(View.GONE);
            }
        });

        return view;
    }

    private void viewExtraUser(User extraUser) {
        username = extraUser.getUsername();
        initials = getInitials(username);

        TextDrawable drawable = TextDrawable.builder()
                .beginConfig()
                .textColor(Color.WHITE)
                .toUpperCase()
                .endConfig()
                .buildRoundRect(initials, ColorGenerator.MATERIAL.getColor(username), 16);

        Glide.with(view).load(extraUser.getPhotoUrl())
                .placeholder(drawable)
                .into(binding.imgUser);
        binding.fabAddImage.setVisibility(View.GONE);
        binding.fabUploadImage.setVisibility(View.GONE);
        binding.tietUsername.setText(extraUser.getUsername());
        binding.tietUsername.setEnabled(false);
        binding.tietEmail.setText(extraUser.getEmail());
        binding.tietEmail.setEnabled(false);
        binding.tietPhoneNumber.setText(extraUser.getPhoneNumber());
        binding.tietPhoneNumber.setEnabled(false);
        changeRealtimeDatabaseRole(extraUser);
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

    private void changeRealtimeDatabaseUser(String currentUserEmail) {
        progressDialog.ShowProgressDialog();
        Query query = databaseReferenceUser.orderByChild("email").equalTo(currentUserEmail);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                        User user = dataSnapshot.getValue(User.class);
                        if (user != null) {
                            userId = user.getUserId();
                        }
                    }
                    databaseReferenceUser.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            progressDialog.DismissProgressDialog();
                            if (snapshot.exists()) {
                                User user = snapshot.getValue(User.class);
                                if (user != null) {
                                    username = user.getUsername();
                                    initials = getInitials(username);

                                    TextDrawable drawable = TextDrawable.builder()
                                            .beginConfig()
                                            .textColor(Color.WHITE)
                                            .toUpperCase()
                                            .endConfig()
                                            .buildRoundRect(initials, ColorGenerator.MATERIAL.getColor(username), 16);

                                    Glide.with(view).load(user.getPhotoUrl())
                                            .placeholder(drawable)
                                            .into(binding.imgUser);
                                    if (!user.getPhotoUrl().equals("")) {
                                        binding.imgUser.setOnLongClickListener(v -> {
                                            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                                            builder.setTitle(R.string.delete).setMessage(R.string.delete_image).setCancelable(false)
                                                    .setNegativeButton(R.string.no, (dialog, id) -> dialog.cancel())
                                                    .setPositiveButton(R.string.yes, (dialog, id) -> deleteImage(user));
                                            builder.show();
                                            return false;
                                        });
                                    }
                                    binding.tietUsername.setText(user.getUsername());
                                    binding.tietUsername.addTextChangedListener(new TextWatcher() {
                                        @Override
                                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                        }

                                        @Override
                                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                                        }

                                        @Override
                                        public void afterTextChanged(Editable s) {
                                            if (s.length() >= user.getUsername().length() || s.length() <= user.getUsername().length()) {
                                                binding.tilUsername.setEndIconDrawable(R.drawable.baseline_save);
                                                binding.tilUsername.setEndIconOnClickListener(v -> {
                                                    username = Objects.requireNonNull(binding.tietUsername.getText()).toString().trim();
                                                    updateUsername(user.getUserId());
                                                });
                                            }
                                        }
                                    });

                                    binding.tilEmail.setOnClickListener(v -> {
                                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                                        builder.setTitle(R.string.verification).setMessage(getString(R.string.send_verification, user.getEmail())).setCancelable(false)
                                                .setNegativeButton(R.string.no, (dialog, id) -> dialog.cancel())
                                                .setPositiveButton(R.string.send, (dialog, id) -> sendEmailVerification())
                                                .setNeutralButton(R.string.update_email, (dialog, id) -> updateEmail(user));
                                        builder.show();
                                    });

                                    binding.tietEmail.setText(user.getEmail());
                                    binding.tietEmail.setEnabled(false);

                                    if (!emailVerified && !currentUser.isEmailVerified()) {
                                        binding.tilEmail.setError(getString(R.string.email_not_verified));
                                    } else {
                                        binding.tilEmail.setErrorEnabled(false);
                                    }

                                    binding.tietPhoneNumber.setText(user.getPhoneNumber());
                                    binding.tietPhoneNumber.addTextChangedListener(new TextWatcher() {
                                        @Override
                                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                        }

                                        @Override
                                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                                        }

                                        @Override
                                        public void afterTextChanged(Editable s) {
                                            if (s.length() >= user.getUsername().length() || s.length() <= user.getUsername().length()) {
                                                binding.tilPhoneNumber.setEndIconDrawable(R.drawable.baseline_save);
                                                binding.tilPhoneNumber.setEndIconOnClickListener(v -> {
                                                    phoneNumber = Objects.requireNonNull(binding.tietPhoneNumber.getText()).toString().trim();
                                                    updatePhoneNumber(user.getUserId());
                                                });
                                            }
                                        }
                                    });

                                    changeRealtimeDatabaseRole(user);

                                    binding.fabAddImage.setOnClickListener(v -> selectImage());
                                    binding.fabUploadImage.setOnClickListener(v -> uploadImage(user.getUserId()));
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
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.DismissProgressDialog();
            }
        });
    }

    private void changeRealtimeDatabaseRole(User user) {
        progressDialog.ShowProgressDialog();
        String roleId = user.getRoleId();
        databaseReferenceRole.child(roleId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.DismissProgressDialog();
                if (snapshot.exists()){
                    Role role = snapshot.getValue(Role.class);
                    if (role !=null){
                        binding.tietRole.setText(role.getRole());
                        binding.tilRole.setEnabled(false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.DismissProgressDialog();

            }
        });
    }

    private void selectImage() {
        resultLauncher.launch("image/*");
    }
    private void uploadImage(String userId) {
        progressDialog.ShowProgressDialog();
        String pathImage = "user/" + userId + ".jpg";
        storageReference.child(pathImage).putFile(imageUrl).addOnSuccessListener(taskSnapshot -> {
            storageReference.child(pathImage).getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUri = uri.toString();

                Map<String, Object> mapUsers = new HashMap<>();
                mapUsers.put("photoUrl", downloadUri);

                databaseReferenceUser.child(userId).updateChildren(mapUsers).addOnSuccessListener(command -> {
                    progressDialog.DismissProgressDialog();
                    changeRealtimeDatabaseUser(currentUserEmail);
                }).addOnFailureListener(e -> {
                    progressDialog.DismissProgressDialog();
                    Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
                });

            }).addOnFailureListener(e -> {
                progressDialog.DismissProgressDialog();
                Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
            });

            binding.fabAddImage.setVisibility(View.VISIBLE);
            binding.fabUploadImage.setVisibility(View.GONE);
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            binding.fabAddImage.setVisibility(View.GONE);
            binding.fabUploadImage.setVisibility(View.VISIBLE);
            Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void deleteImage(User user) {
        progressDialog.ShowProgressDialog();
        storageReference = storage.getReferenceFromUrl(user.getPhotoUrl());
        storageReference.delete().addOnSuccessListener(taskSnapshot -> {
            Map<String, Object> mapUsers = new HashMap<>();
            mapUsers.put("photoUrl", "");

            databaseReferenceUser.child(user.getUserId()).updateChildren(mapUsers).addOnSuccessListener(command -> {
                progressDialog.DismissProgressDialog();
                changeRealtimeDatabaseUser(currentUserEmail);
            }).addOnFailureListener(e -> {
                progressDialog.DismissProgressDialog();
                Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
            });

        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUsername(String userId) {
        progressDialog.ShowProgressDialog();

        Map<String, Object> mapUser = new HashMap<>();
        mapUser.put("username", username);

        databaseReferenceUser.child(userId).updateChildren(mapUser).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                progressDialog.DismissProgressDialog();
                binding.tilUsername.setEndIconVisible(false);
            } else {
                progressDialog.DismissProgressDialog();
                Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
            }

        });
    }
    private void updateEmail(User user) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.update_email).setMessage(getString(R.string.request_new_email, user.getEmail())).setCancelable(false)
                .setNegativeButton(R.string.no, (dialog, id) -> dialog.cancel())
                .setPositiveButton(R.string.yes, (dialog, id) -> Navigation.findNavController(view).navigate(R.id.action_nav_profile_to_nav_email));
        builder.show();
    }
    private void sendEmailVerification() {
        progressDialog.ShowProgressDialog();
        currentUser.sendEmailVerification().addOnSuccessListener(unused -> {
            progressDialog.DismissProgressDialog();
            mAuth.signOut();
            Toast.makeText(requireContext(), getString(R.string.email_verification_sent, currentUser.getEmail()), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void updatePhoneNumber(String userId) {
        progressDialog.ShowProgressDialog();

        Map<String, Object> mapUser = new HashMap<>();
        mapUser.put("phoneNumber", phoneNumber);

        databaseReferenceUser.child(userId).updateChildren(mapUser).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                progressDialog.DismissProgressDialog();
                binding.tilPhoneNumber.setEndIconVisible(false);
            } else {
                progressDialog.DismissProgressDialog();
                Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void updateTokenId(String userId) {
        progressDialog.ShowProgressDialog();
        Map<String, Object> mapUsers = new HashMap<>();
        mapUsers.put("tokenId", "");

        databaseReferenceUser.child(userId).updateChildren(mapUsers).addOnSuccessListener(unused -> {
            progressDialog.DismissProgressDialog();
            mAuth.signOut();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        }).addOnFailureListener(e -> progressDialog.DismissProgressDialog());
    }
}