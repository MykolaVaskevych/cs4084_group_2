package com.example.appdevelopmentprojectfinal.marketplace;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.appdevelopmentprojectfinal.utils.DataManager;
import com.example.appdevelopmentprojectfinal.utils.AcademicDatabaseManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import android.widget.Button;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class RecommendedCoursesFragment extends Fragment implements CourseAdapter.CourseClickListener {

    private static final String TAG = "RecommendedCoursesFragment";
    
    private TextView tvWalletBalance;
    private TextView tvSectionTitle;
    private EditText etSearch;
    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private TextView tvEmptyMessage;
    private View loadingView;
    private Button btnCreateCourses;
    
    private CourseAdapter adapter;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                         Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_marketplace_section, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "Initializing RecommendedCoursesFragment");
        
        tvWalletBalance = view.findViewById(R.id.tvWalletBalance);
        tvSectionTitle = view.findViewById(R.id.tvSectionTitle);
        etSearch = view.findViewById(R.id.etSearch);
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        loadingView = view.findViewById(R.id.loadingView);
        btnCreateCourses = view.findViewById(R.id.btnCreateCourses);
        
        // Set up button click listener
        btnCreateCourses.setOnClickListener(v -> createSampleCoursesDirectly());
        
        tvSectionTitle.setText(getString(R.string.recommended_courses));
        
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
            Toast.makeText(getContext(), "Please login to view recommended courses", Toast.LENGTH_SHORT).show();
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
                loadRecommendedCourses();
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
        MarketplaceFirestoreManager.getInstance().searchCourses(query, new MarketplaceFirestoreManager.OnCoursesLoadedListener() {
            @Override
            public void onCoursesLoaded(List<Course> courses) {
                showLoading(false);
                updateCoursesList(courses);
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(getContext(), "Search error: " + errorMessage, Toast.LENGTH_SHORT).show();
                updateCoursesList(new ArrayList<>());
            }
        });
    }
    
    private void loadRecommendedCourses() {
        showLoading(true);
        
        MarketplaceFirestoreManager marketplaceManager = MarketplaceFirestoreManager.getInstance();
        User currentUser = marketplaceManager.getCurrentUser();
        
        if (currentUser == null || currentUser.getModules() == null || currentUser.getModules().isEmpty()) {
            Log.w(TAG, "No current user or modules available for recommendations");
            showLoading(false);
            updateCoursesList(new ArrayList<>());
            return;
        }
        
        Log.i(TAG, "Loading recommended courses for user with " + currentUser.getModules().size() + " modules");
        List<String> userModules = currentUser.getModules();
        final List<Course> allRecommendedCourses = new ArrayList<>();
        final Set<String> processedCourseIds = new HashSet<>();
        final AtomicInteger remainingRequests = new AtomicInteger(userModules.size());
        
        for (String moduleCode : userModules) {
            Log.v(TAG, "Fetching recommended courses for module: " + moduleCode);
            marketplaceManager.loadCoursesByModule(moduleCode, new MarketplaceFirestoreManager.OnCoursesLoadedListener() {
                @Override
                public void onCoursesLoaded(List<Course> courses) {
                    Log.d(TAG, "Loaded " + courses.size() + " courses for module " + moduleCode);
                    synchronized (allRecommendedCourses) {
                        for (Course course : courses) {
                            if (!processedCourseIds.contains(course.getId())) {
                                processedCourseIds.add(course.getId());
                                if (!marketplaceManager.userOwnsCourse(course.getId())) {
                                    Log.v(TAG, "Adding recommended course: " + course.getName());
                                    allRecommendedCourses.add(course);
                                } else {
                                    Log.v(TAG, "Skipping already owned course: " + course.getName());
                                }
                            }
                        }
                    }
                    
                    if (remainingRequests.decrementAndGet() == 0) {
                        showLoading(false);
                        Log.d(TAG, "Loaded " + allRecommendedCourses.size() + " recommended courses");
                        updateCoursesList(allRecommendedCourses);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error loading recommended courses for module " + moduleCode + ": " + errorMessage);
                    
                    if (remainingRequests.decrementAndGet() == 0) {
                        showLoading(false);
                        if (allRecommendedCourses.isEmpty()) {
                            Toast.makeText(getContext(), "Failed to load courses", Toast.LENGTH_SHORT).show();
                        }
                        updateCoursesList(allRecommendedCourses);
                    }
                }
            });
        }
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
            tvEmptyMessage.setText(getString(R.string.no_recommended_courses));
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
    
    @Override
    public void onCourseClicked(Course course) {
        CourseDetailDialog dialog = CourseDetailDialog.newInstance(course.getId());
        dialog.show(getParentFragmentManager(), "CourseDetail");
        
        dialog.setPurchaseCompletedListener(() -> {
            loadRecommendedCourses();
            updateWalletBalance();
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "RecommendedCoursesFragment resumed");
        
        // Only refresh data if we already have a user loaded
        if (MarketplaceFirestoreManager.getInstance().getCurrentUser() != null) {
            loadRecommendedCourses();
            updateWalletBalance();
        } else {
            Log.w(TAG, "Cannot refresh courses: no user loaded");
            initializeUserData();
        }
    }
    
    private void createSampleCoursesDirectly() {
        Log.i(TAG, "Creating sample marketplace courses directly");
        
        Toast.makeText(requireContext(), "Creating sample courses...", Toast.LENGTH_SHORT).show();
        
        // Show loading
        showLoading(true);
        
        // Delete any existing courses first
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("marketplace")
            .get()
            .addOnSuccessListener(snapshot -> {
                Log.d(TAG, "Found " + snapshot.size() + " existing marketplace courses - deleting them");
                
                if (snapshot.size() > 0) {
                    // Delete each course document
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        doc.getReference().delete()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Deleted marketplace course: " + doc.getId()))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to delete marketplace course: " + doc.getId(), e));
                    }
                }
                
                // Create new courses after a short delay
                new android.os.Handler().postDelayed(() -> {
                    createMarketplaceCourses();
                }, 1000);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting existing marketplace courses: " + e.getMessage(), e);
                // Create courses anyway
                createMarketplaceCourses();
            });
    }
    
    private void createMarketplaceCourses() {
        Log.i(TAG, "Creating new marketplace courses");
        
        // Create directly
        createCourses();
    }
    
    private void createCourses() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference coursesRef = db.collection("marketplace");
        
        // Sample module codes and course names
        String[][] coursesData = {
            {"cs4013", "Object Oriented Development Masterclass"},
            {"cs4141", "Introduction to Programming"},
            {"cs4111", "Computer Architecture and Organization"},
            {"cs4084", "Mobile Application Development"},
            {"cs4116", "Software Development Project"}
        };
        
        // YouTube demo video URL
        String demoVideoUrl = "https://www.youtube.com/embed/bhwPhcFJU7E?si=S5KGzdEw8ie4IUq1";
        
        // Create each course
        for (String[] courseData : coursesData) {
            String moduleCode = courseData[0];
            String courseName = courseData[1];
            
            Course course = new Course();
            String courseId = coursesRef.document().getId();
            course.setId(courseId);
            course.setName(courseName);
            course.setRelatedModule(moduleCode);
            course.setDescription("A comprehensive course covering all aspects of " + courseName + 
                                 ". Learn the fundamentals and advanced techniques needed to excel in this subject.");
            course.setPrice(19.99);
            course.setLogo("https://picsum.photos/200");
            course.setAuthor("Professor " + getRandomName());
            
            // Set tags
            List<String> tags = new ArrayList<>();
            tags.add(moduleCode.toUpperCase());
            tags.add("Computer Science");
            tags.add("Programming");
            tags.add(courseName.split(" ")[0]);
            course.setTags(tags);
            
            // Set statistics (high values to appear in trending)
            Course.CourseStatistics stats = new Course.CourseStatistics();
            stats.setTotalPurchases(200);
            stats.setViewsToday(100);
            stats.setPurchasesToday(30);
            course.setStatistics(stats);
            
            // Set rating
            course.setAverageRating(4.5);
            
            // Create reviews
            List<Course.Review> reviews = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Course.Review review = new Course.Review();
                review.setUser(getRandomName());
                review.setRating(4.5);
                review.setComment("Great course! Very informative and well-structured.");
                reviews.add(review);
            }
            course.setReviews(reviews);
            
            // Create course content
            Course.CourseContent content = new Course.CourseContent();
            
            // Preview
            Course.Preview preview = new Course.Preview();
            preview.setTitle("Course Preview");
            
            List<Course.ContentItem> previewItems = new ArrayList<>();
            
            // Preview video
            Course.ContentItem previewVideo = new Course.ContentItem();
            previewVideo.setType("video");
            previewVideo.setTitle("Introduction to " + courseName);
            previewVideo.setUrl(demoVideoUrl);
            previewVideo.setCaption("A brief introduction to the course");
            previewItems.add(previewVideo);
            
            // Preview text
            Course.ContentItem previewText = new Course.ContentItem();
            previewText.setType("text");
            previewText.setTitle("About this Course");
            previewText.setContent("This course will teach you everything you need to know about " + courseName + 
                                  ". You'll learn key concepts, practical skills, and gain hands-on experience.");
            previewItems.add(previewText);
            
            preview.setItems(previewItems);
            content.setPreview(preview);
            
            // Chapters
            List<Course.Chapter> chapters = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                Course.Chapter chapter = new Course.Chapter();
                chapter.setTitle("Chapter " + i + ": " + (i == 1 ? "Introduction" : "Advanced Topics"));
                
                List<Course.ContentItem> lessons = new ArrayList<>();
                for (int j = 1; j <= 2; j++) {
                    // Video lesson
                    Course.ContentItem videoLesson = new Course.ContentItem();
                    videoLesson.setType("video");
                    videoLesson.setTitle("Lesson " + j + ": Video Lecture");
                    videoLesson.setUrl(demoVideoUrl);
                    videoLesson.setCaption("Chapter " + i + " - Lesson " + j);
                    lessons.add(videoLesson);
                    
                    // Text lesson
                    Course.ContentItem textLesson = new Course.ContentItem();
                    textLesson.setType("text");
                    textLesson.setTitle("Lesson " + j + ": Reading Material");
                    textLesson.setContent("This lesson covers important concepts related to " + courseName + 
                                         ".\n\nExample code:\n\n```java\npublic class Example {\n    public static void main(String[] args) {\n        " +
                                         "System.out.println(\"Hello world\");\n    }\n}\n```");
                    lessons.add(textLesson);
                }
                
                chapter.setItems(lessons);
                chapters.add(chapter);
            }
            
            content.setChapters(chapters);
            course.setContent(content);
            
            // Save to Firestore
            coursesRef.document(courseId).set(course)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Course created successfully: " + courseName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating course: " + e.getMessage());
                });
        }
        
        // Give time for courses to be created then reload
        new android.os.Handler().postDelayed(() -> {
            showLoading(false);
            Toast.makeText(requireContext(), "Sample courses created, reloading...", Toast.LENGTH_SHORT).show();
            loadRecommendedCourses();
        }, 3000);
    }
    
    private String getRandomName() {
        String[] firstNames = {"John", "Jane", "David", "Michael", "Sarah"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Jones", "Brown"};
        
        String firstName = firstNames[(int)(Math.random() * firstNames.length)];
        String lastName = lastNames[(int)(Math.random() * lastNames.length)];
        
        return firstName + " " + lastName;
    }
}