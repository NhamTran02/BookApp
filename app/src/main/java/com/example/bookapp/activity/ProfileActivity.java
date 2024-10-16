package com.example.bookapp.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.bookapp.MyApplication;
import com.example.bookapp.R;
import com.example.bookapp.adapter.AdapterPdfFavorite;
import com.example.bookapp.databinding.ActivityProfileBinding;
import com.example.bookapp.model.ModelPdf;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {
    //view binding
    ActivityProfileBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private ArrayList<ModelPdf> pdfArrayList;
    private AdapterPdfFavorite adapterPdfFavotire;
    private static final String TAG="PROFILE_TAG";
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityProfileBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        //reset data of user info
        binding.accountTypeTv.setText("N/A");
        binding.memberDateTv.setText("N/A");
        binding.favoriteBookCountTv.setText("N/A");
        binding.accountStatusTv.setText("N/A");


        //setup firebase auth
        firebaseAuth=FirebaseAuth.getInstance();
        //get current user
        firebaseUser=firebaseAuth.getCurrentUser();
        //init progress dialog
        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        loadUserInfo();

        loadFavoriteBooks();

        //handle click, back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //handle click, start profile edit page
        binding.profileEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ProfileActivity.this,ProfileEditActivity.class));
            }
        });
        //handle click,verify user if not
        binding.accountStatusTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (firebaseUser.isEmailVerified()){
                    //đã đc xác minh
                    Toast.makeText(ProfileActivity.this, "Đã được xác minh", Toast.LENGTH_SHORT).show();
                }else {
                    //chưa đc xác minh
                    emailVerificationDialog();
                }
            }
        });
    }

    private void emailVerificationDialog() {
        //alert dialog
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Verify email")
                .setMessage("Bạn có chắc muốn gửi hướng dẫn xác minh tới email: "+firebaseUser.getEmail())
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sendEmailVerification();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();
    }

    private void sendEmailVerification() {
        //show progress
        progressDialog.setMessage("Gửi xác minh email tới email: "+firebaseUser.getEmail());
        progressDialog.show();

        firebaseUser.sendEmailVerification()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        progressDialog.dismiss();
                        Toast.makeText(ProfileActivity.this, "Đã gửi hướng dẫn xác minh tới email, hãy kiểm tra email của bạn: "+firebaseUser.getEmail(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(ProfileActivity.this, "Lỗi do: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void loadUserInfo() {
        Log.d(TAG,"loadUserInfo: loading user info of user: "+firebaseAuth.getUid());
        //get email verification status, sau khi xác minh hãy đăng nhập lại để thay đổi
        if (firebaseUser.isEmailVerified()){
            binding.accountStatusTv.setText("Verified");
        }else {
            binding.accountStatusTv.setText("Not Verified");
        }
        DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get all info of user here from snapshot
                        String email=""+snapshot.child("email").getValue();
                        String name=""+snapshot.child("name").getValue();
                        String profileImage=""+snapshot.child("profileImage").getValue();
                        String timestamp=""+snapshot.child("timestamp").getValue();
                        String uid=""+snapshot.child("uid").getValue();
                        String userType=""+snapshot.child("userType").getValue();
                        //format date to dd/MM/yyyy
                        String formatDate= MyApplication.formatTimestamp(Long.parseLong(timestamp));
                        //set data to ui
                        binding.emailTv.setText(email);
                        binding.nameTv.setText(name);
                        binding.memberDateTv.setText(formatDate);
                        binding.accountTypeTv.setText(userType);
                        //set image,using glide
                        Glide.with(ProfileActivity.this)
                                .load(profileImage)
                                .placeholder(R.drawable.ic_person_gray)
                                .into(binding.profileIv);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadFavoriteBooks() {
        //init list
        pdfArrayList=new ArrayList<>();
        //load favorite books from database
        //User>userId>Favorite
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid()).child("Favorites")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            pdfArrayList.clear();
                            for (DataSnapshot data:snapshot.getChildren()){
                                String bookId = "" + data.child("bookId").getValue();
                                //set id to model
                                ModelPdf modelPdf=new ModelPdf();
                                modelPdf.setId(bookId);
                                //add model to list
                                pdfArrayList.add(modelPdf);
                            }
                            //set số lượng favorite book
                            binding.favoriteBookCountTv.setText(String.valueOf(pdfArrayList.size()));
                            //setup adapter
                            adapterPdfFavotire=new AdapterPdfFavorite(ProfileActivity.this, pdfArrayList);
                            //setup adapter to recyclerview
                            binding.bookRv.setAdapter(adapterPdfFavotire);
                        }catch (Exception e){
                            Log.d(TAG, "Error: "+e.getMessage());
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}