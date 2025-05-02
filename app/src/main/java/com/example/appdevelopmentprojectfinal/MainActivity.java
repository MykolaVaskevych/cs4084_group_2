package com.example.appdevelopmentprojectfinal;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.appdevelopmentprojectfinal.databinding.ActivityMainBinding;
import com.example.appdevelopmentprojectfinal.timetable.TimetableFragment;
import com.example.appdevelopmentprojectfinal.calendar.CalendarFragment;
//import com.example.appdevelopmentprojectfinal.timetable.TimetableNotificationManager;

import android.util.Log;

import com.example.appdevelopmentprojectfinal.utils.JsonUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import android.widget.Toast;

import com.example.appdevelopmentprojectfinal.model.User;
import com.example.appdevelopmentprojectfinal.utils.AcademicDatabaseManager;
import com.example.appdevelopmentprojectfinal.utils.DataManager;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // First verify Firebase Auth session is valid
        com.google.firebase.auth.FirebaseUser firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            // Clear any error messages if already logged out
            Log.i("MainActivity", "No Firebase user logged in, redirecting to login screen");
            // Redirect to login
            startActivity(new Intent(this, com.example.appdevelopmentprojectfinal.auth.LoginActivity.class));
            finish();
            return;
        }
        
        // Get user ID from intent (from LoginActivity/RegisterActivity)
        String userId = getIntent().getStringExtra("USER_ID");
        
        // If no user ID was provided, use the Firebase user's UID
        if (userId == null || userId.isEmpty()) {
            userId = firebaseUser.getUid();
            Log.d("MainActivity", "Using Firebase UID: " + userId);
        }
        
        // We'll consistently use UIDs for all database operations

        Log.d("MainActivity", "Initializing DataManager with user ID: " + userId);
        
        DataManager.getInstance().initialize(this, userId, new DataManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                Log.d("MainActivity", "User loaded successfully: " + user.getFullName() + " - Wallet: €" + user.getWallet());

                // Initialize the academic database
                AcademicDatabaseManager.getInstance().initializeDatabaseIfNeeded();
                
                // Check if we need to create sample courses
                checkAndCreateSampleCourses();
                
                setupNavigation();
                replaceFragment(new HomepageFragment());
            }

            @Override
            public void onError(String error) {
                Log.e("MainActivity", "Error loading user: " + error);
                Toast.makeText(MainActivity.this, "Error loading user data: " + error, Toast.LENGTH_LONG).show();
                
                // Redirect to login
                startActivity(new Intent(MainActivity.this, com.example.appdevelopmentprojectfinal.auth.LoginActivity.class));
                finish();
            }
        });


        // Create notification channel and load fragments
//        TimetableNotificationManager.createNotificationChannel(this);

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.home) {
                replaceFragment(new HomepageFragment());
            } else if (itemId == R.id.market) {
                replaceFragment(new StoreFragment());
            } else if (itemId == R.id.timetable) {
                replaceFragment(new TimetableFragment());
            } else if (itemId == R.id.calendar) {
                replaceFragment(new CalendarFragment());
            } else if (itemId == R.id.profile) {
                replaceFragment(new ProfileFragment());
            }

            return true;
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }
    private void setupNavigation() {
//        TimetableNotificationManager.createNotificationChannel(this);

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.home) {
                replaceFragment(new HomepageFragment());
            } else if (itemId == R.id.market) {
                replaceFragment(new StoreFragment());
            } else if (itemId == R.id.timetable) {
                replaceFragment(new TimetableFragment());
            } else if (itemId == R.id.calendar) {
                replaceFragment(new CalendarFragment());
            } else if (itemId == R.id.profile) {
                replaceFragment(new ProfileFragment());
            }

            return true;
        });
    }
    
    /**
     * Checks if sample courses exist in Firebase and creates them if needed
     */
    private void checkAndCreateSampleCourses() {
        Log.d("MainActivity", "Creating sample marketplace courses");
        
        // Force creation of sample courses for testing
        Log.i("MainActivity", "Forcibly creating sample marketplace courses in Firebase");
        
        // Delete any existing courses first
        FirebaseFirestore.getInstance().collection("marketplace")
            .get()
            .addOnSuccessListener(snapshot -> {
                Log.d("MainActivity", "Found " + snapshot.size() + " existing marketplace courses - deleting them");
                
                if (snapshot.size() > 0) {
                    // Delete each course document
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        doc.getReference().delete()
                            .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Deleted marketplace course: " + doc.getId()))
                            .addOnFailureListener(e -> Log.e("MainActivity", "Failed to delete marketplace course: " + doc.getId(), e));
                    }
                    
                    // Create new courses after a delay to ensure deletions complete
                    new android.os.Handler().postDelayed(() -> {
                        createMarketplaceCourses();
                    }, 1000); // 1 second delay
                } else {
                    // No courses to delete, create new ones immediately
                    createMarketplaceCourses();
                }
            })
            .addOnFailureListener(e -> {
                Log.e("MainActivity", "Error getting existing marketplace courses: " + e.getMessage(), e);
                
                // Create new sample courses anyway
                createMarketplaceCourses();
            });
    }
    
    
    private void createMarketplaceCourses() {
        Log.i("MainActivity", "Creating new marketplace courses");
        
        // Create new sample courses
        AcademicDatabaseManager.getInstance().createSampleCoursesInFirebase(new AcademicDatabaseManager.OnCourseOperationListener() {
            @Override
            public void onSuccess() {
                Log.i("MainActivity", "Sample marketplace courses created successfully");
                
                // Set the initialization flag
                FirebaseDatabase.getInstance().getReference("course_init_flag").setValue(true)
                    .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Course initialization flag set"))
                    .addOnFailureListener(e -> Log.e("MainActivity", "Failed to set course initialization flag", e));
                
                // Force refresh StoreFragment if it's currently visible
                if (getSupportFragmentManager().findFragmentById(R.id.frame_layout) instanceof StoreFragment) {
                    Log.d("MainActivity", "Refreshing StoreFragment");
                    replaceFragment(new StoreFragment());
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e("MainActivity", "Error creating sample courses: " + errorMessage);
                Toast.makeText(MainActivity.this, "Error creating courses: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

}