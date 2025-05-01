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

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    // UI components
    private EditText etEmail, etPassword;
    private Button btnLogin, btnLoginAnonymously;
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
        btnLoginAnonymously = findViewById(R.id.btnLoginAnonymously);
        tvRegister = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progressBar);
        
        // Set up listeners
        btnLogin.setOnClickListener(v -> loginWithEmailPassword());
        btnLoginAnonymously.setOnClickListener(v -> loginAnonymously());
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
                        Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                        startMainActivity();
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
     * Login anonymously
     */
    private void loginAnonymously() {
        showLoading(true);
        
        firebaseAuth.signInAnonymously()
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Sign in success
                    Log.d(TAG, "signInAnonymously:success");
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        Toast.makeText(LoginActivity.this, "Signed in anonymously", Toast.LENGTH_SHORT).show();
                        startMainActivity();
                    }
                } else {
                    // Sign in failed
                    Log.w(TAG, "signInAnonymously:failure", task.getException());
                    Toast.makeText(LoginActivity.this, "Anonymous authentication failed: " + task.getException().getMessage(),
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
            btnLoginAnonymously.setEnabled(false);
            tvRegister.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            btnLoginAnonymously.setEnabled(true);
            tvRegister.setEnabled(true);
        }
    }
}