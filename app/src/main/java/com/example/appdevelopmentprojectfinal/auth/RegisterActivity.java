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
    private EditText etEmail, etPassword, etFirstName, etLastName;
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
        
        // Show progress
        showLoading(true);
        
        // If the user already exists in Firebase Auth but not in Firestore,
        // we need to handle this explicitly (this happens if Firestore collection was deleted)
        
        // First try to sign in with the provided credentials to check if user exists
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(signInTask -> {
                if (signInTask.isSuccessful()) {
                    // User exists in Firebase Auth, but may need Firestore document
                    Log.d(TAG, "User exists in Firebase Auth, re-creating Firestore document");
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        // Re-create user profile in Firestore with provided details
                        createUserProfile(firebaseUser.getUid(), email, firstName, lastName);
                    }
                } else {
                    // User doesn't exist, create a new one
                    Log.d(TAG, "Creating new user account");
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, createTask -> {
                            if (createTask.isSuccessful()) {
                                // Sign in success, create user profile
                                Log.d(TAG, "createUserWithEmail:success");
                                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                                if (firebaseUser != null) {
                                    // Create user profile in Firestore
                                    createUserProfile(firebaseUser.getUid(), email, firstName, lastName);
                                }
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "createUserWithEmail:failure", createTask.getException());
                                Toast.makeText(RegisterActivity.this, 
                                    "Registration failed: " + createTask.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                                showLoading(false);
                            }
                        });
                }
            });
    }
    
    /**
     * Create user profile in Firestore
     */
    private void createUserProfile(String uid, String email, String firstName, String lastName) {
        // Create a new user with default values
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        
        // Default initial wallet balance
        user.setWallet(0.0);
        
        // Initialize empty modules list
        user.setModules(new ArrayList<>());
        
        // Empty owned courses
        user.setOwnedCourses(new ArrayList<>());
        
        // Save to Firestore using the UID instead of email
        firestore.collection("users").document(uid)
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
        
        // Pass user information to MainActivity
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getEmail();
            if (userId == null || userId.isEmpty()) {
                userId = currentUser.getUid();
            }
            
            // Use Firebase UID instead of email for database paths
            String uid = currentUser.getUid();
            
            intent.putExtra("USER_ID", uid);
            Log.d(TAG, "User email: " + userId + ", Using UID: " + uid);
        }
        
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