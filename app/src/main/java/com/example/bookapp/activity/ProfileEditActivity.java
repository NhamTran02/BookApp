package com.example.bookapp.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.bookapp.R;
import com.example.bookapp.databinding.ActivityProfileEditBinding;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.util.HashMap;

public class ProfileEditActivity extends AppCompatActivity {
    // View binding
    ActivityProfileEditBinding binding;
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;
    private static final String TAG = "PROFILE_EDIT_TAG";
    private Uri imageUri;
    private String name = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileEditBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        // Setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false); // không thể bỏ qua khi nhấp ngoài tiến trình này

        // Setup firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        loadUserInfo();

        binding.backBtn.setOnClickListener(view -> onBackPressed());
        // Handle click, pick image
        binding.profileIv.setOnClickListener(view -> showImageAttachMenu());
        // Handle click, update profile
        binding.updateBtn.setOnClickListener(view -> validateData());
    }

    private void loadUserInfo() {
        Log.d(TAG, "loadUserInfo: loading user info of user: " + firebaseAuth.getUid());
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Get all info of user here from snapshot
                        String email = "" + snapshot.child("email").getValue();
                        String name = "" + snapshot.child("name").getValue();
                        String profileImage = "" + snapshot.child("profileImage").getValue();
                        String timestamp = "" + snapshot.child("timestamp").getValue();
                        String uid = "" + snapshot.child("uid").getValue();
                        String userType = "" + snapshot.child("userType").getValue();
                        // Set data to UI
                        binding.nameEt.setText(name);
                        // Set image, using Glide
                        Glide.with(ProfileEditActivity.this)
                                .load(profileImage)
                                .placeholder(R.drawable.ic_person_gray)
                                .into(binding.profileIv);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "onCancelled: " + error.getMessage());
                    }
                });
    }

    private void validateData() {
        name = binding.nameEt.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Enter name...", Toast.LENGTH_SHORT).show();
        } else {
            if (imageUri == null) {
                // Cập nhật mà không có hình ảnh
                updateProfile("");
            } else {
                // Cập nhật có hình ảnh
                uploadImage();
            }
        }
    }

    private void updateProfile(String imageUrl) {
        Log.d(TAG, "updateProfile: Updating user profile");
        progressDialog.setMessage("Updating user profile...");
        progressDialog.show();
        // Setup data to update in db
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("name", name);
        if (imageUri != null) {
            hashMap.put("profileImage", imageUrl);
        }
        // Update data to db
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid())
                .updateChildren(hashMap)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "onSuccess: profile updated");
                    progressDialog.dismiss();
                    Toast.makeText(ProfileEditActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "onFailure: Failed to update db due to: " + e.getMessage());
                    progressDialog.dismiss();
                    Toast.makeText(ProfileEditActivity.this, "Failed to update db: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadImage() {
        Log.d(TAG, "uploadImage: Uploading profile image");
        progressDialog.setMessage("Updating profile image");
        progressDialog.show();

        String filePathAndName = "ProfileImages/" + firebaseAuth.getUid();
        // Storage reference
        StorageReference reference = FirebaseStorage.getInstance().getReference(filePathAndName);
        reference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "onSuccess: profile image uploaded");
                    Log.d(TAG, "onSuccess: Getting url of uploaded image");
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    uriTask.addOnSuccessListener(uploadedImageUrl -> {
                        Log.d(TAG, "onSuccess: Uploaded image Url: " + uploadedImageUrl);
                        updateProfile(uploadedImageUrl.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "onFailure: Failed to upload image: " + e.getMessage());
                    progressDialog.dismiss();
                    Toast.makeText(ProfileEditActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showImageAttachMenu() {
        // Init/setup popup menu
        PopupMenu popupMenu = new PopupMenu(this, binding.profileIv);
        popupMenu.getMenu().add(Menu.NONE, 0, 0, "Camera");
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "Gallery");

        popupMenu.show();
        // Handle menu item click
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int i = menuItem.getItemId();
            if (i == 0) {
                // Camera click
                pickImageCamera();
            } else if (i == 1) {
                // Gallery
                pickImageGallery();
            }
            return false;
        });
    }

    private void pickImageCamera() {
        // Check for camera permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 1);
        } else {
            // Chọn image từ camera
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "New Pick");
            values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Image Description");
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "sample_image.jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Log.d(TAG,"pickImageCamera: "+imageUri);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            cameraActivityResultLauncher.launch(intent);
        }
    }

    private void pickImageGallery() {
        // Chọn image từ bộ sưu tập
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG, "onActivityResult: " + imageUri);
                        if (imageUri != null) {
                            binding.profileIv.setImageURI(imageUri);
                        } else {
                            Log.e(TAG, "onActivityResult: Image URI is null after capturing");
                        }
                    } else {
                        Toast.makeText(ProfileEditActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        imageUri = data.getData();
                        if (imageUri != null) {
                            Log.d(TAG, "onActivityResult: Picked from gallery " + imageUri);


                            Glide.with(ProfileEditActivity.this).load(imageUri).into(binding.profileIv);
                        } else {
                            Log.d(TAG, "onActivityResult: Image URI is null after picking from gallery.");
                        }
                    } else {
                        Log.d(TAG, "onActivityResult: Cancelled or data is null");
                        Toast.makeText(ProfileEditActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}