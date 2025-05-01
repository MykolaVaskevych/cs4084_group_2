package com.example.appdevelopmentprojectfinal.auth;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.appdevelopmentprojectfinal.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Handles Firebase Authentication for the marketplace feature
 */
public class FirebaseAuthHandler {
    private static final String TAG = "FirebaseAuthHandler";
    
    private static FirebaseAuthHandler instance;
    private final FirebaseAuth firebaseAuth;
    
    /**
     * Callback interface for auth operations
     */
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String errorMessage);
    }
    
    /**
     * Private constructor for singleton
     */
    private FirebaseAuthHandler() {
        firebaseAuth = FirebaseAuth.getInstance();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized FirebaseAuthHandler getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthHandler();
        }
        return instance;
    }
    
    /**
     * Check if user is signed in
     */
    public boolean isUserSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }
    
    /**
     * Get current user
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }
    
    /**
     * Sign in with email and password
     */
    public void signInWithEmailPassword(String email, String password, final AuthCallback callback) {
        Log.d(TAG, "Signing in with email: " + email);
        
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        Log.d(TAG, "signInWithEmail:success for " + email);
                        callback.onSuccess(user);
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        callback.onError(task.getException() != null ? 
                                task.getException().getMessage() : "Authentication failed");
                    }
                }
            });
    }
    
    /**
     * Sign in anonymously
     */
    public void signInAnonymously(final AuthCallback callback) {
        Log.d(TAG, "Signing in anonymously");
        
        firebaseAuth.signInAnonymously()
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        Log.d(TAG, "signInAnonymously:success");
                        callback.onSuccess(user);
                    } else {
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                        callback.onError(task.getException() != null ? 
                                task.getException().getMessage() : "Anonymous authentication failed");
                    }
                }
            });
    }
    
    /**
     * Create a new user account
     */
    public void createUserWithEmailPassword(String email, String password, final AuthCallback callback) {
        Log.d(TAG, "Creating user account for email: " + email);
        
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        Log.d(TAG, "createUserWithEmail:success for " + email);
                        callback.onSuccess(user);
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        callback.onError(task.getException() != null ? 
                                task.getException().getMessage() : "User creation failed");
                    }
                }
            });
    }
    
    /**
     * Sign out
     */
    public void signOut() {
        firebaseAuth.signOut();
        Log.d(TAG, "User signed out");
    }
}