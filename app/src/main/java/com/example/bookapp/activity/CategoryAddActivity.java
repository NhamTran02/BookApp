package com.example.bookapp.activity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bookapp.databinding.ActivityCategoryAddBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class CategoryAddActivity extends AppCompatActivity {
    //view binding
    private ActivityCategoryAddBinding binding;
    //firebase auth
    private FirebaseAuth firebaseAuth;

    //progress dialog
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityCategoryAddBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        //init firebase auth
        firebaseAuth= FirebaseAuth.getInstance();

        //configure progress dialog
        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //handle click, back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //handle click,begin upload category
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateDate();
            }
        });
    }
    private String category="";
    private void validateDate() {
        //get data
        category=binding.categoryEt.getText().toString().trim();
        //validate
        if (TextUtils.isEmpty(category)){
            Toast.makeText(this,"Please enter category...",Toast.LENGTH_SHORT).show();
        }else {
            addCategoryFirebase();
        }

    }

    private void addCategoryFirebase() {
        //show progress
        progressDialog.setMessage("Adding category...");
        progressDialog.show();
        //get timestamp
        long timestamp=System.currentTimeMillis();
        //setup info to add in firebase db
        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("id", ""+timestamp);
        hashMap.put("category", ""+category);
        hashMap.put("timestamp", ""+timestamp);
        hashMap.put("uid", ""+firebaseAuth.getUid());

        //add to firebase db... Database Root > Categories > CategoryId >Catgory info
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Categories");
        ref.child(""+timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        progressDialog.dismiss();
                        Toast.makeText(CategoryAddActivity.this,"Category added successfully...",Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(CategoryAddActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
    }
}