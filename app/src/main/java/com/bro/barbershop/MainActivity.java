package com.bro.barbershop;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bro.barbershop.model.user.User;
import com.bro.barbershop.model.role.Role;
import com.bro.barbershop.utils.progressBar.CustomProgressDialog;
import com.bro.barbershop.utils.textDrawable.ColorGenerator;
import com.bro.barbershop.utils.textDrawable.TextDrawable;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.bro.barbershop.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration mAppBarConfiguration;
    private CustomProgressDialog progressDialog;
    private NavigationView navigationView;
    private FirebaseUser currentUser;
    private FirebaseMessaging messaging;
    private ImageView imgNavUser;
    private TextView username, email;

    private String tokenId;
    private DatabaseReference databaseReferenceUser, databaseReferenceRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.bro.barbershop.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog = new CustomProgressDialog(MainActivity.this);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        messaging = FirebaseMessaging.getInstance();

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://barbershopbro-55cec-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReferenceRole = database.getReference("role");
        databaseReferenceUser = database.getReference("user");

        if (currentUser !=null) {
            String currentUserEmail = currentUser.getEmail();
            changeRealtimeDatabaseUser(currentUserEmail);
        } else {
            Toast.makeText(MainActivity.this, R.string.please_login_first, Toast.LENGTH_SHORT).show();
        }

        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = binding.drawerLayout;
        navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_home) {
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            } else {
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
        });

        imgNavUser = navigationView.getHeaderView(0).findViewById(R.id.img_nav_user);
        username = navigationView.getHeaderView(0).findViewById(R.id.tv_nav_username);
        email = navigationView.getHeaderView(0).findViewById(R.id.tv_nav_email);
    }
    private void changeRealtimeDatabaseUser(String currentUserEmail) {
        progressDialog.ShowProgressDialog();

        Query query = databaseReferenceUser.orderByChild("email").equalTo(currentUserEmail);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.DismissProgressDialog();
                if (snapshot.exists()) {
                    String userId = "";
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user != null) {
                            userId = user.getUserId();
                        }
                    }
                    viewRealtimeDatabaseUser(userId);
                } else {
                    createRealtimeDatabaseUser(currentUserEmail);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.DismissProgressDialog();
            }
        });
    }

    private void createRealtimeDatabaseUser(String currentUserEmail) {
        progressDialog.ShowProgressDialog();

        String userId = UUID.randomUUID().toString();

        User user = new User(userId,currentUserEmail,"", "", "", "", "");
        databaseReferenceUser.child(userId).setValue(user).addOnSuccessListener(command -> {
            progressDialog.DismissProgressDialog();
            viewRealtimeDatabaseUser(userId);
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(MainActivity.this, R.string.add_user_failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void viewRealtimeDatabaseUser(String userId) {
        progressDialog.ShowProgressDialog();

        databaseReferenceUser.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.DismissProgressDialog();
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        String usernameDrawable = user.getUsername();
                        String initials = getInitials(usernameDrawable);

                        TextDrawable drawable = TextDrawable.builder()
                                .beginConfig()
                                .textColor(Color.WHITE)
                                .toUpperCase()
                                .endConfig()
                                .buildRoundRect(initials, ColorGenerator.MATERIAL.getColor(usernameDrawable), 16);
                        
                        Glide.with(getApplicationContext())
                                .load(user.getPhotoUrl())
                                .placeholder(drawable)
                                .into(imgNavUser);
                        username.setText(user.getUsername());
                        MainActivity.this.email.setText(user.getEmail());

                        if (user.getEmail().equals(getString(R.string.default_email))) {
                            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this);
                            builder.setTitle(getString(R.string.reminder))
                                    .setMessage(R.string.do_not_change_the_default_email)
                                    .setCancelable(false)
                                    .setNeutralButton(getString(R.string.yes), (dialog, id) -> dialog.cancel());
                            builder.show();
                        } else {
                            if (!currentUser.isEmailVerified()) {
                                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this);
                                builder.setTitle(getString(R.string.reminder))
                                        .setMessage(R.string.email_must_be_verified).setCancelable(false)
                                        .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.cancel())
                                        .setPositiveButton(getString(R.string.yes), (dialog, id) -> Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment_content_main).navigate(R.id.nav_profile));
                                builder.show();
                            }
                        }
                        refreshTokenIdUser(user);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.DismissProgressDialog();
            }
        });

    }

    private void updateRoleId(String roleId, User user) {
        progressDialog.ShowProgressDialog();

        Map<String, Object> mapUser = new HashMap<>();
        mapUser.put("roleId", roleId);

        String userId = user.getUserId();
        databaseReferenceUser.child(userId).updateChildren(mapUser).addOnSuccessListener(unused -> {
            progressDialog.DismissProgressDialog();
            viewRealtimeDatabaseUser(userId);
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(this, R.string.failed, Toast.LENGTH_SHORT).show();
        });
    }

    private String getInitials(String usernameDrawable) {
        StringBuilder initials = new StringBuilder();

        String[] words = usernameDrawable.split("\\s+");

        for (String word : words) {
            if (!word.isEmpty()) {
                initials.append(Character.toUpperCase(word.charAt(0)));
            }
        }

        return initials.toString();
    }

    private void changeUserRole(Role role) {
        Menu nav_menu = navigationView.getMenu();

        if (role.getRole().equals(getString(R.string.employee))){
            nav_menu.findItem(R.id.nav_user).setVisible(false);
        }
    }
    private void refreshTokenIdUser(User user) {
        progressDialog.ShowProgressDialog();
        messaging.getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                progressDialog.DismissProgressDialog();
                tokenId = task.getResult();
                updateTokenId(user, tokenId);
            } else {
                progressDialog.DismissProgressDialog();
            }
        });
    }

    private void updateTokenId(User user, String tokenId) {
        progressDialog.ShowProgressDialog();
        Map<String, Object> mapUsers = new HashMap<>();
        mapUsers.put("tokenId", tokenId);

        String userId = user.getUserId();
        databaseReferenceUser.child(userId).updateChildren(mapUsers).addOnSuccessListener(unused -> {
            progressDialog.DismissProgressDialog();
            changeRealtimeDatabaseRole(user);
        }).addOnFailureListener(e -> progressDialog.DismissProgressDialog());
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
                    if (role != null && !roleId.isEmpty()){
                        changeUserRole(role);
                    } else {
                        createRealtimeDatabaseRole(user);
                    }
                } else {
                    createRealtimeDatabaseRole(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.DismissProgressDialog();

            }
        });
    }

    private void createRealtimeDatabaseRole(User user) {
        progressDialog.ShowProgressDialog();

        String roleId = UUID.randomUUID().toString();
        Role role;
        if (user.getEmail().equals(getString(R.string.default_email))){
            role = new Role(roleId, getString(R.string.administrator));
        } else {
            role = new Role(roleId, getString(R.string.employee));
        }

        databaseReferenceRole.child(roleId).setValue(role).addOnSuccessListener(command -> {
            progressDialog.DismissProgressDialog();
            updateRoleId(roleId, user);
        }).addOnFailureListener(e -> {
            progressDialog.DismissProgressDialog();
            Toast.makeText(MainActivity.this, R.string.add_role_failed, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}