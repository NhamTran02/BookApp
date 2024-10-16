package com.example.bookapp.filter;

import android.widget.Filter;

import com.example.bookapp.adapter.AdapterPdfUser;
import com.example.bookapp.model.ModelPdf;
import java.util.ArrayList;

public class FilterPdfUser extends Filter {
    //arraylist in which we want to search
    ArrayList<ModelPdf> filterList;
    //adapter in which filter need to be implement
    AdapterPdfUser adapterPdfUser;

    public FilterPdfUser(ArrayList<ModelPdf> filterList, AdapterPdfUser adapterPdfUser) {
        this.filterList = filterList;
        this.adapterPdfUser = adapterPdfUser;
    }

    @Override
    protected FilterResults performFiltering(CharSequence charSequence) {
        FilterResults results=new FilterResults();
        //value to be searched should not be null/empty
        if (charSequence!=null || charSequence.length()>0){
            //not null nor empty
            //change to uppercase or lower case to avoid case sensitivity
            charSequence=charSequence.toString().toUpperCase();
            ArrayList<ModelPdf> filteredModels=new ArrayList<>();
            for (int i=0;i<filterList.size();i++){
                //validate
                if (filterList.get(i).getTitle().toUpperCase().contains(charSequence)){
                    //search matches, add to list
                    filterList.add(filterList.get(i));
                }
            }
            results.count=filteredModels.size();
            results.values=filteredModels;
        }else {
            //empty or null,make original list/result
            results.count=filterList.size();
            results.values=filterList;
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
        //apply filter changes
        adapterPdfUser.pdfArrayList=(ArrayList<ModelPdf>)filterResults.values;
        //notify changes
        adapterPdfUser.notifyDataSetChanged();
    }
}
