package com.example.bookapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.MyApplication;
import com.example.bookapp.activity.PdfDetailActivity;
import com.example.bookapp.databinding.RowPdfFavoriteBinding;
import com.example.bookapp.model.ModelPdf;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rajat.pdfviewer.PdfRendererView;

import java.util.ArrayList;

public class AdapterPdfFavorite extends RecyclerView.Adapter<AdapterPdfFavorite.HolderPdfFavorite> {
    private Context context;
    private ArrayList<ModelPdf> pdfArrayList;
    //view binding for row_pdf_favorite
    private RowPdfFavoriteBinding binding;
    private static final String TAG="FAVORITE_BOOK_TAG";

    public AdapterPdfFavorite(Context context, ArrayList<ModelPdf> pdfArrayList) {
        this.context = context;
        this.pdfArrayList = pdfArrayList;
    }

    @NonNull
    @Override
    public HolderPdfFavorite onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //bind/inflate row_pdf_favorite
        binding=RowPdfFavoriteBinding.inflate(LayoutInflater.from(context),parent,false);

        return new HolderPdfFavorite(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderPdfFavorite holder, int position) {
        ModelPdf modelPdf=pdfArrayList.get(position);
        loadBookDetails(modelPdf,holder);
        //handle click, mở trang pdf detail
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(context, PdfDetailActivity.class);
                intent.putExtra("bookId", modelPdf.getId());
                context.startActivity(intent);
            }
        });
        //handle click, remove favorite
        holder.removeFavoriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyApplication.removeFromFarivate(context, modelPdf.getId());
            }
        });

    }

    private void loadBookDetails(ModelPdf modelPdf, HolderPdfFavorite holder) {
        String bookId=modelPdf.getId();
        Log.d(TAG,"loadBookDetails: Book Details of Book ID: "+bookId);

        DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Books");
        reference.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // lấy thông tin sách
                        String bookTitle=""+snapshot.child("title").getValue();
                        String description=""+snapshot.child("description").getValue();
                        String categoryId=""+snapshot.child("categoryId").getValue();
                        String bookUrl=""+snapshot.child("url").getValue();
                        String timestamp=""+snapshot.child("timestamp").getValue();
                        String uid=""+snapshot.child("uid").getValue();
                        String viewsCount=""+snapshot.child("viewsCount").getValue();
                        String dowloadsCount=""+snapshot.child("dowloadsCount").getValue();
                        //set to model
                        modelPdf.setFavorite(true);
                        modelPdf.setTitle(bookTitle);
                        modelPdf.setDescription(description);
                        modelPdf.setCategoryId(categoryId);
                        modelPdf.setUrl(bookUrl);
                        modelPdf.setTimestamp(timestamp);
                        modelPdf.setUid(uid);
                        //format date
                        String date= MyApplication.formatTimestamp(Long.parseLong(timestamp));
                        MyApplication.loadPdfFromUrlSinglePage(""+bookUrl,""+ bookTitle, holder.pdfView, holder.progressBar, context, null);
                        MyApplication.loadPdfSize(""+bookUrl,""+ bookTitle, holder.sizeTv);
                        //set data to views
                        holder.titleTv.setText(bookTitle);
                        holder.descriptionTv.setText(description);
                        holder.dateTv.setText(date);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return pdfArrayList.size();
    }

    //viewHolder class
    class HolderPdfFavorite extends RecyclerView.ViewHolder{
        PdfRendererView pdfView;
        ProgressBar progressBar;
        TextView titleTv,descriptionTv,categoryTv,sizeTv,dateTv;
        ImageButton removeFavoriteBtn;

        public HolderPdfFavorite(@NonNull View itemView) {
            super(itemView);
            //init ui views of row_pdf_favorite
            pdfView=binding.pdfView;
            progressBar=binding.progressBar;
            titleTv=binding.titleTv;
            removeFavoriteBtn=binding.removeFavoriteBtn;
            descriptionTv=binding.descriptionTv;
            categoryTv=binding.categoryTv;
            sizeTv=binding.sizeTv;
            dateTv=binding.dateTv;

        }
    }
}
