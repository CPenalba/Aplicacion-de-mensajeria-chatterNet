package com.example.chatternet.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatternet.ChatActivity;
import com.example.chatternet.R;
import com.example.chatternet.model.ChatroomModel;
import com.example.chatternet.model.UserModel;
import com.example.chatternet.utils.AndroidUtil;
import com.example.chatternet.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class RecentChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatroomModel, RecentChatRecyclerAdapter.ChatroomViewHolder> {

    Context context;

    public RecentChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatroomModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatroomViewHolder holder, int position, @NonNull ChatroomModel model) {
        FirebaseUtil.getOtherUserFromChatroom(model.getUserIds()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                UserModel otherUserModel = task.getResult().toObject(UserModel.class);
                if (otherUserModel != null) {
                    boolean lastMessageSentByMe = model.getLastMessageSenderId().equals(FirebaseUtil.currentUserId());

                    FirebaseUtil.getOtherProfilePicStorageRef(otherUserModel.getUserId()).getDownloadUrl().addOnCompleteListener(t -> {
                        if (t.isSuccessful() && t.getResult() != null) {
                            Uri uri = t.getResult();
                            AndroidUtil.setProfilePic(context, uri, holder.profilePic);
                        } else {
                            Log.e("RecentChatRecyclerAdapter", "Error al obtener la URL de la imagen de perfil", t.getException());
                        }
                    });

                    holder.usernameText.setText(otherUserModel.getUsername());
                    if (lastMessageSentByMe)
                        holder.lastMessageText.setText("You: " + model.getLastMessage());
                    else holder.lastMessageText.setText(model.getLastMessage());
                    holder.lastMessageTime.setText(FirebaseUtil.timestampToString(model.getLastMessageTimestamp()));

                    holder.itemView.setOnClickListener(v -> {
                        Intent intent = new Intent(context, ChatActivity.class);
                        AndroidUtil.passUserModelAsIntent(intent, otherUserModel);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    });
                } else {
                    Log.e("RecentChatRecyclerAdapter", "UserModel es nulo para el chatroom: " + model.getUserIds());
                }
            } else {
                Log.e("RecentChatRecyclerAdapter", "Error al obtener UserModel", task.getException());
            }
        });
    }

    @NonNull
    @Override
    public ChatroomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_chat_recycler_row, parent, false);
        return new ChatroomViewHolder(view);
    }

    class ChatroomViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        TextView lastMessageText;
        TextView lastMessageTime;
        ImageView profilePic;

        public ChatroomViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            lastMessageText = itemView.findViewById(R.id.last_message_text);
            lastMessageTime = itemView.findViewById(R.id.last_message_time_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
        }
    }
}
