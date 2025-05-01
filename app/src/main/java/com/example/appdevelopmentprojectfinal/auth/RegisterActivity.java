package com.example.appdevelopmentprojectfinal.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appdevelopmentprojectfinal.MainActivity;
import com.example.appdevelopmentprojectfinal.R;
import com.example.appdevelopmentprojectfinal.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";

    // UI components
    private EditText etEmail, etPassword, etFirstName, etLastName, etYear, etDepartment, etCourse;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;
    
    // Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        // Initialize Firebase Auth and Firestore
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        
        // Initialize UI components
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etYear = findViewById(R.id.etYear);
        etDepartment = findViewById(R.id.etDepartment);
        etCourse = findViewById(R.id.etCourse);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);
        
        // Set up listeners
        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> finish()); // Return to login screen
    }
    
    /**
     * Register a new user
     */
    private void registerUser() {
        // Get input values
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String yearStr = etYear.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String course = etCourse.getText().toString().trim();
        
        // Validate input
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }
        
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }
        
        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("First name is required");
            return;
        }
        
        if (TextUtils.isEmpty(lastName)) {
            etLastName.setError("Last name is required");
            return;
        }
        
        if (TextUtils.isEmpty(yearStr)) {
            etYear.setError("Year is required");
            return;
        }
        
        int year;
        try {
            year = Integer.parseInt(yearStr);
            if (year < 1 || year > 4) {
                etYear.setError("Year must be between 1 and 4");
                return;
            }
        } catch (NumberFormatException e) {
            etYear.setError("Year must be a number");
            return;
        }
        
        if (TextUtils.isEmpty(department)) {
            etDepartment.setError("Department is required");
            return;
        }

        if (TextUtils.isEmpty(course)) {
            etCourse.setError("Course is required");
        }
        
        // Show progress
        showLoading(true);
        
        // Create user with email and password
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Sign in success, create user profile
                    Log.d(TAG, "createUserWithEmail:success");
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        // Create user profile in Firestore
                        createUserProfile(firebaseUser.getUid(), email, firstName, lastName, year, department, course);
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                    Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                    showLoading(false);
                }
            });
    }
    
    /**
     * Create user profile in Firestore
     */
    private void createUserProfile(String uid, String email, String firstName, String lastName, int year, String department, String course) {
        // Create a new user with default values
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setYear(year);
        user.setDepartment(department);
        user.setCourse(course);
        
        // Default initial wallet balance
        user.setWallet(0.0);
        
        // Default modules for CS students
        List<String> defaultModules = Arrays.asList("CS4084", "CS4106", "CS4116", "CS4187", "CS4457");
        user.setModules(defaultModules);
        
        // Empty owned courses
        user.setOwnedCourses(new ArrayList<>());
        
        // Save to Firestore
        firestore.collection("users").document(email)
            .set(user)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User profile created for: " + email);
                Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                startMainActivity();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating user profile: " + e.getMessage(), e);
                Toast.makeText(RegisterActivity.this, "Failed to create user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                showLoading(false);
            });
    }
    
    /**
     * Start the main activity
     */
    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    /**
     * Show or hide loading
     */
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnRegister.setEnabled(false);
            tvLogin.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnRegister.setEnabled(true);
            tvLogin.setEnabled(true);
        }
    }
}