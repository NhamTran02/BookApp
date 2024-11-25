package com.example.bookapp.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwnerKt;

import com.example.bookapp.Constants;
import com.example.bookapp.R;
import com.example.bookapp.databinding.ActivityPdfViewBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.rajat.pdfviewer.HeaderData;

import java.util.HashMap;
import java.util.Map;

public class PdfViewActivity extends AppCompatActivity {
    //view binding
    private ActivityPdfViewBinding binding;

    private String bookId;
    private Map<String, String> notesMap = new HashMap<>(); // Lưu ghi chú cho các đoạn văn bản

    private static final String TAG="PDF_VIEW_TAG";


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityPdfViewBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        //get bookId from intent that we passed in intent
        Intent intent=getIntent();
        bookId=intent.getStringExtra("bookId");
        Log.d(TAG,"onCreate: BookId: "+bookId);

        loadBookDetails();

        //handle click,go back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        //handle click, note
        binding.notesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNotesMenu(view);
            }
        });

    }

    private void showNotesMenu(View view) {
        //Tạo PopupMenu
        PopupMenu popupMenu=new PopupMenu(this, view);
        popupMenu.getMenu().add("Thêm ghi chú");
        popupMenu.getMenu().add("Xem ghi chú");
        //xử lý sự kiện khi chọn menu
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getTitle().toString()) {
                    case "Thêm ghi chú":
                        openAddNoteDialog();
                        break;
                    case "Xem ghi chú":
                        openViewNotesDialog();
                        break;
                }
                return true;
            }
        });
        popupMenu.show();
    }

    private void openAddNoteDialog() {
        // Tạo hộp thoại để nhập ghi chú
        EditText noteInput = new EditText(this);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Thêm ghi chú")
                .setMessage("Nhập ghi chú cho đoạn văn bản đã chọn:")
                .setView(noteInput)
                .setPositiveButton("Lưu", (dialog1, which) -> {
                    String noteText = noteInput.getText().toString();
                    String pageNumber = "1"; // Cần thay đổi để lấy trang thực tế
                    addNoteToMap(pageNumber, noteText);
                })
                .setNegativeButton("Hủy", null)
                .create();
        dialog.show();
    }

    private void addNoteToMap(String pageNumber, String noteText) {
        // Tham chiếu đến node sách cụ thể trong Firebase Database
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Books").child(bookId);
        // Tạo đối tượng ghi chú để lưu lên Firebase
        Map<String,String> note=new HashMap<>();
        note.put("pageNumber", pageNumber);
        note.put("noteText", noteText);

        // Tạo một id duy nhất cho ghi chú
        String noteId=reference.push().getKey();
        if (noteId != null){
            reference.child("Notes").child(noteId).setValue(note)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(PdfViewActivity.this, "Ghi chú đã được thêm", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(PdfViewActivity.this, "Lỗi khi lưu ghi chú", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        notesMap.put(pageNumber, noteText);
        Toast.makeText(this, "Ghi chú đã được thêm", Toast.LENGTH_SHORT).show();
    }

    private void openViewNotesDialog() {
        // Tham chiếu tới Firebase Database
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Books").child(bookId).child("Notes");
        // Lấy dữ liệu từ Firebase
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder note=new StringBuilder();
                // Duyệt qua từng ghi chú
                for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                    String noteId = dataSnapshot.getKey();
                    String pageNumber = dataSnapshot.child("pageNumber").getValue(String.class);
                    String noteText = dataSnapshot.child("noteText").getValue(String.class);
                    if (pageNumber != null && noteText != null) {
                        note.append("ID: ").append(noteId).append("\n\n")
                                .append("Trang: ").append(pageNumber).append("\n")
                                .append("Ghi chú: ").append(noteText).append("\n\n");
                    }
                }
                // Hiển thị dialog với nội dung ghi chú
                new android.app.AlertDialog.Builder(PdfViewActivity.this)
                        .setTitle("Xem ghi chú")
                        .setMessage(note.length()>0 ? note.toString() : "Không có ghi chú nào!")
                        .setPositiveButton("OK", null)
                        .setNegativeButton("Xóa ghi chú", (dialog, which) -> {
                            // Xóa tất cả ghi chú
                            deleteAllNotesFromFirebase();
                        })
                        .show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Xử lý lỗi khi truy cập Firebase
                Toast.makeText(PdfViewActivity.this, "Lỗi khi tải ghi chú: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Lỗi khi tải ghi chú: ", error.toException());
            }
        });
    }

    private void deleteAllNotesFromFirebase() {
        // Reference to the Firebase node where the notes are stored
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Books").child(bookId).child("Notes");

        // Check if there are any notes before attempting to delete
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // If there are notes, remove them
                    reference.removeValue()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(PdfViewActivity.this, "Ghi chú đã được xoá", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(PdfViewActivity.this, "Lỗi khi xóa ghi chú", Toast.LENGTH_SHORT).show());
                } else {
                    // If no notes exist, show a message
                    Toast.makeText(PdfViewActivity.this, "Không có ghi chú nào để xoá", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle the error
                Toast.makeText(PdfViewActivity.this, "Lỗi khi kiểm tra ghi chú: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Lỗi khi kiểm tra ghi chú: ", error.toException());
            }
        });
    }



    private void loadBookDetails() {
        Log.d(TAG,"loadBookDetails: Get pdf url...");
        //tham chiếu cơ sở dữ liệu để lấy thông tin về sách
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get book url
                        String pdfUrl=""+snapshot.child("url").getValue(String.class);
                        Log.d(TAG,"onDataChange: Pdf url: "+pdfUrl);
                        //load pdf using that url from firebase storage
                        loadBookFromUrl(pdfUrl);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

    public void loadBookFromUrl(String pdfUrl) {
        try {
            Log.d(TAG,"loadBookFromUrl: Get PDF from storage ");
            StorageReference reference = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
            reference.getBytes(Constants.MAX_BYTES_PDF)
                    .addOnSuccessListener(bytes -> {
                        // Create a temporary file from byte array
                        // Hiển thị thanh tiến trình
                        binding.progressBar.setVisibility(View.VISIBLE);

                        binding.pdfView.initWithUrl(
                                pdfUrl,
                                new HeaderData(),
                                LifecycleOwnerKt.getLifecycleScope(this),
                                getLifecycle());
                        // Ẩn thanh tiến trình khi tải xong
                        binding.progressBar.setVisibility(View.GONE);

                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get file from URL: " + e.getMessage());
                        binding.progressBar.setVisibility(View.GONE);
                    });
        }catch (IllegalArgumentException e){
            Log.e(TAG, "Invalid PDF URL: " + e.getMessage());
            binding.progressBar.setVisibility(View.GONE);
        }

    }
}