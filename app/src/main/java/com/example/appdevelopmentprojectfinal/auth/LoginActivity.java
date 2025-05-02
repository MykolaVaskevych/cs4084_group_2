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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    // UI components
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ProgressBar progressBar;
    
    // Firebase Auth
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Check if user is already signed in
        // Check if authentication is already in progress
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Log.i(TAG, "User already signed in: " + (currentUser.getEmail() != null ? currentUser.getEmail() : "anonymous"));
            // User already signed in, go directly to main activity
            startMainActivity();
            return;
        }
        
        // Show note to enable Email/Password auth if needed
        TextView tvNote = findViewById(R.id.tvNote);
        
        // Check if Email/Password auth is enabled
        firebaseAuth.fetchSignInMethodsForEmail("test@example.com")
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Email/Password auth check completed");
                } else {
                    // If there's an error, it might be because Email/Password auth is not enabled
                    Log.w(TAG, "Email auth might not be enabled", task.getException());
                    if (tvNote != null) {
                        tvNote.setVisibility(View.VISIBLE);
                    }
                }
            });
        
        // Initialize UI components
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progressBar);
        
        // Set up listeners
        btnLogin.setOnClickListener(v -> loginWithEmailPassword());
        tvRegister.setOnClickListener(v -> startRegisterActivity());
    }
    
    /**
     * Login with email and password
     */
    private void loginWithEmailPassword() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        // Validate input
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }
        
        // Show progress
        showLoading(true);
        
        // Attempt to sign in
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Sign in success
                    Log.d(TAG, "signInWithEmail:success");
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        // Check if user has a Firestore document
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("users").document(user.getUid()).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    // User exists in Firestore, proceed as normal
                                    Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                    startMainActivity();
                                } else {
                                    // User doesn't exist in Firestore, they need to register
                                    Toast.makeText(LoginActivity.this, 
                                        "Account needs to be registered. Please register first.", 
                                        Toast.LENGTH_SHORT).show();
                                    // Sign out the user
                                    firebaseAuth.signOut();
                                    showLoading(false);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error checking Firestore document", e);
                                Toast.makeText(LoginActivity.this, 
                                    "Error checking account. Please try again.", 
                                    Toast.LENGTH_SHORT).show();
                                showLoading(false);
                            });
                    }
                } else {
                    // Sign in failed
                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                    Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                    showLoading(false);
                }
            });
    }
    
    
    /**
     * Start the register activity
     */
    private void startRegisterActivity() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
    
    /**
     * Start the main activity
     */
    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        // Use Firebase UID consistently for database paths
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            String email = currentUser.getEmail();
            
            // Always use the UID for database operations
            intent.putExtra("USER_ID", uid);
            Log.d(TAG, "User email: " + (email != null ? email : "anonymous") + ", Using UID: " + uid);
        } else {
            Log.w(TAG, "Starting MainActivity with no user ID");
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
            btnLogin.setEnabled(false);
            tvRegister.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            tvRegister.setEnabled(true);
        }
    }
}