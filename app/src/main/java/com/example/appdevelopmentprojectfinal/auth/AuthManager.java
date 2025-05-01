package com.example.appdevelopmentprojectfinal.auth;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.appdevelopmentprojectfinal.model.User;
import com.example.appdevelopmentprojectfinal.utils.AcademicDatabaseManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Centralized authentication manager for Firebase Auth and user data
 */
public class AuthManager {
    private static final String TAG = "AuthManager";
    private static final String COLLECTION_USERS = "users";
    
    private static AuthManager instance;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final AcademicDatabaseManager academicDbManager;
    
    private User currentUser;
    
    /**
     * Interface for user data listeners
     */
    public interface UserDataListener {
        void onUserDataLoaded(User user);
        void onUserDataError(String errorMessage);
    }
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private AuthManager() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        academicDbManager = AcademicDatabaseManager.getInstance();
        
        // Initialize academic database
        academicDbManager.initializeDatabaseIfNeeded();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }
    
    /**
     * Get current Firebase user
     */
    public FirebaseUser getCurrentFirebaseUser() {
        return firebaseAuth.getCurrentUser();
    }
    
    /**
     * Get current application user
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Check if user is signed in
     */
    public boolean isUserSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }
    
    /**
     * Load user data for current authenticated user
     */
    public void loadUserData(final UserDataListener listener) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            if (listener != null) {
                listener.onUserDataError("No authenticated user");
            }
            return;
        }
        
        // For anonymous users, create a temporary profile
        if (firebaseUser.isAnonymous()) {
            Log.d(TAG, "Creating temporary profile for anonymous user");
            User anonymousUser = createAnonymousUserProfile(firebaseUser.getUid());
            currentUser = anonymousUser;
            if (listener != null) {
                listener.onUserDataLoaded(anonymousUser);
            }
            return;
        }
        
        // For email users, load from Firestore
        String email = firebaseUser.getEmail();
        if (email == null) {
            if (listener != null) {
                listener.onUserDataError("User email is null");
            }
            return;
        }
        
        Log.d(TAG, "Loading user data for: " + email);
        firestore.collection(COLLECTION_USERS).document(email)
            .get()
            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                Log.d(TAG, "User data loaded: " + user.getEmail());
                                currentUser = user;
                                if (listener != null) {
                                    listener.onUserDataLoaded(user);
                                }
                            } else {
                                Log.w(TAG, "Error converting user data");
                                if (listener != null) {
                                    listener.onUserDataError("Error converting user data");
                                }
                            }
                        } else {
                            Log.d(TAG, "User document doesn't exist, creating new profile");
                            createNewUserProfile(email, listener);
                        }
                    } else {
                        Log.w(TAG, "Error loading user data", task.getException());
                        if (listener != null) {
                            listener.onUserDataError("Error loading user data: " + 
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                        }
                    }
                }
            });
    }
    
    /**
     * Create a new user profile if one doesn't exist
     */
    private void createNewUserProfile(String email, final UserDataListener listener) {
        User newUser = new User();
        newUser.setEmail(email);
        
        // Extract name from email (before @)
        String name = email.split("@")[0];
        newUser.setFirstName(name);
        newUser.setLastName("");
        
        // Default values
        newUser.setYear(1);
        newUser.setDepartment("Computer Science & Information Systems");
        newUser.setCourse("LM051");
        newUser.setWallet(1000.0);
        
        // Default modules
        List<String> defaultModules = Arrays.asList("CS4084", "CS4106", "CS4116", "CS4187", "CS4457");
        newUser.setModules(defaultModules);
        
        // Empty owned courses
        newUser.setOwnedCourses(new ArrayList<>());
        
        // Save to Firestore
        firestore.collection(COLLECTION_USERS).document(email)
            .set(newUser)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User profile created for: " + email);
                currentUser = newUser;
                if (listener != null) {
                    listener.onUserDataLoaded(newUser);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating user profile", e);
                if (listener != null) {
                    listener.onUserDataError("Error creating user profile: " + e.getMessage());
                }
            });
    }
    
    /**
     * Create a temporary profile for anonymous users
     */
    private User createAnonymousUserProfile(String uid) {
        User anonymousUser = new User();
        anonymousUser.setEmail("anonymous_" + uid);
        anonymousUser.setFirstName("Guest");
        anonymousUser.setLastName("User");
        anonymousUser.setYear(1);
        anonymousUser.setDepartment("Guest");
        anonymousUser.setCourse("Guest");
        anonymousUser.setWallet(1000.0);
        
        // Default modules for guests
        List<String> defaultModules = Arrays.asList("CS4084", "CS4106");
        anonymousUser.setModules(defaultModules);
        
        // Empty owned courses
        anonymousUser.setOwnedCourses(new ArrayList<>());
        
        return anonymousUser;
    }
    
    /**
     * Sign out current user
     */
    public void signOut() {
        firebaseAuth.signOut();
        currentUser = null;
    }
}