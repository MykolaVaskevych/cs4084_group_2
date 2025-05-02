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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class OwnedCoursesFragment extends Fragment implements CourseAdapter.CourseClickListener {

    private static final String TAG = "OwnedCoursesFragment";
    
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
        Log.i(TAG, "Initializing OwnedCoursesFragment");
        
        tvWalletBalance = view.findViewById(R.id.tvWalletBalance);
        tvSectionTitle = view.findViewById(R.id.tvSectionTitle);
        etSearch = view.findViewById(R.id.etSearch);
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        loadingView = view.findViewById(R.id.loadingView);
        
        // Hide any sample course creation button that might be in the layout
        View btnCreateCourses = view.findViewById(R.id.btnCreateCourses);
        if (btnCreateCourses != null) {
            btnCreateCourses.setVisibility(View.GONE);
        }
        
        tvSectionTitle.setText(getString(R.string.my_courses));
        
        adapter = new CourseAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
        
        initializeUserData();
    }
    
    private void initializeUserData() {
        Log.i(TAG, "Initializing user data");
        showLoading(true);
        
        // Get current Firebase user
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Log.e(TAG, "Cannot initialize user data: no authenticated user");
            Toast.makeText(getContext(), "Please login to view your courses", Toast.LENGTH_SHORT).show();
            showLoading(false);
            updateCoursesList(new ArrayList<>());
            return;
        }
        
        String userId = firebaseUser.getUid();
        
        MarketplaceFirestoreManager.getInstance().loadCurrentUser(userId, new MarketplaceFirestoreManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                Log.i(TAG, "User loaded successfully: " + user.getEmail());
                
                updateWalletBalance();
                setUpSearch();
                loadOwnedCourses();
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
        
        // Get current user to check owned courses
        User currentUser = MarketplaceFirestoreManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getOwnedCourses() == null || currentUser.getOwnedCourses().isEmpty()) {
            showLoading(false);
            updateCoursesList(new ArrayList<>());
            return;
        }
        
        MarketplaceFirestoreManager.getInstance().searchCourses(query, new MarketplaceFirestoreManager.OnCoursesLoadedListener() {
            @Override
            public void onCoursesLoaded(List<Course> allCourses) {
                // Filter to only include owned courses
                List<Course> ownedCourses = new ArrayList<>();
                for (Course course : allCourses) {
                    if (MarketplaceFirestoreManager.getInstance().userOwnsCourse(course.getId())) {
                        ownedCourses.add(course);
                    }
                }
                
                showLoading(false);
                updateCoursesList(ownedCourses);
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(getContext(), "Search error: " + errorMessage, Toast.LENGTH_SHORT).show();
                updateCoursesList(new ArrayList<>());
            }
        });
    }
    
    private void loadOwnedCourses() {
        showLoading(true);
        
        // Get fresh user data from Firestore
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Log.e(TAG, "Cannot load owned courses: no authenticated user");
            showLoading(false);
            updateCoursesList(new ArrayList<>());
            return;
        }
        
        // First fetch the latest user data directly from Firestore
        String userId = firebaseUser.getUid();
        Log.d(TAG, "Fetching fresh user data from Firestore with ID: " + userId);
        
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Log.d(TAG, "Fresh user document retrieved from Firestore");
                    
                    // Get ownedCourses list directly from the document
                    List<String> ownedCourseIds = (List<String>) documentSnapshot.get("ownedCourses");
                    
                    if (ownedCourseIds == null || ownedCourseIds.isEmpty()) {
                        Log.w(TAG, "No owned courses found in Firestore document");
                        showLoading(false);
                        updateCoursesList(new ArrayList<>());
                        return;
                    }
                    
                    Log.i(TAG, "Found " + ownedCourseIds.size() + " owned course IDs in Firestore: " + ownedCourseIds);
                    
                    // Now load all courses and filter by owned IDs
                    MarketplaceFirestoreManager.getInstance().loadAllCourses(new MarketplaceFirestoreManager.OnCoursesLoadedListener() {
                        @Override
                        public void onCoursesLoaded(List<Course> allCourses) {
                            // Filter to only include owned courses
                            List<Course> ownedCourses = new ArrayList<>();
                            
                            for (Course course : allCourses) {
                                Log.v(TAG, "Checking if course is owned: " + course.getId());
                                if (ownedCourseIds.contains(course.getId())) {
                                    Log.d(TAG, "Adding owned course to list: " + course.getName() + " (" + course.getId() + ")");
                                    ownedCourses.add(course);
                                }
                            }
                            
                            showLoading(false);
                            Log.d(TAG, "Filtered " + allCourses.size() + " courses to " + ownedCourses.size() + " owned courses");
                            updateCoursesList(ownedCourses);
                            
                            // Update cached user in MarketplaceFirestoreManager
                            User currentUser = MarketplaceFirestoreManager.getInstance().getCurrentUser();
                            if (currentUser != null) {
                                currentUser.setOwnedCourses(ownedCourseIds);
                                Log.d(TAG, "Updated cached user with fresh owned courses list");
                            }
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Error loading courses: " + errorMessage);
                            
                            showLoading(false);
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Failed to load your courses", Toast.LENGTH_SHORT).show();
                            }
                            updateCoursesList(new ArrayList<>());
                        }
                    });
                } else {
                    Log.e(TAG, "User document not found in Firestore");
                    showLoading(false);
                    updateCoursesList(new ArrayList<>());
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching user document: " + e.getMessage());
                showLoading(false);
                updateCoursesList(new ArrayList<>());
            });
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
            tvEmptyMessage.setText(getString(R.string.no_owned_courses));
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            adapter.updateCourses(courses);
        }
    }
    
    private void updateWalletBalance() {
        // Get the Firebase authenticated user
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Log.w(TAG, "Cannot update wallet balance: no authenticated user");
            return;
        }
        
        // Always fetch fresh wallet balance from Firestore
        FirebaseFirestore.getInstance().collection("users").document(firebaseUser.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Double walletBalance = documentSnapshot.getDouble("wallet");
                    if (walletBalance != null) {
                        Log.d(TAG, String.format("Retrieved fresh wallet balance: %.2f", walletBalance));
                        
                        // Update UI with fresh data
                        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
                        format.setCurrency(Currency.getInstance("EUR"));
                        String balanceText = getString(R.string.wallet_balance, format.format(walletBalance));
                        tvWalletBalance.setText(balanceText);
                        
                        // Also update the cached user in MarketplaceFirestoreManager
                        User currentUser = MarketplaceFirestoreManager.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            currentUser.setWallet(walletBalance);
                        }
                    }
                }
            })
            .addOnFailureListener(e -> 
                Log.e(TAG, "Error fetching fresh wallet balance: " + e.getMessage()));
    }
    
    @Override
    public void onCourseClicked(Course course) {
        CourseDetailDialog dialog = CourseDetailDialog.newInstance(course.getId());
        dialog.show(getParentFragmentManager(), "CourseDetail");
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "OwnedCoursesFragment resumed");
        
        // Only refresh data if we already have a user loaded
        if (MarketplaceFirestoreManager.getInstance().getCurrentUser() != null) {
            loadOwnedCourses();
            updateWalletBalance();
        } else {
            Log.w(TAG, "Cannot refresh courses: no user loaded");
            initializeUserData();
        }
    }
    
    /**
     * Public method to force data refresh
     */
    public void refreshData() {
        Log.i(TAG, "Forcing data refresh");
        loadOwnedCourses();
        updateWalletBalance();
    }
}