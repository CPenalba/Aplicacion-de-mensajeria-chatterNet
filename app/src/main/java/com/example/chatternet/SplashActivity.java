package com.example.chatternet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.example.chatternet.model.UserModel;
import com.example.chatternet.utils.AndroidUtil;
import com.example.chatternet.utils.FirebaseUtil;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            String userId = extras.getString("userId");
            if (userId != null) {
                FirebaseUtil.allUserCollectionReference().document(userId).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        UserModel model = task.getResult().toObject(UserModel.class);
                        if (model != null) {
                            Intent mainIntent = new Intent(this, MainActivity.class);
                            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(mainIntent);

                            Intent intent = new Intent(this, ChatActivity.class);
                            AndroidUtil.passUserModelAsIntent(intent, model);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            handleError("User model is null");
                        }
                    } else {
                        handleError("Task unsuccessful or result is null");
                    }
                });
            } else {
                handleError("userId is null");
            }
        } else {
            new Handler().postDelayed(() -> {
                if (FirebaseUtil.isLoggedIn()) {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, LoginNumberActivity.class));
                }
                finish();
            }, 1000);
        }
    }

    private void handleError(String message) {
        Log.e("SplashActivity", message);
        new Handler().postDelayed(() -> {
            if (FirebaseUtil.isLoggedIn()) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginNumberActivity.class));
            }
            finish();
        }, 1000);
    }
}