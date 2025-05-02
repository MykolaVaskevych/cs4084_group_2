package com.example.appdevelopmentprojectfinal.marketplace;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class AuthoredCoursesFragment extends Fragment implements CourseAdapter.CourseClickListener {

    private static final String TAG = "AuthoredCoursesFragment";
    
    private TextView tvWalletBalance;
    private TextView tvSectionTitle;
    private EditText etSearch;
    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private TextView tvEmptyMessage;
    private View loadingView;
    private FloatingActionButton fabAddCourse;
    
    private CourseAdapter adapter;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                         Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_marketplace_section, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "Initializing AuthoredCoursesFragment");
        
        tvWalletBalance = view.findViewById(R.id.tvWalletBalance);
        tvSectionTitle = view.findViewById(R.id.tvSectionTitle);
        etSearch = view.findViewById(R.id.etSearch);
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        loadingView = view.findViewById(R.id.loadingView);
        
        // Repurpose the sample course creation button
        Button btnCreateCourses = view.findViewById(R.id.btnCreateCourses);
        if (btnCreateCourses != null) {
            btnCreateCourses.setText(R.string.create_new_course);
            btnCreateCourses.setVisibility(View.VISIBLE);
            btnCreateCourses.setOnClickListener(v -> openCourseCreationDialog());
        }
        
        // Add a floating action button for adding new courses
        ViewGroup rootView = (ViewGroup) view.getParent();
        fabAddCourse = new FloatingActionButton(requireContext());
        fabAddCourse.setImageResource(android.R.drawable.ic_input_add);
        fabAddCourse.setContentDescription("Add Course");
        
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 32, 32); // right, bottom margins
        fabAddCourse.setLayoutParams(params);
        
        // Position the FAB at the bottom right
        if (rootView instanceof ViewGroup) {
            fabAddCourse.setUseCompatPadding(true);
            rootView.addView(fabAddCourse);
            fabAddCourse.setOnClickListener(v -> openCourseCreationDialog());
        }
        
        tvSectionTitle.setText(R.string.authored_courses);
        
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
            Toast.makeText(getContext(), "Please login to view your authored courses", Toast.LENGTH_SHORT).show();
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
                loadAuthoredCourses();
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
        
        // Get current user
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            showLoading(false);
            updateCoursesList(new ArrayList<>());
            return;
        }
        
        String authorId = firebaseUser.getUid();
        
        MarketplaceFirestoreManager.getInstance().searchCourses(query, new MarketplaceFirestoreManager.OnCoursesLoadedListener() {
            @Override
            public void onCoursesLoaded(List<Course> allCourses) {
                // Filter to only include courses authored by current user
                List<Course> authoredCourses = new ArrayList<>();
                for (Course course : allCourses) {
                    if (course.getAuthor() != null && course.getAuthor().equals(authorId)) {
                        authoredCourses.add(course);
                    }
                }
                
                showLoading(false);
                updateCoursesList(authoredCourses);
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(getContext(), "Search error: " + errorMessage, Toast.LENGTH_SHORT).show();
                updateCoursesList(new ArrayList<>());
            }
        });
    }
    
    private void loadAuthoredCourses() {
        showLoading(true);
        
        // Get current user
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Log.e(TAG, "Cannot load authored courses: no authenticated user");
            showLoading(false);
            updateCoursesList(new ArrayList<>());
            return;
        }
        
        String authorId = firebaseUser.getUid();
        
        // Load all courses and filter by author
        MarketplaceFirestoreManager.getInstance().loadAuthoredCourses(authorId, new MarketplaceFirestoreManager.OnCoursesLoadedListener() {
            @Override
            public void onCoursesLoaded(List<Course> authoredCourses) {
                showLoading(false);
                Log.i(TAG, "Loaded " + authoredCourses.size() + " authored courses");
                updateCoursesList(authoredCourses);
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading authored courses: " + errorMessage);
                
                showLoading(false);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load your authored courses", Toast.LENGTH_SHORT).show();
                }
                updateCoursesList(new ArrayList<>());
            }
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
            tvEmptyMessage.setText(R.string.no_authored_courses);
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
    
    private void openCourseCreationDialog() {
        // Open dialog for creating/editing a course
        CourseCreationDialog dialog = new CourseCreationDialog();
        dialog.show(getParentFragmentManager(), "CourseCreation");
        dialog.setCourseCreatedListener(() -> {
            // Refresh the list when a course is created or updated
            loadAuthoredCourses();
        });
    }
    
    @Override
    public void onCourseClicked(Course course) {
        // When a course is clicked, open it for editing in the course creation dialog
        CourseCreationDialog dialog = CourseCreationDialog.newInstance(course.getId());
        dialog.show(getParentFragmentManager(), "CourseEdit");
        dialog.setCourseCreatedListener(() -> {
            // Refresh the list when a course is updated
            loadAuthoredCourses();
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "AuthoredCoursesFragment resumed");
        
        // Only refresh data if we already have a user loaded
        if (MarketplaceFirestoreManager.getInstance().getCurrentUser() != null) {
            loadAuthoredCourses();
            updateWalletBalance();
        } else {
            Log.w(TAG, "Cannot refresh courses: no user loaded");
            initializeUserData();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove FAB when fragment is destroyed
        if (fabAddCourse != null && fabAddCourse.getParent() != null) {
            ((ViewGroup) fabAddCourse.getParent()).removeView(fabAddCourse);
        }
    }
    
    /**
     * Public method to force data refresh
     */
    public void refreshData() {
        Log.i(TAG, "Forcing data refresh");
        loadAuthoredCourses();
        updateWalletBalance();
    }
}