package com.example.appdevelopmentprojectfinal.marketplace;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdevelopmentprojectfinal.R;
import com.example.appdevelopmentprojectfinal.model.Course;
import com.example.appdevelopmentprojectfinal.model.User;
import com.example.appdevelopmentprojectfinal.utils.DataManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class TrendingCoursesFragment extends Fragment implements CourseAdapter.CourseClickListener {

    private static final String TAG = "TrendingCoursesFragment";
    
    private TextView tvWalletBalance;
    private TextView tvSectionTitle;
    private EditText etSearch;
    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private TextView tvEmptyMessage;
    private View loadingView;
    
    private CourseAdapter adapter;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                         Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        return inflater.inflate(R.layout.fragment_marketplace_section, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");
        
        initializeViews(view);
        setupAdapter();
        updateWalletBalance();
        setUpSearch();
        loadTrendingCourses();
    }
    
    private void initializeViews(View view) {
        Log.v(TAG, "Initializing views");
        tvWalletBalance = view.findViewById(R.id.tvWalletBalance);
        tvSectionTitle = view.findViewById(R.id.tvSectionTitle);
        etSearch = view.findViewById(R.id.etSearch);
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        loadingView = view.findViewById(R.id.loadingView);
        
        tvSectionTitle.setText(getString(R.string.trending_courses));
        Log.v(TAG, "Views initialized, section title set to: " + getString(R.string.trending_courses));
    }
    
    private void setupAdapter() {
        Log.v(TAG, "Setting up course adapter");
        adapter = new CourseAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
    }
    
    private void setUpSearch() {
        Log.v(TAG, "Setting up search functionality");
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = etSearch.getText().toString().trim();
            Log.i(TAG, "Search initiated with query: " + query);
            searchCourses(query);
            return true;
        });
    }
    
    private void searchCourses(String query) {
        Log.i(TAG, "Searching courses with query: " + query);
        showLoading(true);
        
        MarketplaceFirestoreManager.getInstance().searchCourses(query, new MarketplaceFirestoreManager.OnCoursesLoadedListener() {
            @Override
            public void onCoursesLoaded(List<Course> courses) {
                Log.i(TAG, "Search completed, found " + courses.size() + " matching courses");
                showLoading(false);
                updateCoursesList(courses);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Search error: " + errorMessage);
                showLoading(false);
                Toast.makeText(getContext(), "Search error: " + errorMessage, Toast.LENGTH_SHORT).show();
                updateCoursesList(new ArrayList<>());
            }
        });
    }
    
    private void loadTrendingCourses() {
        Log.i(TAG, "Loading trending courses");
        showLoading(true);
        
        MarketplaceFirestoreManager.getInstance().loadTrendingCourses(new MarketplaceFirestoreManager.OnCoursesLoadedListener() {
            @Override
            public void onCoursesLoaded(List<Course> courses) {
                Log.i(TAG, "Successfully loaded " + courses.size() + " trending courses");
                showLoading(false);
                updateCoursesList(courses);
                
                // Log some details about the top courses
                if (!courses.isEmpty()) {
                    Course topCourse = courses.get(0);
                    Log.d(TAG, "Top trending course: " + topCourse.getName() + 
                          " (Purchases today: " + 
                          (topCourse.getStatistics() != null ? topCourse.getStatistics().getPurchasesToday() : "N/A") + 
                          ")");
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading trending courses: " + errorMessage);
                showLoading(false);
                Toast.makeText(getContext(), "Failed to load courses: " + errorMessage, Toast.LENGTH_SHORT).show();
                updateCoursesList(new ArrayList<>());
            }
        });
    }
    
    private void showLoading(boolean isLoading) {
        Log.v(TAG, "Setting loading state: " + isLoading);
        if (isLoading) {
            loadingView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        } else {
            loadingView.setVisibility(View.GONE);
        }
    }
    
    private void updateCoursesList(List<Course> courses) {
        Log.d(TAG, "Updating courses list with " + courses.size() + " courses");
        if (courses.isEmpty()) {
            Log.w(TAG, "No courses to display, showing empty state");
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText(getString(R.string.no_trending_courses));
        } else {
            Log.v(TAG, "Displaying courses in RecyclerView");
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            adapter.updateCourses(courses);
        }
    }
    
    private void updateWalletBalance() {
        Log.d(TAG, "Updating wallet balance display");
        User currentUser = DataManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
            format.setCurrency(Currency.getInstance("EUR"));
            double balance = currentUser.getWallet();
            String formattedBalance = format.format(balance);
            String balanceText = getString(R.string.wallet_balance, formattedBalance);
            
            Log.v(TAG, "Wallet balance: " + formattedBalance);
            tvWalletBalance.setText(balanceText);
        } else {
            Log.w(TAG, "Cannot update wallet balance: current user is null");
        }
    }
    
    @Override
    public void onCourseClicked(Course course) {
        Log.i(TAG, "Course clicked: " + course.getName() + " (ID: " + course.getId() + ")");
        
        MarketplaceFirestoreManager.getInstance().updateCourseStatistics(
            course.getId(), 
            1,
            new MarketplaceFirestoreManager.OnCourseOperationListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Course view statistics updated successfully");
                }

                @Override
                public void onError(String errorMessage) {
                    Log.w(TAG, "Failed to update course view statistics: " + errorMessage);
                }
            }
        );
        
        // Show course details dialog
        CourseDetailDialog dialog = CourseDetailDialog.newInstance(course.getId());
        dialog.show(getParentFragmentManager(), "CourseDetail");
        
        // Set callback for purchase completion
        dialog.setPurchaseCompletedListener(() -> {
            Log.i(TAG, "Purchase completed callback received, refreshing data");
            loadTrendingCourses();
            updateWalletBalance();
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        loadTrendingCourses();
        updateWalletBalance();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }
}