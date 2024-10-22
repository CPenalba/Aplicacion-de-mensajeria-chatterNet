package com.example.chatternet;

import static com.example.chatternet.utils.FirebaseUtil.currentUserId;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.chatternet.utils.AndroidUtil;
import com.example.chatternet.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    ImageButton searchButton;
    ImageButton createGroupButton;
    ChatFragment chatFragment;
    ProfileFragment profileFragment;
    GroupFragment groupFragment;

    public static MainActivity getInstance() {
        return instance;
    }

    private static MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;

        chatFragment = new ChatFragment();
        profileFragment = new ProfileFragment();
        groupFragment = new GroupFragment();
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        searchButton = findViewById(R.id.main_search_btn);
        createGroupButton = findViewById(R.id.main_create_group_option);

        createGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RequestNewGroup();
            }
        });

        searchButton.setOnClickListener((v) -> {
            startActivity(new Intent(MainActivity.this, SearchUserActivity.class));
        });

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.menu_chat) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout, chatFragment).commit();
                }
                if (item.getItemId() == R.id.my_groups) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout, groupFragment).commit();
                }
                if (item.getItemId() == R.id.menu_profile) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout, profileFragment).commit();
                }
                return true;
            }
        });

        bottomNavigationView.setSelectedItemId(R.id.menu_chat);

        getFCMToken();
    }

    private void RequestNewGroup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog);
        builder.setTitle("Enter group name:");

        final EditText groupNameField = new EditText(MainActivity.this);
        groupNameField.setHint("Example: The family");
        builder.setView(groupNameField);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String groupName = groupNameField.getText().toString();
                if (TextUtils.isEmpty(groupName)) {
                    AndroidUtil.showToast(MainActivity.this, "Please write the group name.");
                } else {
                    createNewGroup(groupName);
                }
            }

            private void createNewGroup(String groupName) {
                String currentUserId = currentUserId();

                Map<String, Object> groupData = new HashMap<>();
                groupData.put("name", groupName);
                groupData.put("createdBy", currentUserId);
                groupData.put("timestamp", FieldValue.serverTimestamp());

                Map<String, Boolean> members = new HashMap<>();
                members.put(currentUserId, true);
                groupData.put("members", members);

                FirebaseUtil.groupRef().document(groupName).set(groupData).addOnSuccessListener(aVoid -> {
                    AndroidUtil.showToast(MainActivity.this, "Group created successfully!");
                }).addOnFailureListener(e -> {
                    AndroidUtil.showToast(MainActivity.this, "Failed to create group: " + e.getMessage());
                });
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void createAccessRequest(String groupId, String userId, Context context) {
        CollectionReference accessRequestsRef = FirebaseFirestore.getInstance().collection("accessRequests");

        Map<String, Object> accessRequestData = new HashMap<>();
        accessRequestData.put("groupId", groupId);
        accessRequestData.put("userId", userId);
        accessRequestData.put("status", "pendiente");

        accessRequestsRef.add(accessRequestData).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("WELCOME! :) Do you want to write your first message?");
                builder.setMessage("Press continue to write your firt message ");

                builder.setPositiveButton("CONTINUE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        addUserToGroup(groupId, userId, documentReference.getId());
                    }
                });
                builder.setNegativeButton("CANCEL", null);
                builder.show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                AndroidUtil.showToast(MainActivity.this, "Error al crear la solicitud de acceso: " + e.getMessage());
            }
        });
    }

    private void addUserToGroup(String groupId, String userId, String requestId) {
        DocumentReference groupRef = FirebaseFirestore.getInstance().collection("groups").document(groupId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("members." + userId, true);
        updates.put("status", "aprobada");

        groupRef.update(updates).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                AndroidUtil.showToast(MainActivity.this, "You can now access");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                AndroidUtil.showToast(MainActivity.this, "Error al conceder acceso al grupo: " + e.getMessage());
            }
        });

        FirebaseFirestore.getInstance().collection("accessRequests").document(requestId).delete();
    }

    void getFCMToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                FirebaseUtil.currrentUserDetails().update("fcmToken", token);
            }
        });
    }
}
