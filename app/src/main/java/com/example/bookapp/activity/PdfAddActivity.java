package com.example.bookapp.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bookapp.databinding.ActivityPdfAddBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;

public class PdfAddActivity extends AppCompatActivity {
    //view binding
    private ActivityPdfAddBinding binding;

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    private ArrayList<String> categoryTitleArrayList,categoryIdArrayList;

    private Uri pdfUri=null;

    private static final int PDF_PICK_CODE=1000;
    //tag for debugging
    private static final String TAG="ADD_PDF_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityPdfAddBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        //init firebaseAuth
        firebaseAuth=FirebaseAuth.getInstance();
        loadPdfCategories();

        //setup progress dialog
        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //handle click, backbtn
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        //handle click, attach pdf
        binding.attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pdfPickIntent();
            }
        });
        //handle click,pick category
        binding.categoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                categoryPickDialog();
            }
        });
        //handle click, uploadpdf
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });
    }

    private String title="",description="";
    private void validateData() {
        title=binding.titleEt.getText().toString().trim();
        description=binding.descriptionEt.getText().toString().trim();

        if(TextUtils.isEmpty(title)){
            Toast.makeText(this,"Enter Title",Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(description)) {
            Toast.makeText(this,"Enter Description",Toast.LENGTH_SHORT).show();
        }else if (TextUtils.isEmpty(selectedCategoryTitle)) {
            Toast.makeText(this,"Enter Category",Toast.LENGTH_SHORT).show();
        }else if (pdfUri==null){
            Toast.makeText(this,"Pick Pdf",Toast.LENGTH_SHORT).show();
        }else {
            //can upload now
            uploadPdfToStorage();
        }
    }

    private void uploadPdfToStorage() {
        //uplaod pdf to firebase storage
        Log.d(TAG,"uploadPdfToStorage: uploading to storage...");
        //show progress
        progressDialog.setMessage("Uploading Pdf...");
        progressDialog.show();
        //timestamp
        long timestamp=System.currentTimeMillis();
        //path of pdf in firebase storage
        String filePathAndName="Book/"+timestamp;
        //storage reference
        StorageReference storageReference= FirebaseStorage.getInstance().getReference(filePathAndName);
        storageReference.putFile(pdfUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG,"onSuccess: Pdf uploaded to storage");
                        Log.d(TAG,"onSuccess: getting pdf url ");

                        //get pdf url
                        Task<Uri> uriTask=taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String uploadedPdfUrl=""+uriTask.getResult();
                        //upload to firebase db
                        uploadPdfInfoToDb(uploadedPdfUrl,timestamp);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG,"onFailure: Pdf upload failed due to "+e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "Pdf upload failed due to", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadPdfInfoToDb(String uploadedPdfUrl, long timestamp) {
        //upload pdf info to firebase db
        Log.d(TAG,"uploadPdfInfoToDb: uploading pdf info to firebase db...");
        progressDialog.setMessage("Uploading pdf info...");
        String uid=firebaseAuth.getUid();
        //setup data to upload, also add view count,dowload count while adding pdf/book
        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("uid",""+uid);
        hashMap.put("id", ""+timestamp);
        hashMap.put("title", ""+title);
        hashMap.put("description", ""+description);
        hashMap.put("categoryId", ""+selectedCategoryId);
        hashMap.put("url", ""+uploadedPdfUrl);
        hashMap.put("timestamp", ""+timestamp);
        hashMap.put("viewsCount", 0);
        hashMap.put("dowloadsCount", 0);

        //db reference: DB > Books
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Books");
        ref.child(""+timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        progressDialog.dismiss();
                        Log.d(TAG,"onSuccess: Successfully Uploaded...");
                        Toast.makeText(PdfAddActivity.this, "Successfully Uploaded", Toast.LENGTH_SHORT).show();


                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG,"onFailure: Failed to upload to db due to "+e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "Failed to upload to db due to", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadPdfCategories() {
        Log.d(TAG,"loadPdfCategories: Loading pdf categories...");
        categoryTitleArrayList =new ArrayList<>();
        categoryIdArrayList=new ArrayList<>();
        //tham chiếu để tải danh mục > Categories
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Categories");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryTitleArrayList.clear();
                categoryIdArrayList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    //get id and title of category
                    String categoryId=""+ds.child("id").getValue();
                    String categoryTitle=""+ds.child("category").getValue();
                    //add to respective arrayList
                    categoryTitleArrayList.add(categoryTitle);
                    categoryIdArrayList.add(categoryId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //selected category id and category title
    private String selectedCategoryId, selectedCategoryTitle;
    private void categoryPickDialog() {
        Log.d(TAG,"categoryPickDialog: showing category pick dialog");
        //lấy mảng các danh mục từ arraylist
        String[] categoryArray=new String[categoryTitleArrayList.size()];
        for (int i = 0; i< categoryTitleArrayList.size(); i++){
            categoryArray[i]= categoryTitleArrayList.get(i);
        }
        //alert dialog
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Pick Category")
                .setItems(categoryArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //handle item click
                        //get clicked item from list
                        selectedCategoryTitle=categoryTitleArrayList.get(i);
                        selectedCategoryId=categoryIdArrayList.get(i);
                        //set to category textview
                        binding.categoryTv.setText(selectedCategoryTitle);
                        Log.d(TAG,"onClick: Selected Category: "+selectedCategoryId+" "+selectedCategoryTitle);
                    }
                }).show();
    }

    private void pdfPickIntent() {
        Log.d(TAG, "pdfOickIntent: starting pdf pick intent");
        Intent intent=new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Pdf"), PDF_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode==RESULT_OK){
            if (requestCode==PDF_PICK_CODE){
                Log.d(TAG, "onActivityResult: PDF Picked");
                pdfUri=data.getData();
                Log.d(TAG, "onActivityResult: URI: "+ pdfUri);
            }
        }else {
            Log.d(TAG,"onActivityResult: cancelled picking pdf");
            Toast.makeText(this, "cancelled picking pdf", Toast.LENGTH_SHORT).show();
        }
    }
}