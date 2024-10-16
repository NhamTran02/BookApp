package com.example.bookapp.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.inputmethodservice.Keyboard;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bookapp.MyApplication;
import com.example.bookapp.R;
import com.example.bookapp.databinding.RowCommentBinding;
import com.example.bookapp.model.ModelComment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AdapterComment extends RecyclerView.Adapter<AdapterComment.HolderComment> {
    private Context context;
    private ArrayList<ModelComment> commentArrayList;
    //view binding
    private RowCommentBinding binding;
    private FirebaseAuth firebaseAuth;

    public AdapterComment(Context context, ArrayList<ModelComment> commentArrayList) {
        this.context = context;
        this.commentArrayList = commentArrayList;

        firebaseAuth=FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public HolderComment onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate/bind the view xml
        binding= RowCommentBinding.inflate(LayoutInflater.from(context),parent,false);
        return new HolderComment(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderComment holder, int position) {
        //get data
        ModelComment modelComment=commentArrayList.get(position);
        String id=modelComment.getId();
        String bookId=modelComment.getBookId();
        String comment=modelComment.getComment();
        String uid=modelComment.getUid();
        String timestamp=modelComment.getTimestamp();
        //format date
        String date=MyApplication.formatTimestamp(Long.parseLong(timestamp));
        //set data
        holder.dateTv.setText(date);
        holder.commentTv.setText(comment);

        loadUserDetails(modelComment,holder);
        //handle click,show option to delete comment
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (firebaseAuth.getCurrentUser() != null && uid.equals(firebaseAuth.getUid())){
                    deleteComment(modelComment,holder);
                }
            }
        });


    }

    private void deleteComment(ModelComment modelComment, HolderComment holder) {
        //show confirm dialog before deleting comment
        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setTitle("Xoá bình luận.")
                .setMessage("Bạn có chắc chắn muốn xoá bình luận này không?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Books");
                        reference.child(modelComment.getBookId())
                                .child("Comment")
                                .child(modelComment.getId())
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(context, "Xoá thành công", Toast.LENGTH_SHORT).show();


                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(context,"Không thể xoá bình luận do: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();
    }

    private void loadUserDetails(ModelComment modelComment, HolderComment holder) {
        String uid=modelComment.getUid();

        DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Users");
        reference.child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get data
                        String name=""+snapshot.child("name").getValue();
                        String profileImage=""+snapshot.child("profileImage").getValue();
                        //set data
                        holder.nameTv.setText(name);
                        try {
                            Glide.with(context)
                                    .load(profileImage)
                                    .placeholder(R.drawable.ic_person_gray)
                                    .into(holder.profileIv);
                        }catch (Exception e){
                            holder.profileIv.setImageResource(R.drawable.ic_person_gray);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return commentArrayList.size();
    }

    //view holder class for row_comment.xml
    class HolderComment extends RecyclerView.ViewHolder{
        //ui views of row_comment.xml
        ShapeableImageView profileIv;
        TextView nameTv,dateTv,commentTv;

        public HolderComment(@NonNull View itemView) {
            super(itemView);
            //init ui views
            profileIv=binding.profileIv;
            nameTv=binding.nameTv;
            dateTv=binding.dateTv;
            commentTv=binding.commentTv;
        }
    }
}
