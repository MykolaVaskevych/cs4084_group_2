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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class RecommendedCoursesFragment extends Fragment implements CourseAdapter.CourseClickListener {

    private static final String TAG = "RecommendedCoursesFragment";
    
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
        return inflater.inflate(R.layout.fragment_marketplace_section, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "Initializing RecommendedCoursesFragment");
        
        tvWalletBalance = view.findViewById(R.id.tvWalletBalance);
        tvSectionTitle = view.findViewById(R.id.tvSectionTitle);
        etSearch = view.findViewById(R.id.etSearch);
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        loadingView = view.findViewById(R.id.loadingView);
        
        tvSectionTitle.setText(getString(R.string.recommended_courses));
        
        adapter = new CourseAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
        
        initializeUserData();
    }
    
    private void initializeUserData() {
        Log.i(TAG, "Initializing user data");
        showLoading(true);
        
        User dataManagerUser = null;
        try {
            dataManagerUser = com.example.appdevelopmentprojectfinal.utils.DataManager.getInstance().getCurrentUser();
        } catch (Exception e) {
            Log.e(TAG, "Error accessing DataManager: " + e.getMessage());
        }
        
        // Get user ID from DataManager or use default
        // TODO: remove? or maybe anonimous
        String userId = (dataManagerUser != null) ? dataManagerUser.getEmail() : "default@studentmail.ul.ie";
        
        MarketplaceFirestoreManager.getInstance().loadCurrentUser(userId, new MarketplaceFirestoreManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                Log.i(TAG, "User loaded successfully: " + user.getEmail());
                
                updateWalletBalance();
                setUpSearch();
                loadRecommendedCourses();
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading user: " + errorMessage);
                Toast.makeText(getContext(), "Error loading user data: " + errorMessage, Toast.LENGTH_SHORT).show();
                
                showLoading(false);
                updateCoursesList(new ArrayList<>());
            }
        });
    }
    
    private void setUpSearch() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            searchCourses(etSearch.getText().toString());
            return true;
        });
    }
    
    private void searchCourses(String query) {
        showLoading(true);
        MarketplaceFirestoreManager.getInstance().searchCourses(query, new MarketplaceFirestoreManager.OnCoursesLoadedListener() {
            @Override
            public void onCoursesLoaded(List<Course> courses) {
                showLoading(false);
                updateCoursesList(courses);
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(getContext(), "Search error: " + errorMessage, Toast.LENGTH_SHORT).show();
                updateCoursesList(new ArrayList<>());
            }
        });
    }
    
    private void loadRecommendedCourses() {
        showLoading(true);
        
        MarketplaceFirestoreManager marketplaceManager = MarketplaceFirestoreManager.getInstance();
        User currentUser = marketplaceManager.getCurrentUser();
        
        if (currentUser == null || currentUser.getModules() == null || currentUser.getModules().isEmpty()) {
            Log.w(TAG, "No current user or modules available for recommendations");
            showLoading(false);
            updateCoursesList(new ArrayList<>());
            return;
        }
        
        Log.i(TAG, "Loading recommended courses for user with " + currentUser.getModules().size() + " modules");
        List<String> userModules = currentUser.getModules();
        final List<Course> allRecommendedCourses = new ArrayList<>();
        final Set<String> processedCourseIds = new HashSet<>();
        final AtomicInteger remainingRequests = new AtomicInteger(userModules.size());
        
        for (String moduleCode : userModules) {
            Log.v(TAG, "Fetching recommended courses for module: " + moduleCode);
            marketplaceManager.loadCoursesByModule(moduleCode, new MarketplaceFirestoreManager.OnCoursesLoadedListener() {
                @Override
                public void onCoursesLoaded(List<Course> courses) {
                    Log.d(TAG, "Loaded " + courses.size() + " courses for module " + moduleCode);
                    synchronized (allRecommendedCourses) {
                        for (Course course : courses) {
                            if (!processedCourseIds.contains(course.getId())) {
                                processedCourseIds.add(course.getId());
                                if (!marketplaceManager.userOwnsCourse(course.getId())) {
                                    Log.v(TAG, "Adding recommended course: " + course.getName());
                                    allRecommendedCourses.add(course);
                                } else {
                                    Log.v(TAG, "Skipping already owned course: " + course.getName());
                                }
                            }
                        }
                    }
                    
                    if (remainingRequests.decrementAndGet() == 0) {
                        showLoading(false);
                        Log.d(TAG, "Loaded " + allRecommendedCourses.size() + " recommended courses");
                        updateCoursesList(allRecommendedCourses);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error loading recommended courses for module " + moduleCode + ": " + errorMessage);
                    
                    if (remainingRequests.decrementAndGet() == 0) {
                        showLoading(false);
                        if (allRecommendedCourses.isEmpty()) {
                            Toast.makeText(getContext(), "Failed to load courses", Toast.LENGTH_SHORT).show();
                        }
                        updateCoursesList(allRecommendedCourses);
                    }
                }
            });
        }
    }
    
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            loadingView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        } else {
            loadingView.setVisibility(View.GONE);
        }
    }
    
    private void updateCoursesList(List<Course> courses) {
        if (courses.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText(getString(R.string.no_recommended_courses));
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            adapter.updateCourses(courses);
        }
    }
    
    private void updateWalletBalance() {
        User currentUser = MarketplaceFirestoreManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, String.format("Updating wallet balance display: %.2f", currentUser.getWallet()));
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
            format.setCurrency(Currency.getInstance("EUR"));
            String balanceText = getString(R.string.wallet_balance, format.format(currentUser.getWallet()));
            tvWalletBalance.setText(balanceText);
        } else {
            Log.w(TAG, "Cannot update wallet balance: current user is null");
        }
    }
    
    @Override
    public void onCourseClicked(Course course) {
        CourseDetailDialog dialog = CourseDetailDialog.newInstance(course.getId());
        dialog.show(getParentFragmentManager(), "CourseDetail");
        
        dialog.setPurchaseCompletedListener(() -> {
            loadRecommendedCourses();
            updateWalletBalance();
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "RecommendedCoursesFragment resumed");
        
        // Only refresh data if we already have a user loaded
        if (MarketplaceFirestoreManager.getInstance().getCurrentUser() != null) {
            loadRecommendedCourses();
            updateWalletBalance();
        } else {
            Log.w(TAG, "Cannot refresh courses: no user loaded");
            initializeUserData();
        }
    }
}