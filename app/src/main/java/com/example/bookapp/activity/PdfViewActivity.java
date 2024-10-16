package com.example.bookapp.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwnerKt;

import com.example.bookapp.Constants;
import com.example.bookapp.databinding.ActivityPdfViewBinding;
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
//                        openAddNoteDialog();
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
                    String pageNumber = "Trang 1"; // Cần thay đổi để lấy trang thực tế
                    addNoteToMap(pageNumber, noteText);
                })
                .setNegativeButton("Hủy", null)
                .create();
        dialog.show();
    }

    private void addNoteToMap(String pageNumber, String noteText) {
        notesMap.put(pageNumber, noteText);
        Toast.makeText(this, "Ghi chú đã được thêm", Toast.LENGTH_SHORT).show();
    }

    private void openViewNotesDialog() {
        StringBuilder notesContent = new StringBuilder();
        for (Map.Entry<String, String> entry : notesMap.entrySet()) {
            notesContent.append("Trang: ").append(entry.getKey()).append("\n")
                    .append("Ghi chú: ").append(entry.getValue()).append("\n\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Xem ghi chú")
                .setMessage(notesContent.length() > 0 ? notesContent.toString() : "Không có ghi chú nào!")
                .setPositiveButton("OK", null)
                .show();
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