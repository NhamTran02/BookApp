package com.example.bookapp.adapter;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.MyApplication;
import com.example.bookapp.activity.PdfDetailActivity;
import com.example.bookapp.activity.PdfEditActivity;
import com.example.bookapp.databinding.RowPdfAdminBinding;
import com.example.bookapp.filter.FilterPdfAdmin;
import com.example.bookapp.model.ModelPdf;
import com.rajat.pdfviewer.PdfRendererView;

import java.util.ArrayList;

public class AdapterPdfAdmin extends RecyclerView.Adapter<AdapterPdfAdmin.HolderPdfAdmin> implements Filterable {
    //context
    private Context context;
    //arrayList to hold list of data of type modelPdf
    public ArrayList<ModelPdf> pdfArrayList,filterList;

    private RowPdfAdminBinding binding;
    private FilterPdfAdmin filter;
    private static final String TAG="PDF_ADAPTER_TAG";
    //progress
    private ProgressDialog progressDialog;

    public AdapterPdfAdmin(Context context, ArrayList<ModelPdf> pdfArrayList) {
        this.context = context;
        this.pdfArrayList = pdfArrayList;
        this.filterList=pdfArrayList;

        //init progress dialog
        progressDialog=new ProgressDialog(context);
        progressDialog.setTitle("please wait");
        progressDialog.setCanceledOnTouchOutside(false);
    }

    @NonNull
    @Override
    public HolderPdfAdmin onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //bind layout using view binding
        binding=RowPdfAdminBinding.inflate(LayoutInflater.from(context),parent,false);

        return new HolderPdfAdmin(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderPdfAdmin holder, int position) {
        //get data
        ModelPdf model=pdfArrayList.get(position);
        String pdfId=model.getId();
        String categoryId=model.getCategoryId();
        String title=model.getTitle();
        String description=model.getDescription();
        String pdfUrl=model.getUrl();
        String timestamp=model.getTimestamp();
        //we need to convert timestamp to dd/MM/yyyy format
        String formattedDate= MyApplication.formatTimestamp(Long.parseLong(timestamp));
        //set data
        holder.titleTv.setText(title);
        holder.descriptionTv.setText(description);
        holder.dateTv.setText(formattedDate);
        //load further details like category, pdf from url, pdf size in seprate functions
        MyApplication.loadCategory(""+categoryId, holder.categoryTv);
        MyApplication.loadPdfFromUrlSinglePage(""+pdfUrl, ""+title,holder.pdfRendererView,holder.progressBar , context,null);
        MyApplication.loadPdfSize(""+pdfUrl, ""+title, holder.sizeTv);

        //handle click, show dialog with option 1.edit 2.delete
        holder.moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moreOptionsDialog(model,holder);
            }
        });
        //handle click,open pdf details page, pass pdf/book id to get details of it
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(context, PdfDetailActivity.class);
                intent.putExtra("bookId", pdfId);
                context.startActivity(intent);
            }
        });
    }

    private void moreOptionsDialog(ModelPdf model, HolderPdfAdmin holder) {
        String bookId=model.getId();
        String bookUrl= model.getUrl();
        String bookTitle= model.getTitle();
        //options to show in dialog
        String[] options={"Edit","Delete"};
        //alert dialog
        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setTitle("Choose Options")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //handle dialog option click
                        if (i==0){
                            //edit clicked,open new activity to edit the book info
                            Intent intent=new Intent(context, PdfEditActivity.class);
                            intent.putExtra("bookId", bookId);
                            context.startActivity(intent);

                        }
                        else if (i == 1){
                            //delete clicked
                            MyApplication.deleteBook(context, ""+bookId, ""+bookUrl, ""+bookTitle);
                        }
                    }
                }).show();
    }

    @Override
    public int getItemCount() {
        return pdfArrayList.size();
    }

    @Override
    public Filter getFilter() {
        if (filter==null){
            filter=new FilterPdfAdmin(filterList, this);

        }
        return filter;
    }

    //view holder class for row_pdf_admin.xml
    class HolderPdfAdmin extends RecyclerView.ViewHolder{
        //UI view of row_pdf_admin.pdf
        PdfRendererView pdfRendererView;
        ProgressBar progressBar;
        TextView titleTv,descriptionTv,categoryTv,sizeTv,dateTv;
        ImageButton moreBtn;
        public HolderPdfAdmin(@NonNull View itemView) {
            super(itemView);
            //init ui views
            pdfRendererView =binding.pdfView;
            progressBar =binding.progressBar;
            titleTv=binding.titleTv;
            descriptionTv=binding.descriptionTv;
            categoryTv=binding.categoryTv;
            sizeTv=binding.sizeTv;
            dateTv=binding.dateTv;
            moreBtn=binding.moreBtn;

        }
    }
}
