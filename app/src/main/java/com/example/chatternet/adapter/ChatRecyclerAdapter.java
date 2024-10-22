package com.example.chatternet.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.chatternet.R;
import com.example.chatternet.model.ChatMessageModel;
import com.example.chatternet.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class ChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModelViewHolder> {

    Context context;

    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options, Context context) {
        super(options);
        this.context = context;
    }

    public void clear() {
        stopListening();
        notifyItemRangeRemoved(0, getItemCount());
        getSnapshots().clear();
        notifyDataSetChanged();
        startListening();
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatModelViewHolder holder, int position, @NonNull ChatMessageModel model) {
        if (model.getSenderId().equals(FirebaseUtil.currentUserId())) {
            holder.leftChatLayout.setVisibility(View.GONE);
            holder.rightChatLayout.setVisibility(View.VISIBLE);

            if (model.getImageUrl() != null) {
                holder.rightChatTextView.setVisibility(View.GONE);
                holder.rightChatImageView.setVisibility(View.VISIBLE);
                Glide.with(context).load(model.getImageUrl()).into(holder.rightChatImageView);
                holder.rightChatLayout.setBackground(null);
            } else {
                holder.rightChatTextView.setVisibility(View.VISIBLE);
                holder.rightChatImageView.setVisibility(View.GONE);
                holder.rightChatTextView.setText(model.getMessage());
                holder.rightChatLayout.setBackgroundResource(R.drawable.edit_text_rounded_corner);
            }
        } else {
            holder.rightChatLayout.setVisibility(View.GONE);
            holder.leftChatLayout.setVisibility(View.VISIBLE);

            if (model.getImageUrl() != null) {
                holder.leftChatTextView.setVisibility(View.GONE);
                holder.leftChatImageView.setVisibility(View.VISIBLE);
                Glide.with(context).load(model.getImageUrl()).into(holder.leftChatImageView);
                holder.leftChatLayout.setBackground(null);
            } else {
                holder.leftChatTextView.setVisibility(View.VISIBLE);
                holder.leftChatImageView.setVisibility(View.GONE);
                holder.leftChatTextView.setText(model.getMessage());
                holder.leftChatLayout.setBackgroundResource(R.drawable.edit_text_rounded_corner);
            }
        }
    }

    @NonNull
    @Override
    public ChatModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_message_recycler_row, parent, false);
        return new ChatModelViewHolder(view);
    }

    class ChatModelViewHolder extends RecyclerView.ViewHolder {

        LinearLayout leftChatLayout, rightChatLayout;
        TextView leftChatTextView, rightChatTextView;
        ImageView leftChatImageView, rightChatImageView;

        public ChatModelViewHolder(@NonNull View itemView) {
            super(itemView);

            leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
            leftChatTextView = itemView.findViewById(R.id.left_chat_textView);
            rightChatTextView = itemView.findViewById(R.id.right_chat_textView);
            leftChatImageView = itemView.findViewById(R.id.left_chat_imageView);
            rightChatImageView = itemView.findViewById(R.id.right_chat_imageView);
        }
    }
}
