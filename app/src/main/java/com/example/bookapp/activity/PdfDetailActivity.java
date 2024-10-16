package com.example.bookapp.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.bookapp.MyApplication;
import com.example.bookapp.R;
import com.example.bookapp.adapter.AdapterComment;
import com.example.bookapp.databinding.ActivityPdfDetailBinding;
import com.example.bookapp.databinding.DialogCommentAddBinding;
import com.example.bookapp.model.ModelComment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class PdfDetailActivity extends AppCompatActivity {
    //viewBinding
    private ActivityPdfDetailBinding binding;
    //pdf id,get from intent
    String bookId,bookTitle,bookUrl;
    private Context context;
    boolean isInmyFavorite=false;
    private FirebaseAuth firebaseAuth;
    public static final String TAG_DOWLOAD="DOWLOAD_TAG";
    private ProgressDialog progressDialog;
    private ArrayList<ModelComment> commentArrayList;
    private AdapterComment adapterComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityPdfDetailBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        //get data from intent e.g bookId
        Intent intent=getIntent();
        bookId=intent.getStringExtra("bookId");

        firebaseAuth=FirebaseAuth.getInstance();

        //at start hide dowload button, because we need book url that we will load later in function loadBookDetails();
        binding.dowloadBookBtn.setVisibility(View.GONE);
        //init progress dialog
        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        loadBookDetails();
        loadComments();
        //increment book view count, whenever this page starts
        MyApplication.incrementBookViewCount(bookId);

        //handle click, go back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        //handle click, read book
        binding.readBookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1=new Intent(PdfDetailActivity.this,PdfViewActivity.class);
                intent1.putExtra("bookId", bookId);
                startActivity(intent1);
            }
        });
        //handle click,dowload book
        binding.dowloadBookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG_DOWLOAD,"onClick: Checking permission");
                if (ContextCompat.checkSelfPermission( PdfDetailActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG_DOWLOAD,"onClick: Permission already granted, can dowload book");
                    MyApplication.dowloadBook(PdfDetailActivity.this, ""+bookId, ""+bookTitle, ""+bookUrl);
                }else {
                    Log.d(TAG_DOWLOAD,"onClick: Permission was not granted,request permission...");
                    requestPermissionLaucher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
        });

        //handle click, add/remove favorite
        binding.favoriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (firebaseAuth.getCurrentUser() == null){
                    Toast.makeText(PdfDetailActivity.this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
                }
                else {
                    if (isInmyFavorite){
                        //đã trong favorite, xoá khỏi favorite
                        MyApplication.removeFromFarivate(PdfDetailActivity.this, bookId);
                        checkIsFavorite();
                    }else {
                        //Không có trong favorite, thêm vào favorite
                        MyApplication.addToFavorite(PdfDetailActivity.this, bookId);
                        checkIsFavorite();
                    }
                }
            }
        });
        //handle click, show comment add dialog
        binding.addComentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (firebaseAuth.getCurrentUser() == null){
                    Toast.makeText(PdfDetailActivity.this, "Bạn phải đăng nhập mới dùng đc chức năng này", Toast.LENGTH_SHORT).show();
                }else {
                    addCommentDialog();
                }
            }
        });
        onResume();
    }

    private void loadComments() {
        commentArrayList=new ArrayList<>();
        //db path to load comments
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Books");
        reference.child(bookId).child("Comment")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        commentArrayList.clear();
                        for (DataSnapshot ds:snapshot.getChildren()){
                            //Lấy dữ liệu làm model, cách viết các biến trong model phải giống trong firebase
                            ModelComment model=ds.getValue(ModelComment.class);
                            //thêm vào arraylist
                            commentArrayList.add(model);
                        }
                        //setup adapter
                        adapterComment=new AdapterComment(PdfDetailActivity.this, commentArrayList);
                        //setup adapter to recycleview
                        binding.commentsRv.setAdapter(adapterComment);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseError", "Error loading comments: " + error.getMessage());
                    }
                });
    }

    private String comment="";
    private void addCommentDialog() {
        //inflate bind view for dialog
        DialogCommentAddBinding commentAddBinding=DialogCommentAddBinding.inflate(LayoutInflater.from(this));
        //setup alert dialog builder
        AlertDialog.Builder builder=new AlertDialog.Builder(this,R.style.CustomDialog);
        builder.setView(commentAddBinding.getRoot());
        //create and show alert dialog
        AlertDialog alertDialog=builder.create();
        alertDialog.show();
        //handle click, dismis dialog
        commentAddBinding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
        //handle click, add comment
        commentAddBinding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get data
                comment=commentAddBinding.commentEt.getText().toString().trim();
                if (TextUtils.isEmpty(comment)){
                    Toast.makeText(PdfDetailActivity.this, "Hãy nhập nhận xét của bạn...", Toast.LENGTH_SHORT).show();
                }else {
                    alertDialog.dismiss();
                    addComment();
                }
            }
        });

    }

    private void addComment() {
        //show progress
        progressDialog.setMessage("Adding comment...");
        progressDialog.show();
        //timestamp for comment id,comment title
        String timestamp= String.valueOf(System.currentTimeMillis());
        //set data to add in db for comment
        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("id", ""+timestamp);
        hashMap.put("bookId", ""+bookId);
        hashMap.put("timestamp", ""+timestamp);
        hashMap.put("comment", ""+comment);
        hashMap.put("uid", ""+firebaseAuth.getUid());
        //db path to add data into it
        //Books > bookId > Comments > CommentId > commentData
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Books");
        reference.child(bookId).child("Comment").child(timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(PdfDetailActivity.this, "Thêm nhận xét thành công", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed to add comment
                        progressDialog.dismiss();
                        Toast.makeText(PdfDetailActivity.this, "Thêm nhận xét lỗi do: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Load book details and increment views count
        loadBookDetails();
        MyApplication.incrementBookViewCount(bookId);
    }
    //request storage permission
    private ActivityResultLauncher<String> requestPermissionLaucher=
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted ->{
                if (isGranted){
                    Log.d(TAG_DOWLOAD,"Permission Granted");
                    MyApplication.dowloadBook(context, ""+bookId, ""+bookTitle,""+bookUrl);

                }else {
                    Log.d(TAG_DOWLOAD,"Permission was denied");
                    Toast.makeText(context, "Permission was denied", Toast.LENGTH_SHORT).show();
                }
            });
    private void loadBookDetails() {
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get data
                        bookTitle=""+snapshot.child("title").getValue();
                        String description=""+snapshot.child("description").getValue();
                        String categoryId=""+snapshot.child("categoryId").getValue();
                        String viewsCount=""+snapshot.child("viewsCount").getValue();
                        String dowloadsCount=""+snapshot.child("dowloadsCount").getValue();
                        bookUrl=""+snapshot.child("url").getValue();
                        String timestamp=""+snapshot.child("timestamp").getValue();

                        //required data  is loaded, show dowload button
                        binding.dowloadBookBtn.setVisibility(View.VISIBLE);

                        //format date
                        String date= MyApplication.formatTimestamp(Long.parseLong(timestamp));

                        MyApplication.loadCategory(""+categoryId, binding.categoryTv);

                        MyApplication.loadPdfFromUrlSinglePage(""+bookUrl, ""+bookTitle,
                                binding.pdfView,binding.progressBar, PdfDetailActivity.this,binding.pagesTv);

                        MyApplication.loadPdfSize(""+bookUrl, ""+bookTitle, binding.sizeTv);

                        //set data
                        binding.titleTv.setText(bookTitle);
                        binding.descriptionTv.setText(description);
                        binding.viewsTv.setText(viewsCount.replace("null", "N/A"));
                        binding.dowloadsTv.setText(dowloadsCount.replace("null", "N/A"));
                        binding.dateTv.setText(date);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void checkIsFavorite(){
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Favorites").child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isInmyFavorite=snapshot.exists();
                        Log.d("Favorites Check", "isInmyFavorite: " + isInmyFavorite);
                        if (isInmyFavorite){//true: if exists, false: if not exists
                            //exists in favorite
                            Log.d("Favorites Check", "Book is in favorites, updating UI");
                            binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_favorite_white, 0, 0);
                            binding.favoriteBtn.setText("Remove Favorite");
                        }
                        else {
                            //not exists in favorite
                            Log.d("Favorites Check", "Book is not in favorites, updating UI");
                            binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_favorite_border_white, 0, 0);
                            binding.favoriteBtn.setText("Add Favorite");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }
}