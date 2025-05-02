package com.example.appdevelopmentprojectfinal.marketplace;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.appdevelopmentprojectfinal.model.Course;
import com.example.appdevelopmentprojectfinal.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

import com.example.appdevelopmentprojectfinal.auth.FirebaseAuthHandler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarketplaceFirestoreManager {
    private static final String TAG = "MarketplaceFirestore";
    private static final String COLLECTION_COURSES = "marketplace";
    private static final String COLLECTION_USERS = "users";
    
    private FirebaseFirestore db;
    private CollectionReference coursesCollection;
    private CollectionReference usersCollection;
    
    private static MarketplaceFirestoreManager instance;
    private String currentUserId;
    private User currentUser;
    
    private MarketplaceFirestoreManager() {
        Log.d(TAG, "Initializing MarketplaceFirestoreManager");
        db = FirebaseFirestore.getInstance();
        coursesCollection = db.collection(COLLECTION_COURSES);
        usersCollection = db.collection(COLLECTION_USERS);
    }
    
    public static synchronized MarketplaceFirestoreManager getInstance() {
        if (instance == null) {
            Log.d(TAG, "Creating new MarketplaceFirestoreManager instance");
            instance = new MarketplaceFirestoreManager();
        }
        return instance;
    }
    
    public void loadAllCourses(final OnCoursesLoadedListener listener) {
        Log.i(TAG, "Loading all courses from Firestore");
        long startTime = System.currentTimeMillis();
        
        coursesCollection
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Course> courses = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Course course = document.toObject(Course.class);
                    if (course != null) {
                        course.setId(document.getId());
                        courses.add(course);
                        Log.v(TAG, "Loaded course: " + course.getId() + " - " + course.getName());
                    } else {
                        Log.w(TAG, "Failed to parse course document: " + document.getId());
                    }
                }
                
                long endTime = System.currentTimeMillis();
                Log.i(TAG, "Loaded " + courses.size() + " courses in " + (endTime - startTime) + "ms");
                
                if (listener != null) {
                    listener.onCoursesLoaded(courses);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading courses: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            });
    }
    
    public void loadTrendingCourses(final OnCoursesLoadedListener listener) {
        Log.i(TAG, "Loading trending courses");
        long startTime = System.currentTimeMillis();
        
        coursesCollection
            .orderBy("statistics.purchasesToday", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Course> courses = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Course course = document.toObject(Course.class);
                    if (course != null) {
                        course.setId(document.getId());
                        courses.add(course);
                        Log.v(TAG, "Loaded trending course: " + course.getId() + " - " + course.getName());
                    }
                }
                
                long endTime = System.currentTimeMillis();
                Log.i(TAG, "Loaded " + courses.size() + " trending courses in " + (endTime - startTime) + "ms");
                
                if (listener != null) {
                    listener.onCoursesLoaded(courses);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading trending courses: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            });
    }
    
    public void loadCoursesByModule(String moduleCode, final OnCoursesLoadedListener listener) {
        if (moduleCode == null || moduleCode.isEmpty()) {
            Log.e(TAG, "Cannot load courses: moduleCode is null or empty");
            if (listener != null) {
                listener.onError("Invalid module code");
            }
            return;
        }
        
        Log.i(TAG, "Loading courses for module: " + moduleCode);
        long startTime = System.currentTimeMillis();
        
        coursesCollection
            .whereEqualTo("relatedModule", moduleCode)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Course> courses = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Course course = document.toObject(Course.class);
                    if (course != null) {
                        course.setId(document.getId());
                        courses.add(course);
                        Log.v(TAG, "Loaded module course: " + course.getId() + " - " + course.getName());
                    }
                }
                
                long endTime = System.currentTimeMillis();
                Log.i(TAG, "Loaded " + courses.size() + " courses for module " + moduleCode + " in " + (endTime - startTime) + "ms");
                
                if (listener != null) {
                    listener.onCoursesLoaded(courses);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading courses for module " + moduleCode + ": " + e.getMessage(), e);
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            });
    }
    
    public void loadCourseById(String courseId, final OnCourseLoadedListener listener) {
        if (courseId == null || courseId.isEmpty()) {
            Log.e(TAG, "Cannot load course: courseId is null or empty");
            if (listener != null) {
                listener.onError("Invalid course ID");
            }
            return;
        }
        
        Log.i(TAG, "Loading course by ID: " + courseId);
        long startTime = System.currentTimeMillis();
        
        coursesCollection.document(courseId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Course course = documentSnapshot.toObject(Course.class);
                    if (course != null) {
                        course.setId(documentSnapshot.getId());
                        
                        long endTime = System.currentTimeMillis();
                        Log.i(TAG, "Loaded course " + course.getName() + " in " + (endTime - startTime) + "ms");
                        
                        if (listener != null) {
                            listener.onCourseLoaded(course);
                        }
                    } else {
                        Log.e(TAG, "Error parsing course data from document: " + courseId);
                        if (listener != null) {
                            listener.onError("Error parsing course data");
                        }
                    }
                } else {
                    Log.w(TAG, "Course not found with ID: " + courseId);
                    if (listener != null) {
                        listener.onError("Course not found");
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading course by ID: " + courseId + " - " + e.getMessage(), e);
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            });
    }
    
    public void searchCourses(String query, final OnCoursesLoadedListener listener) {
        if (query == null || query.isEmpty()) {
            loadAllCourses(listener);
            return;
        }
        
        Log.i(TAG, "Searching courses with query: " + query);
        long startTime = System.currentTimeMillis();
        String searchLower = query.toLowerCase();
        
        coursesCollection
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Course> matchingCourses = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Course course = document.toObject(Course.class);
                    if (course != null) {
                        course.setId(document.getId());
                        
                        // Check if course matches search query
                        boolean matches = false;
                        
                        if (course.getName() != null && course.getName().toLowerCase().contains(searchLower)) {
                            matches = true;
                            Log.v(TAG, "Course matches by name: " + course.getName());
                        } else if (course.getDescription() != null && course.getDescription().toLowerCase().contains(searchLower)) {
                            matches = true;
                            Log.v(TAG, "Course matches by description: " + course.getId());
                        } else if (course.getAuthor() != null && course.getAuthor().toLowerCase().contains(searchLower)) {
                            matches = true;
                            Log.v(TAG, "Course matches by author: " + course.getAuthor());
                        } else if (course.getTags() != null) {
                            for (String tag : course.getTags()) {
                                if (tag.toLowerCase().contains(searchLower)) {
                                    matches = true;
                                    Log.v(TAG, "Course matches by tag: " + tag);
                                    break;
                                }
                            }
                        }
                        
                        if (matches) {
                            matchingCourses.add(course);
                        }
                    }
                }
                
                long endTime = System.currentTimeMillis();
                Log.i(TAG, "Found " + matchingCourses.size() + " courses matching query: " + query + " in " + (endTime - startTime) + "ms");
                
                if (listener != null) {
                    listener.onCoursesLoaded(matchingCourses);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error searching courses: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            });
    }
    
    public void getUserById(String userId, final OnUserLoadedListener listener) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot load user: userId is null or empty");
            if (listener != null) {
                listener.onError("Invalid user ID");
            }
            return;
        }
        
        Log.i(TAG, "Loading user by ID: " + userId);
        long startTime = System.currentTimeMillis();
        
        usersCollection.document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        long endTime = System.currentTimeMillis();
                        Log.i(TAG, "Loaded user " + userId + " in " + (endTime - startTime) + "ms");
                        
                        if (listener != null) {
                            listener.onUserLoaded(user);
                        }
                    } else {
                        Log.e(TAG, "Error parsing user data from document: " + userId);
                        if (listener != null) {
                            listener.onError("Error parsing user data");
                        }
                    }
                } else {
                    Log.w(TAG, "User not found with ID: " + userId);
                    if (listener != null) {
                        listener.onError("User not found");
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading user by ID: " + userId + " - " + e.getMessage(), e);
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            });
    }
    
    public void purchaseCourse(String userId, String courseId, double price, final OnPurchaseListener listener) {
        if (userId == null || userId.isEmpty() || courseId == null || courseId.isEmpty()) {
            Log.e(TAG, "Cannot complete purchase: invalid user ID or course ID");
            if (listener != null) {
                listener.onError("Invalid user ID or course ID");
            }
            return;
        }
        
        Log.i(TAG, String.format("Processing purchase: userId=%s, courseId=%s, price=%.2f", userId, courseId, price));
        long startTime = System.currentTimeMillis();
        
        // Need to run this as a transaction to ensure both the user update and course statistics update succeed or fail together
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // Get user document
            DocumentReference userRef = usersCollection.document(userId);
            DocumentSnapshot userSnapshot = transaction.get(userRef);
            
            if (!userSnapshot.exists()) {
                Log.e(TAG, "Transaction failed: User not found");
                throw new RuntimeException("User not found");
            }
            
            User user = userSnapshot.toObject(User.class);
            if (user == null) {
                Log.e(TAG, "Transaction failed: Invalid user data");
                throw new RuntimeException("Invalid user data");
            }
            
            // Check if user already owns the course
            if (user.getOwnedCourses() != null && user.getOwnedCourses().contains(courseId)) {
                Log.e(TAG, "Transaction failed: User already owns this course");
                throw new RuntimeException("User already owns this course");
            }
            
            // Check if user has enough funds
            if (user.getWallet() < price) {
                Log.e(TAG, "Transaction failed: Insufficient funds");
                throw new RuntimeException("Insufficient funds");
            }
            
            // Check if course exists
            DocumentReference courseRef = coursesCollection.document(courseId);
            DocumentSnapshot courseSnapshot = transaction.get(courseRef);
            
            if (!courseSnapshot.exists()) {
                Log.e(TAG, "Transaction failed: Course not found");
                throw new RuntimeException("Course not found");
            }
            
            Course course = courseSnapshot.toObject(Course.class);
            if (course == null) {
                Log.e(TAG, "Transaction failed: Invalid course data");
                throw new RuntimeException("Invalid course data");
            }
            
            // Calculate new balance
            double newBalance = user.getWallet() - price;
            
            // Update user wallet and owned courses
            Map<String, Object> userUpdates = new HashMap<>();
            userUpdates.put("wallet", newBalance);
            
            List<String> ownedCourses = user.getOwnedCourses();
            if (ownedCourses == null) {
                ownedCourses = new ArrayList<>();
            }
            ownedCourses.add(courseId);
            userUpdates.put("ownedCourses", ownedCourses);
            
            transaction.update(userRef, userUpdates);
            
            // Update course purchase statistics
            Map<String, Object> statsUpdates = new HashMap<>();
            if (course.getStatistics() != null) {
                statsUpdates.put("statistics.purchasesToday", course.getStatistics().getPurchasesToday() + 1);
                statsUpdates.put("statistics.totalPurchases", course.getStatistics().getTotalPurchases() + 1);
            } else {
                Map<String, Object> statistics = new HashMap<>();
                statistics.put("purchasesToday", 1);
                statistics.put("totalPurchases", 1);
                statistics.put("viewsToday", 0);
                statsUpdates.put("statistics", statistics);
            }
            
            transaction.update(courseRef, statsUpdates);
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            long endTime = System.currentTimeMillis();
            Log.i(TAG, "Purchase completed successfully in " + (endTime - startTime) + "ms");
            
            if (listener != null) {
                listener.onPurchaseSuccess();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Purchase failed: " + e.getMessage(), e);
            
            if (listener != null) {
                listener.onError(e.getMessage());
            }
        });
    }
    
    public void saveCourse(Course course, final OnCourseOperationListener listener) {
        if (course == null) {
            Log.e(TAG, "Cannot save course: course is null");
            if (listener != null) {
                listener.onError("Invalid course data");
            }
            return;
        }
        
        Log.i(TAG, "Saving course: " + (course.getId() != null ? course.getId() : "new course"));
        long startTime = System.currentTimeMillis();
        
        // If no ID, create a new one
        if (course.getId() == null || course.getId().isEmpty()) {
            String newId = coursesCollection.document().getId();
            course.setId(newId);
            Log.d(TAG, "Created new course ID: " + newId);
        }
        
        // Save to Firestore
        coursesCollection.document(course.getId())
            .set(course)
            .addOnSuccessListener(aVoid -> {
                long endTime = System.currentTimeMillis();
                Log.i(TAG, "Course saved successfully in " + (endTime - startTime) + "ms");
                
                if (listener != null) {
                    listener.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error saving course: " + e.getMessage(), e);
                
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            });
    }
    
    public void updateCourseStatistics(String courseId, int viewIncrement, final OnCourseOperationListener listener) {
        if (courseId == null || courseId.isEmpty()) {
            Log.e(TAG, "Cannot update course statistics: courseId is null or empty");
            if (listener != null) {
                listener.onError("Invalid course ID");
            }
            return;
        }
        
        Log.i(TAG, "Updating view statistics for course: " + courseId + " (+" + viewIncrement + " views)");
        long startTime = System.currentTimeMillis();
        
        DocumentReference courseRef = coursesCollection.document(courseId);
        
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(courseRef);
            
            if (!snapshot.exists()) {
                Log.e(TAG, "Transaction failed: Course not found");
                throw new RuntimeException("Course not found");
            }
            
            Course course = snapshot.toObject(Course.class);
            if (course == null) {
                Log.e(TAG, "Transaction failed: Invalid course data");
                throw new RuntimeException("Invalid course data");
            }
            
            // Update course statistics
            Map<String, Object> statsUpdates = new HashMap<>();
            if (course.getStatistics() != null) {
                statsUpdates.put("statistics.viewsToday", course.getStatistics().getViewsToday() + viewIncrement);
            } else {
                Map<String, Object> statistics = new HashMap<>();
                statistics.put("purchasesToday", 0);
                statistics.put("totalPurchases", 0);
                statistics.put("viewsToday", viewIncrement);
                statsUpdates.put("statistics", statistics);
            }
            
            transaction.update(courseRef, statsUpdates);
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            long endTime = System.currentTimeMillis();
            Log.i(TAG, "Course statistics updated successfully in " + (endTime - startTime) + "ms");
            
            if (listener != null) {
                listener.onSuccess();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error updating course statistics: " + e.getMessage(), e);
            
            if (listener != null) {
                listener.onError(e.getMessage());
            }
        });
    }
    
    // Listener Interfaces
    
    public interface OnCoursesLoadedListener {
        void onCoursesLoaded(List<Course> courses);
        void onError(String errorMessage);
    }
    
    public interface OnCourseLoadedListener {
        void onCourseLoaded(Course course);
        void onError(String errorMessage);
    }
    
    public interface OnUserLoadedListener {
        void onUserLoaded(User user);
        void onError(String errorMessage);
    }
    
    public interface OnPurchaseListener {
        void onPurchaseSuccess();
        void onError(String errorMessage);
    }
    
    public interface OnCourseOperationListener {
        void onSuccess();
        void onError(String errorMessage);
    }
    
    public void addReviewToCourse(String courseId, Course.Review review, final OnCourseOperationListener listener) {
        if (courseId == null || courseId.isEmpty() || review == null) {
            Log.e(TAG, "Invalid parameters for adding review");
            if (listener != null) {
                listener.onError("Invalid parameters");
            }
            return;
        }
        
        Log.i(TAG, "Adding review to course: " + courseId);
        
        coursesCollection.document(courseId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Course course = documentSnapshot.toObject(Course.class);
                    if (course != null) {
                        // Add new review to existing reviews
                        List<Course.Review> reviews = course.getReviews();
                        if (reviews == null) {
                            reviews = new ArrayList<>();
                        }
                        reviews.add(review);
                        
                        // Calculate new average rating
                        double totalRating = 0;
                        for (Course.Review r : reviews) {
                            totalRating += r.getRating();
                        }
                        double newAverageRating = totalRating / reviews.size();
                        
                        // Update course with new reviews and average rating
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("reviews", reviews);
                        updates.put("averageRating", newAverageRating);
                        
                        coursesCollection.document(courseId).update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.i(TAG, "Review added successfully to course: " + courseId);
                                if (listener != null) {
                                    listener.onSuccess();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating course with review: " + e.getMessage());
                                if (listener != null) {
                                    listener.onError("Failed to save review: " + e.getMessage());
                                }
                            });
                    } else {
                        Log.e(TAG, "Error parsing course data");
                        if (listener != null) {
                            listener.onError("Error processing course data");
                        }
                    }
                } else {
                    Log.e(TAG, "Course not found: " + courseId);
                    if (listener != null) {
                        listener.onError("Course not found");
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading course for review: " + e.getMessage());
                if (listener != null) {
                    listener.onError("Failed to load course: " + e.getMessage());
                }
            });
    }
    
    // User management methods
    
    public User getCurrentUser() {
        Log.v(TAG, "Getting current user: " + (currentUser != null ? currentUser.getEmail() : "null"));
        return currentUser;
    }
    
    public void setCurrentUser(User user) {
        Log.i(TAG, "Setting current user: " + (user != null ? user.getEmail() : "null"));
        this.currentUser = user;
        this.currentUserId = user != null ? user.getEmail() : null;
    }
    
    
    public void loadCurrentUser(String userId, final OnUserLoadedListener listener) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot load current user: userId is null or empty");
            if (listener != null) {
                listener.onError("Invalid user ID");
            }
            return;
        }
        
        // Check if we have a cached user
        if (currentUser != null && userId.equals(currentUserId)) {
            Log.i(TAG, "Using cached current user: " + currentUser.getEmail());
            if (listener != null) {
                listener.onUserLoaded(currentUser);
            }
            return;
        }

        // Always use Firebase UID for consistency
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        final String userIdToUse;
        
        if (authUser != null) {
            // Always use the UID for consistency
            userIdToUse = authUser.getUid();
            Log.i(TAG, "Using authenticated user UID: " + userIdToUse);
        } else {
            userIdToUse = userId;
            Log.w(TAG, "No authenticated user, using provided ID: " + userId);
        }
        
        Log.i(TAG, "Loading current user with ID: " + userIdToUse);
        long startTime = System.currentTimeMillis();
        
        // Try to load from Firestore
        getUserById(userIdToUse, new OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                long endTime = System.currentTimeMillis();
                Log.i(TAG, "Current user loaded in " + (endTime - startTime) + "ms");
                
                currentUser = user;
                currentUserId = userIdToUse;
                
                if (listener != null) {
                    listener.onUserLoaded(user);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "User not found in Firestore: " + errorMessage);
                if (listener != null) {
                    listener.onError("User not found in database. Please register first.");
                }
            }
        });
    }
    
    /**
     * Helper method to save user to Firestore
     */
    private void saveUserToFirestore(User user, final OnUserSavedListener listener) {
        if (user == null || user.getEmail() == null) {
            Log.e(TAG, "Cannot save user: user or email is null");
            if (listener != null) {
                listener.onError("Invalid user data");
            }
            return;
        }
        
        Log.i(TAG, "Saving user to Firestore: " + user.getEmail());
        
        usersCollection.document(user.getEmail())
            .set(user)
            .addOnSuccessListener(aVoid -> {
                Log.i(TAG, "User saved successfully in Firestore: " + user.getEmail());
                if (listener != null) {
                    listener.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error saving user to Firestore: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            });
    }

    /**
     * Interface for tracking user save operations
     */
    public interface OnUserSavedListener {
        void onSuccess();
        void onError(String errorMessage);
    }
    
    public void updateUserWallet(double newBalance, final OnCourseOperationListener listener) {
        if (currentUser == null || currentUserId == null) {
            Log.e(TAG, "Cannot update wallet: no current user");
            if (listener != null) {
                listener.onError("No current user");
            }
            return;
        }
        
        Log.i(TAG, String.format("Updating wallet for user %s: %.2f", currentUserId, newBalance));
        long startTime = System.currentTimeMillis();
        
        Map<String, Object> update = new HashMap<>();
        update.put("wallet", newBalance);
        
        usersCollection.document(currentUserId)
            .update(update)
            .addOnSuccessListener(aVoid -> {
                long endTime = System.currentTimeMillis();
                Log.i(TAG, "Wallet updated successfully in " + (endTime - startTime) + "ms");
                
                // Update current user
                currentUser.setWallet(newBalance);
                
                if (listener != null) {
                    listener.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating wallet: " + e.getMessage(), e);
                
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            });
    }
    
    public boolean userOwnsCourse(String courseId) {
        if (currentUser == null || currentUser.getOwnedCourses() == null) {
            Log.w(TAG, "Cannot check course ownership: user or owned courses list is null");
            return false;
        }
        
        boolean owned = currentUser.getOwnedCourses().contains(courseId);
        Log.v(TAG, "User " + (owned ? "owns" : "does not own") + " course: " + courseId);
        return owned;
    }
}