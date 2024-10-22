package com.example.chatternet;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FindFriendActivity extends AppCompatActivity {

    private ImageButton searchButton;
    private RecyclerView findFriendsRecycleView;
    private TextView findFriends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friends);

        findFriendsRecycleView=findViewById(R.id.find_friend_recycle_view);
        findFriendsRecycleView.setLayoutManager(new LinearLayoutManager(this));
        searchButton = findViewById(R.id.main_search_btn);

        findFriends = findViewById(R.id.fiend_friends);
        findFriends.setText("Find Friends");

        searchButton.setOnClickListener((v) -> {
            startActivity(new Intent(FindFriendActivity.this, SearchUserActivity.class));
        });
    }
}
