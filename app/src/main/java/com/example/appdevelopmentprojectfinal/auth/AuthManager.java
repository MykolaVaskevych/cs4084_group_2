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

public class AuthManager {
    private static final String TAG = "AuthManager";
    private static final String COLLECTION_USERS = "users";
    
    private static AuthManager instance;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final AcademicDatabaseManager academicDbManager;
    
    private User currentUser;
    
    public interface UserDataListener {
        void onUserDataLoaded(User user);
        void onUserDataError(String errorMessage);
    }
    
    private AuthManager() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        academicDbManager = AcademicDatabaseManager.getInstance();
        
        academicDbManager.initializeDatabaseIfNeeded();
    }
    
    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }
    
    public FirebaseUser getCurrentFirebaseUser() {
        return firebaseAuth.getCurrentUser();
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public boolean isUserSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }
    
    public void loadUserData(final UserDataListener listener) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            if (listener != null) {
                listener.onUserDataError("No authenticated user");
            }
            return;
        }
        
        String uid = firebaseUser.getUid();
        
        Log.d(TAG, "Loading user data for UID: " + uid);
        firestore.collection(COLLECTION_USERS).document(uid)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
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
                    Log.e(TAG, "Firebase Auth user exists but Firestore document is missing");
                    if (listener != null) {
                        listener.onUserDataError("Account incomplete. Please register first.");
                        firebaseAuth.signOut();
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error loading user data", e);
                if (listener != null) {
                    listener.onUserDataError("Error loading user data: " + 
                            (e != null ? e.getMessage() : "Unknown error"));
                }
            });
    }
    
    private void createNewUserProfile(String uid, String email, final UserDataListener listener) {
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
        
        // Save to Firestore using UID as document ID
        Log.d(TAG, "Creating new user profile with UID: " + uid + " and email: " + email);
        firestore.collection(COLLECTION_USERS).document(uid)
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
     * Sign out current user
     */
    public void signOut() {
        firebaseAuth.signOut();
        currentUser = null;
    }
}