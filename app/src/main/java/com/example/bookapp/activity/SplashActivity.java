package com.example.bookapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bookapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashActivity extends AppCompatActivity {
    //firebase auth
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //init firebase auth
        firebaseAuth= FirebaseAuth.getInstance();

        //start main screen after 1s
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkUser();
            }
        }, 1000);
    }

    private void checkUser() {
        //get current user, if logged in
        FirebaseUser firebaseUser=firebaseAuth.getCurrentUser();
        if(firebaseUser== null){
            //user not logged in
            startActivity(new Intent(SplashActivity.this,MainActivity.class));
            finish();
        }else {
            //user logged in check user type, same as done in login screen
            DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseUser.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            //get user type
                            String userType=""+snapshot.child("userType").getValue();
                            //check user type
                            if (userType.equals("user")){
                                startActivity(new Intent(SplashActivity.this,DashboardUserActivity.class));
                                finish();

                            } else if (userType.equals("admin")) {
                                startActivity(new Intent(SplashActivity.this,DashboardAdminActivity.class));
                                finish();
                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError error) {

                        }
                    });
        }

    }
}