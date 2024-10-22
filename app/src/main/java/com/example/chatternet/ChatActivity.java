package com.example.chatternet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.chatternet.adapter.ChatRecyclerAdapter;
import com.example.chatternet.model.ChatMessageModel;
import com.example.chatternet.model.ChatroomModel;
import com.example.chatternet.model.UserModel;
import com.example.chatternet.utils.AndroidUtil;
import com.example.chatternet.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_CAMERA_CAPTURE = 2;
    private static final int REQUEST_PERMISSIONS = 3;

    UserModel otherUser;
    String chatroomId;
    ChatroomModel chatroomModel;
    ChatRecyclerAdapter adapter;
    EditText messageInput;
    ImageButton sendMessageBtn;
    ImageButton deleteBtn;
    ImageButton backBtn;
    TextView otherUsername;
    RecyclerView recyclerView;
    ImageView imageView;
    ImageButton camBtn;
    StorageReference storageReference;
    FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());
        chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());

        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        otherUsername = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);
        imageView = findViewById(R.id.profile_pic_image_view);
        camBtn = findViewById(R.id.cam_btn);
        deleteBtn = findViewById(R.id.delete_btn);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        FirebaseUtil.getOtherProfilePicStorageRef(otherUser.getUserId()).getDownloadUrl().addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                Uri uri = t.getResult();
                AndroidUtil.setProfilePic(this, uri, imageView);
            }
        });

        backBtn.setOnClickListener((v) -> {
            onBackPressed();
        });

        deleteBtn.setOnClickListener(v -> {
            showDeleteOptionsDialog();
        });

        otherUsername.setText(otherUser.getUsername());

        sendMessageBtn.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (message.isEmpty()) return;
            sendMessageToUser(message);
        });

        camBtn.setOnClickListener(v -> {
            if (checkAndRequestPermissions()) {
                showImagePickerOptions();
            }
        });

        getOrCreateChatroomModel();
        setupChatRecyclerView();
    }

    private boolean checkAndRequestPermissions() {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
            return false;
        }

        return true;
    }

    private void addGroupChats() {

    }

    private void showImagePickerOptions() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(this).setTitle("Select Image").setItems(options, (dialog, which) -> {
            if (which == 0) {
                openCamera();
            } else if (which == 1) {
                openGallery();
            }
        }).show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA_CAPTURE);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showImagePickerOptions();
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK && data != null && data.getData() != null) {
                Uri selectedImageUri = data.getData();
                sendPicture(selectedImageUri);
            } else if (requestCode == REQUEST_CAMERA_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                Uri imageUri = getImageUriFromBitmap(imageBitmap);
                sendPicture(imageUri);
            }
        }
    }

    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    private void sendPicture(Uri imageUri) {
        if (imageUri != null) {
            StorageReference photoRef = storageReference.child("imagenes_chat/" + UUID.randomUUID().toString());
            photoRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    ChatMessageModel chatMessageModel = new ChatMessageModel(null, FirebaseUtil.currentUserId(), Timestamp.now(), downloadUrl);

                    FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessageModel).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUtil.getChatroomReference(chatroomId).update("lastMessage", "Photo").addOnCompleteListener(updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    sendNotification("Photo");
                                } else {
                                    Log.e("UpdateLastMessage", "Error al actualizar lastMessage", updateTask.getException());
                                }
                            });
                        } else {
                            Log.e("SendPicture", "Error al enviar la imagen", task.getException());
                        }
                    });
                }).addOnFailureListener(e -> {
                    Log.e("SendPicture", "Error al obtener la URL de descarga", e);
                });
            }).addOnFailureListener(e -> {
                Log.e("SendPicture", "Error al subir la imagen", e);
            });
        }
    }

    private void showDeleteOptionsDialog() {
        new AlertDialog.Builder(this).setTitle("Delete Messages").setMessage("Please select an option:").setPositiveButton("Clear Chat", (dialog, which) -> clearChatMessages()).setNegativeButton("Delete Last Message", (dialog, which) -> deleteLastMessage()).setNeutralButton("Cancel", null).show();
    }

    private void clearChatMessages() {
        FirebaseUtil.getChatroomMessageReference(chatroomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                adapter.stopListening();
                WriteBatch batch = FirebaseFirestore.getInstance().batch();

                List<Task<Void>> deleteTasks = new ArrayList<>();

                for (DocumentSnapshot doc : task.getResult()) {
                    ChatMessageModel message = doc.toObject(ChatMessageModel.class);
                    if (message.getImageUrl() != null) {
                        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(message.getImageUrl());
                        Task<Void> deleteTask = storageRef.delete().addOnSuccessListener(aVoid -> {
                            batch.delete(doc.getReference());
                        }).addOnFailureListener(e -> {
                            Log.e("ClearChatMessages", "Error al borrar imagen del Storage", e);
                        });

                        deleteTasks.add(deleteTask);
                    } else {
                        batch.delete(doc.getReference());
                    }
                }

                Tasks.whenAll(deleteTasks).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        batch.commit().addOnCompleteListener(batchTask -> {
                            if (batchTask.isSuccessful()) {
                                FirebaseUtil.getChatroomReference(chatroomId).update("lastMessage", "").addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        adapter.notifyDataSetChanged();
                                        adapter.startListening();
                                    } else {
                                        Log.e("ClearChatMessages", "Error al actualizar el chatroom", updateTask.getException());
                                    }
                                });
                            } else {
                                Log.e("ClearChatMessages", "Error al ejecutar el batch", batchTask.getException());
                            }
                        });
                    } else {
                        Log.e("ClearChatMessages", "Error al eliminar imágenes del Storage", task1.getException());
                    }
                });
            } else {
                Log.e("ClearChatMessages", "Error al obtener mensajes para eliminar", task.getException());
            }
        });
    }

    private void deleteLastMessage() {
        FirebaseUtil.getChatroomMessageReference(chatroomId).orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                DocumentSnapshot lastDoc = task.getResult().getDocuments().get(0);
                ChatMessageModel lastMessage = lastDoc.toObject(ChatMessageModel.class);

                adapter.stopListening();

                if (lastMessage.getImageUrl() != null) {
                    StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(lastMessage.getImageUrl());
                    storageRef.delete().addOnSuccessListener(aVoid -> {
                        lastDoc.getReference().delete().addOnCompleteListener(deleteTask -> {
                            if (deleteTask.isSuccessful()) {
                                adapter.notifyDataSetChanged();
                                FirebaseUtil.getChatroomReference(chatroomId).update("lastMessage", "Mensaje eliminado");
                                adapter.startListening();
                            } else {
                                Log.e("DeleteMessages", "Error al eliminar el último mensaje", deleteTask.getException());
                            }
                        });
                    }).addOnFailureListener(e -> {
                        Log.e("DeleteLastMessage", "Error al borrar imagen del Storage", e);
                    });
                } else {
                    lastDoc.getReference().delete().addOnCompleteListener(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            adapter.notifyDataSetChanged();
                            FirebaseUtil.getChatroomReference(chatroomId).update("lastMessage", "Mensaje eliminado");
                            adapter.startListening();
                        } else {
                            Log.e("DeleteMessages", "Error al eliminar el último mensaje", deleteTask.getException());
                        }
                    });
                }
            } else {
                Log.e("DeleteMessages", "Error al obtener el último mensaje para eliminar", task.getException());
            }
        });
    }

    void setupChatRecyclerView() {
        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId).orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>().setQuery(query, ChatMessageModel.class).build();

        adapter = new ChatRecyclerAdapter(options, getApplicationContext());
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    void sendMessageToUser(String message) {
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessage(message);
        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);

        ChatMessageModel chatMessageModel = new ChatMessageModel(message, FirebaseUtil.currentUserId(), Timestamp.now());
        FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessageModel).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if (task.isSuccessful()) {
                    messageInput.setText("");
                    sendNotification(message);
                }
            }
        });
    }

    void getOrCreateChatroomModel() {
        FirebaseUtil.getChatroomReference(chatroomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                chatroomModel = task.getResult().toObject(ChatroomModel.class);
                if (chatroomModel == null) {
                    chatroomModel = new ChatroomModel(chatroomId, Arrays.asList(FirebaseUtil.currentUserId(), otherUser.getUserId()), Timestamp.now(), "");
                    FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
                }
            }
        });
    }

    void sendNotification(String message) {
        FirebaseUtil.currrentUserDetails().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                UserModel currentUser = task.getResult().toObject(UserModel.class);
                try {
                    JSONObject jsonObject = new JSONObject();

                    JSONObject notificationObj = new JSONObject();
                    notificationObj.put("title", currentUser.getUsername());
                    notificationObj.put("body", message);

                    JSONObject dataObj = new JSONObject();
                    dataObj.put("userId", currentUser.getUserId());

                    jsonObject.put("notification", notificationObj);
                    jsonObject.put("data", dataObj);
                    jsonObject.put("to", otherUser.getFcmToken());

                    callApi(jsonObject);

                } catch (Exception e) {
                }
            }
        });
    }

    void callApi(JSONObject jsonObject) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";
        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder().url(url).post(body).header("Authorization", "Bearer AAAAZaqjcLI:APA91bFTsEKIeq_t-Jzhm65JHoABU9LJ26oBSu2JJd4JhO07H8d8XlrHoE1tUynn-ILdyaRt1frshKKtwmuMmL_YWDLkjuZeqL8j75CtXekoAT0Jsv0TSF_pODi6zcsthvUVvZrbLFUy").build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            }
        });
    }
}