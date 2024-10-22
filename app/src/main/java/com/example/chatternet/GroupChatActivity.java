package com.example.chatternet;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.chatternet.utils.AndroidUtil;
import com.example.chatternet.utils.FirebaseUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class GroupChatActivity extends AppCompatActivity {

    EditText messageInput;
    TextView groupName;
    ImageButton sendMessageBtn;
    ImageButton backBtn;

    private DocumentReference userRef, groupNameRef, groupMessageKeyRef;
    private FirebaseAuth auth;
    private String ccurrentGroupName, currentUserID, currentUserName, currentDate, currentTime;
    private LinearLayout messageContainer;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        ccurrentGroupName = getIntent().getStringExtra("groupName");

        auth = FirebaseAuth.getInstance();
        currentUserID = auth.getCurrentUser().getUid();
        userRef = FirebaseUtil.currrentUserDetails();
        groupNameRef = FirebaseUtil.getGroupChatroomReference(ccurrentGroupName);

        groupName = findViewById(R.id.group_username);
        groupName.setText(ccurrentGroupName);

        initializeFields();

        getUserInfo();
        sendMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessageInfoToDatabase();
            }

        });

        backBtn.setOnClickListener((v) -> {
            onBackPressed();
        });

        findViewById(R.id.delete_btn).setOnClickListener(v -> deleteLastMessage());
    }

    private void sendMessageInfoToDatabase() {
        String message = messageInput.getText().toString();
        String messageKey = groupNameRef.collection("messages").document().getId();
        if (message.isEmpty()) {
            AndroidUtil.showToast(GroupChatActivity.this, "Please write a message first...");
        } else {
            Calendar ccalForDate = Calendar.getInstance();
            SimpleDateFormat currentDateFormat = new SimpleDateFormat("MMM dd, yyyy");
            String currentDate = currentDateFormat.format(ccalForDate.getTime());

            Calendar ccalForTime = Calendar.getInstance();
            SimpleDateFormat currentTimeFormat = new SimpleDateFormat("HH:mm");
            String currentTime = currentTimeFormat.format(ccalForTime.getTime());

            HashMap<String, Object> messageInfoMap = new HashMap<>();
            messageInfoMap.put("name", currentUserName);
            messageInfoMap.put("message", message);
            messageInfoMap.put("date", currentDate);
            messageInfoMap.put("time", currentTime);

            groupNameRef.collection("messages").document(messageKey).set(messageInfoMap).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    messageInput.setText("");
                } else {
                    AndroidUtil.showToast(GroupChatActivity.this, "Error sending message: " + task.getException().getMessage());
                }
            });
        }
    }

    private void getUserInfo() {
        userRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                if (e != null) {
                    return;
                }

                if (documentSnapshot.exists()) {
                    currentUserName = documentSnapshot.getString("username");
                }
            }
        });
    }

    protected void onStart() {
        super.onStart();
        groupNameRef.collection("messages").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    return;
                }
                for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                    switch (dc.getType()) {
                        case ADDED:
                            DocumentSnapshot documentAdded = dc.getDocument();
                            if (documentAdded.exists()) {
                                DisplayMessages(documentAdded);
                            }
                            break;
                        case REMOVED:
                            DocumentSnapshot documentRemoved = dc.getDocument();
                            if (documentRemoved.exists()) {
                                removeMessageFromUI(documentRemoved.getId());
                            }
                            break;
                    }
                }
            }
        });
    }

    private void DisplayMessages(DocumentSnapshot document) {
        String chatName = document.getString("name");
        String chatMessage = document.getString("message");
        String chatDate = document.getString("date");
        String chatTime = document.getString("time");
        String messageId = document.getId();

        EditText messageEditText = new EditText(this);
        messageEditText.setText(chatName + ":\n" + chatMessage + " \n" + chatTime + "     " + chatDate);
        messageEditText.setTextSize(16);
        messageEditText.setTextColor(getResources().getColor(R.color.black));
        messageEditText.setFocusable(false);
        messageEditText.setBackground(null);
        messageEditText.setTag(messageId);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 16);
        messageEditText.setLayoutParams(layoutParams);

        messageContainer.addView(messageEditText);

        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void initializeFields() {
        groupName = findViewById(R.id.group_username);
        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        messageContainer = findViewById(R.id.message_container);
        scrollView = findViewById(R.id.scroll_view);
    }

    private void deleteLastMessage() {
        groupNameRef.collection("messages").orderBy("time", Query.Direction.DESCENDING).limit(1).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                DocumentSnapshot lastDoc = task.getResult().getDocuments().get(0);
                lastDoc.getReference().delete().addOnCompleteListener(deleteTask -> {
                    if (deleteTask.isSuccessful()) {
                        removeMessageFromUI(lastDoc.getId());
                    } else {
                        Log.e("DeleteMessages", "Error al eliminar el último mensaje", deleteTask.getException());
                    }
                });
            } else {
                Log.e("DeleteMessages", "Error al obtener el último mensaje para eliminar", task.getException());
            }
        });
    }

    private void removeMessageFromUI(String messageId) {
        int childCount = messageContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childView = messageContainer.getChildAt(i);
            if (childView.getTag() != null && childView.getTag().equals(messageId)) {
                messageContainer.removeViewAt(i);
                break;
            }
        }
    }
}