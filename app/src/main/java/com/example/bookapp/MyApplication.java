package com.example.bookapp;

import static com.example.bookapp.Constants.MAX_BYTES_PDF;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleOwnerKt;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.rajat.pdfviewer.HeaderData;
import com.rajat.pdfviewer.PdfRendererView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

//application class runs before your launcher activity
public class MyApplication extends Application {
    public static final String TAG_DOWLOAD="DOWLOAD_TAG";
    @Override
    public void onCreate() {
        super.onCreate();

    }

    //create a static method to convert timestamp to proper date format, so we can use it everywhere in project, no need to rewirte again
    public static final String formatTimestamp(long timestamp){
        Calendar cal=Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timestamp);
        //format timestamp to dd/MM/yyyy
        String date= DateFormat.format("dd/MM/yyyy", cal).toString();
        return date;
    }
    public static void deleteBook(Context context,String bookId,String bookUrl,String bookTitle) {
        String TAG="DELETE_BOOK_TAG";

        Log.d(TAG,"deleteBook: deleting...");
        ProgressDialog progressDialog=new ProgressDialog(context);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Deleting "+bookTitle+"...");
        progressDialog.show();

        Log.d(TAG,"deleteBook: deleting from storage...");
        StorageReference storageReference= FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl);
        storageReference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG,"onSuccess: deleted from storage");

                        Log.d(TAG,"onSuccess: Now deleting info from db");
                        DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Books");
                        reference.child(bookId)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG,"onSuccess: deleted from db too");
                                        progressDialog.dismiss();
                                        Toast.makeText(context, "Book deleted Successfully...", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG,"onFailure: Failed to delete from db due to "+e.getMessage());
                                        progressDialog.dismiss();
                                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                                    }
                                });

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG,"onFailure: Failed to delete from storage due to "+e.getMessage());
                        progressDialog.dismiss();
                    }
                });
    }

    public static void loadPdfSize(String pdfUrl, String pdfTitle, TextView sizeTv) {
        String TAG="PAD_SIZE_TAG";
        //using url we can get file and its metadata from firebase storage
        StorageReference ref= FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
        ref.getMetadata()
                .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @Override
                    public void onSuccess(StorageMetadata storageMetadata) {
                        //get size in bytes
                        double bytes=storageMetadata.getSizeBytes();
                        Log.d(TAG,"onSuccess: "+ pdfTitle+" "+bytes);
                        //convert bytes to KB, MB
                        double kb=bytes/1024;
                        double mb=kb/1024;
                        if (mb>=1){
                            sizeTv.setText(String.format("%.2f", mb)+" MB");
                        }
                        else if (kb>=1){
                            sizeTv.setText(String.format("%.2f", kb)+" KB");
                        }
                        else {
                            sizeTv.setText(String.format("%.2f", bytes)+" bytes");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG,"onFailure: "+e.getMessage());
                    }
                });
    }

public static void loadPdfFromUrlSinglePage(String pdfUrl, String pdfTitle, PdfRendererView pdfRendererView, ProgressBar progressBar, Context context, TextView pagesTv) {
    // Logging tag
    String TAG = "PDF_LOAD_SINGLE_TAG";

    try {
        Log.d(TAG, "loadPdfFromUrlSinglePage: Get PDF from storage");
        // Tạo tham chiếu tới file PDF trong Firebase Storage
        StorageReference reference = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);

        // Hiển thị progress bar trong khi đang tải
        progressBar.setVisibility(View.VISIBLE);

        reference.getBytes(MAX_BYTES_PDF)
                .addOnSuccessListener(bytes -> {
                    // Sử dụng phương thức initWithUrl để tải PDF từ URL
                    pdfRendererView.initWithUrl(
                            pdfUrl,                    // URL của file PDF
                            new HeaderData(),           // Có thể sử dụng để thêm các tiêu đề
                            LifecycleOwnerKt.getLifecycleScope((LifecycleOwner) context), // Lifecycle owner để quản lý trạng thái
                            ((LifecycleOwner) context).getLifecycle()); // Lifecycle context

                    // Ẩn progress bar khi hoàn tất
                    progressBar.setVisibility(View.GONE);

                })
                .addOnFailureListener(e -> {
                    // Log lỗi và ẩn progress bar
                    Log.e(TAG, "Failed to load PDF: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                });

    } catch (IllegalArgumentException e) {
        // Xử lý lỗi nếu URL PDF không hợp lệ
        Log.e(TAG, "Invalid PDF URL: " + e.getMessage());
        progressBar.setVisibility(View.GONE);
    }
}


    public static void loadCategory(String categoryId,TextView categoryTv) {
        //get category use categoryId
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Categories");
        ref.child(categoryId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get category
                        String category=""+snapshot.child("category").getValue();

                        //set to category text view
                        categoryTv.setText(category);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    public static void incrementBookViewCount(String bookId){
        //get book views count
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get view count
                        String viewsCount=""+snapshot.child("viewsCount").getValue();
                        //in case of null replace with 0
                        if (viewsCount.equals("") || viewsCount.equals("null")){
                            viewsCount="0";
                        }
                        //Increment view count
                        long newViewsCount= Long.parseLong(viewsCount)+1;
                        HashMap<String,Object> hashMap=new HashMap<>();
                        hashMap.put("viewsCount",newViewsCount);

                        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Books");
                        ref.child(bookId)
                                .updateChildren(hashMap);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    public static void dowloadBook(Context context,String bookId,String bookTitle,String bookUrl){
        Log.d(TAG_DOWLOAD,"dowloadBook: dowloading book...");
        String nameWithExtension=bookTitle+".pdf";
        Log.d(TAG_DOWLOAD,"dowloadBook: NAME: "+nameWithExtension);
        //progress dialog
        ProgressDialog progressDialog=new ProgressDialog(context);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Dowloading "+nameWithExtension+"...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
        //dowload from firebase storage using url
        StorageReference reference=FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl);
        reference.getBytes(MAX_BYTES_PDF)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Log.d(TAG_DOWLOAD,"onSuccess: Book dowloaded");
                        Log.d(TAG_DOWLOAD,"onSuccess: Saving book ");
                        saveDowloadBook(context,progressDialog,bytes,nameWithExtension,bookId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG_DOWLOAD,"onFailure: Failed to dowload due to "+e.getMessage());
                        progressDialog.dismiss();
                        Toast.makeText(context, "Failed to dowload due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private static void saveDowloadBook(Context context, ProgressDialog progressDialog, byte[] bytes, String nameWithExtension, String bookId) {
        Log.d(TAG_DOWLOAD,"saveDowloadBook: Saving dowload book");
        try {

            //Lấy thư mục tải xuống
            File dowloadsFolder= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            dowloadsFolder.mkdir();

            //Tạo tệp với đường dẫn ban đầu
            String filePath=dowloadsFolder.getPath()+"/"+nameWithExtension;
            File file = new File(filePath);

            // Kiểm tra nếu tệp đã tồn tại, thêm số vào tên tệp
            int fileNumber = 1;
            String newNameWithExtension = nameWithExtension;
            while (file.exists()) {
                newNameWithExtension = nameWithExtension.replace(".pdf", "(" + fileNumber + ").pdf");
                filePath = dowloadsFolder.getPath() + "/" + newNameWithExtension;
                file = new File(filePath);
                fileNumber++;
            }

            FileOutputStream outputStream=new FileOutputStream(filePath);
            outputStream.write(bytes);
            outputStream.close();

            Toast.makeText(context, "Saved to dowload folder", Toast.LENGTH_SHORT).show();
            Log.d(TAG_DOWLOAD,"saveDowloadBook: Saved to dowload folder");
            progressDialog.dismiss();

            incrementBookDowloadCount(bookId);

        }catch (Exception e){
            Log.d(TAG_DOWLOAD,"saveDowloadBook: Failed saving to dowload folder due to "+e.getMessage());
            Toast.makeText(context, "Failed saving to dowload folder due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        }

    }

    private static void incrementBookDowloadCount(String bookId) {
        Log.d(TAG_DOWLOAD,"incrementBookDowloadCount: incrementing book dowload count");
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String dowloadsCount=""+snapshot.child("dowloadsCount").getValue();
                        Log.d(TAG_DOWLOAD,"onDataChange: Dowloads count: "+dowloadsCount);

                        if (dowloadsCount.equals("")||dowloadsCount.equals("null")){
                            dowloadsCount="0";
                        }
                        //convert to long
                        long newDowloadsCount=Long.parseLong(dowloadsCount)+1;
                        //setup data to update
                        HashMap<String,Object> hashMap=new HashMap<>();
                        hashMap.put("dowloadsCount", newDowloadsCount);
                        //update new incremented dowloads count to db
                        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Books");
                        reference.child(bookId).updateChildren(hashMap)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG_DOWLOAD,"onSuccess: Dowloads count updated: ");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG_DOWLOAD,"onFailure: Failed to update dowloads count due to: "+e.getMessage());
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    public static void addToFavorite(Context context,String bookId){
        //Chỉ có thể thêm nếu đã login
        //ktra xem login chưa
        FirebaseAuth firebaseAuth=FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null){
            Toast.makeText(context, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
        }
        else {
            long timestamp=System.currentTimeMillis();
            //thiết lập db để thêm vào firebase db của ng dùng hiện tại cho cuốn sách yêu thích
            HashMap<String,Object> hashMap=new HashMap<>();
            hashMap.put("bookId", bookId);
            hashMap.put("timestamp", timestamp);
            // save to db
            DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favorites").child(bookId)
                    .setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(context, "Đã thêm vào danh sách yêu thích của bạn ", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(context, "Không thể thêm vào mục yêu thích do: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    public static void removeFromFarivate(Context context,String bookId){
        //Chỉ có thể xoá nếu đã login
        //ktra xem login chưa
        FirebaseAuth firebaseAuth=FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null){
            Toast.makeText(context, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
        }
        else {
            // remove from db
            DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favorites").child(bookId)
                    .removeValue()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(context, "Đã xoá khỏi danh sách yêu thích của bạn ", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(context, "Không thể xoá khỏi danh mục yêu thích do: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
