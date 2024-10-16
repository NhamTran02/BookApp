package com.example.bookapp.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bookapp.databinding.ActivityPdfEditBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class PdfEditActivity extends AppCompatActivity {

    //view binding
    private ActivityPdfEditBinding binding;
    //book id get  from intent started from AdapterPdfAdmin
    private String bookId;
    //progress dialog
    private ProgressDialog progressDialog;
    private ArrayList<String> categoryTitleArraylist,categoryIdArraylist;
    private static final String TAG="BOOK_EDIT_TAG";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityPdfEditBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        bookId=getIntent().getStringExtra("bookId");
        //setup progress dialog
        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        loadCategories();
        loadBookInfo();
        //hanle click, pick category
        binding.categoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                categoryDialog();
            }
        });

        //handle click, go to previous screen
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        //handle click, begin upload
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });
    }

    String title="",description="";
    private void validateData() {
        //get data
        title=binding.titleEt.getText().toString().trim();
        description=binding.descriptionEt.getText().toString().trim();

        if (TextUtils.isEmpty(title)){
            Toast.makeText(this, "Enter Title...", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(description)) {
            Toast.makeText(this, "Enter Description...", Toast.LENGTH_SHORT).show();

        }else if (TextUtils.isEmpty(selectedCategoryId)) {
            Toast.makeText(this, "Pick Category", Toast.LENGTH_SHORT).show();

        }else {
            updatePdf();
        }

    }

    private void updatePdf() {
        Log.d(TAG,"updatePdf: Starting update pdf info to db...");
        //show progress
        progressDialog.setMessage("Updating book info...");
        progressDialog.show();
        //setup data to update to db
        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("title", ""+title);
        hashMap.put("description", ""+description);
        hashMap.put("categoryId", ""+selectedCategoryId);
        //start updating
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG,"onSuccess: book updated...");
                        Toast.makeText(PdfEditActivity.this, "book info updated", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG,"onFailure: failed to update due to:"+e.getMessage());
                        progressDialog.dismiss();
                        Toast.makeText(PdfEditActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadBookInfo() {
        Log.d(TAG,"loadBookInfo: loading book info");
        DatabaseReference refBook=FirebaseDatabase.getInstance().getReference("Books");
        refBook.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get book info
                        selectedCategoryId=""+snapshot.child("categoryId").getValue();
                        String description=""+snapshot.child("description").getValue();
                        String title=""+snapshot.child("title").getValue();
                        //set to views

                        binding.titleEt.setText(title);
                        binding.descriptionEt.setText(description);

                        Log.d(TAG,"loadBookInfo: loading book category info");
                        DatabaseReference refBookCategopry=FirebaseDatabase.getInstance().getReference("Categories");
                        refBookCategopry.child(selectedCategoryId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        //get category
                                        String category=""+snapshot.child("category").getValue();
                                        //set to category text view
                                        binding.categoryTv.setText(category);

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private String selectedCategoryId="",selectedCategoryTitle="";
    private void categoryDialog(){
        //make string array from arraylist of string
        String[] categoriesArray=new String[categoryTitleArraylist.size()];
        for (int i=0;i<categoryTitleArraylist.size();i++){
            categoriesArray[i]=categoryTitleArraylist.get(i);
        }
        //alert dialog
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Choose Category")
                .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selectedCategoryId=categoryIdArraylist.get(i);
                        selectedCategoryTitle=categoryTitleArraylist.get(i);
                        //set to textview
                        binding.categoryTv.setText(selectedCategoryTitle);

                    }
                }).show();
    }
    private void loadCategories() {
        Log.d(TAG,"loadCategories: loading categories...");
        categoryIdArraylist=new ArrayList<>();
        categoryTitleArraylist=new ArrayList<>();
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Categories");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryIdArraylist.clear();
                categoryTitleArraylist.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    String id=""+ds.child("id").getValue();
                    String category=""+ds.child("category").getValue();
                    categoryIdArraylist.add(id);
                    categoryTitleArraylist.add(category);

                    Log.d(TAG,"onDataChange: Id:"+id);
                    Log.d(TAG,"onDataChange: Category: "+category);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
}