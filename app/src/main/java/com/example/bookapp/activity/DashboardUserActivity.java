package com.example.bookapp.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.bookapp.BooksUserFragment;
import com.example.bookapp.databinding.ActivityDashboardUserBinding;
import com.example.bookapp.model.ModelCategory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class DashboardUserActivity extends AppCompatActivity {
    //to show in tabs
    public ArrayList<ModelCategory> categoryArrayList=new ArrayList<>();
    public ViewPagerAdapter viewPagerAdapter;

    //view binding
    private ActivityDashboardUserBinding binding;
    //firebase auth
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityDashboardUserBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        //init firebase auth
        firebaseAuth= FirebaseAuth.getInstance();
        checkUser();

        setupViewPagerAdapter(binding.viewPager);
        binding.tabLayout.setupWithViewPager(binding.viewPager);

        //handle click, logout
        binding.logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseAuth.signOut();
                startActivity(new Intent(DashboardUserActivity.this,MainActivity.class));
                finish();
            }
        });
        //handle click, open profile
        binding.profileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DashboardUserActivity.this,ProfileActivity.class));

            }
        });
    }

    private void setupViewPagerAdapter(ViewPager viewPager){
        viewPagerAdapter=new ViewPagerAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, this);

        categoryArrayList=new ArrayList<>();
        //load categories from firebase
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Categories");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //clear before adding to list
                categoryArrayList.clear();
                //load categories - static e.g. all, most viewed, most dowload
                //add data to models
                ModelCategory modelAll=new ModelCategory("01","All","1","");
                ModelCategory modelMostViews=new ModelCategory("02","Top 10 Viewed","1","");
                ModelCategory modelMostDowloaded=new ModelCategory("03","Top 10 Dowloaded","1","");
                //add models to list
                categoryArrayList.add(modelAll);
                categoryArrayList.add(modelMostViews);
                categoryArrayList.add(modelMostDowloaded);
                //add data to view pager adapter
                viewPagerAdapter.addFragment(BooksUserFragment.newInstance(
                        ""+modelAll.getId(),
                        ""+modelAll.getCategory(),
                        ""+modelAll.getUid())
                , modelAll.getCategory());
                viewPagerAdapter.addFragment(BooksUserFragment.newInstance(
                        ""+modelMostViews.getId(),
                        ""+modelMostViews.getCategory(),
                        ""+modelMostViews.getUid())
                , modelMostViews.getCategory());
                viewPagerAdapter.addFragment(BooksUserFragment.newInstance(
                         ""+modelMostDowloaded.getId(),
                         ""+modelMostDowloaded.getCategory(),
                         ""+modelMostDowloaded.getUid())
                , modelMostDowloaded.getCategory());
                //refresh list
                viewPagerAdapter.notifyDataSetChanged();
                //Now Load from firebase
                for (DataSnapshot ds:snapshot.getChildren()){
                    ModelCategory modelCategory=ds.getValue(ModelCategory.class);
                    categoryArrayList.add(modelCategory);
                    //add data to viewPagerAdapter
                    viewPagerAdapter.addFragment(BooksUserFragment.newInstance(
                            ""+modelCategory.getId(),
                            ""+modelCategory.getCategory(),
                            ""+modelCategory.getUid()),
                            modelCategory.getCategory());
                    //refresh list
                    viewPagerAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        //set adapter to view pager
        viewPager.setAdapter(viewPagerAdapter);
    }

    public class ViewPagerAdapter extends FragmentPagerAdapter{
        private ArrayList<BooksUserFragment> fragmentList=new ArrayList<>();
        private ArrayList<String> fragmentTitleList=new ArrayList<>();
        private Context context;
        public ViewPagerAdapter(FragmentManager fm, int behavior,Context context) {
            super(fm, behavior);
            this.context=context;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        private void addFragment(BooksUserFragment fragment, String title){
            //add fragment passed as parameter in fragmentList
            fragmentList.add(fragment);
            //add title passed as parameter in fragmentTitleList
            fragmentTitleList.add(title);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentTitleList.get(position);
        }
    }

    private void checkUser() {
        //get current user
        FirebaseUser firebaseUser=firebaseAuth.getCurrentUser();
        if (firebaseUser== null){
            //not logged in
            binding.subTitleTv.setText("Not Logged In");
        }else {
            //logged in, get user info
            String email=firebaseUser.getEmail();
            //set in textview of toolbar
            binding.subTitleTv.setText(email);
        }
    }
}