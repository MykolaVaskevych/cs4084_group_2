package com.example.appdevelopmentprojectfinal.marketplace;

import android.app.Dialog;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.appdevelopmentprojectfinal.utils.YouTubeHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdevelopmentprojectfinal.R;
import com.example.appdevelopmentprojectfinal.StoreFragment;
import com.example.appdevelopmentprojectfinal.model.Course;
import com.example.appdevelopmentprojectfinal.model.Module;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CourseDetailDialog extends DialogFragment {

    private static final String TAG = "CourseDetailDialog";
    private static final String ARG_COURSE_ID = "courseId";

    public interface PurchaseCompletedListener {
        void onPurchaseCompleted();
    }

    private String courseId;
    private Course course;
    private PurchaseCompletedListener purchaseCompletedListener;

    private ImageView ivCourseImage;
    private CollapsingToolbarLayout collapsingToolbar;
    private Toolbar toolbar;
    private TextView tvModuleInfo;
    private TextView tvAuthor;
    private RatingBar ratingBar;
    private TextView tvRating;
    private TextView tvDescription;
    private TextView tvPreviewTitle;
    private TextView tvPreviewContent;
    private CardView previewContainer;
    private ImageView btnExpandPreview;
    
    private CardView expandedPreviewContainer;
    private TextView tvExpandedChapterTitle;
    private TextView tvExpandedContent;
    private TextView tvVideoTitle;
    private VideoView videoView;
    private WebView webViewYoutube;
    private ImageView ivPlayButton;
    private FrameLayout videoContainer;
    private ImageView btnCollapsePreview;
    
    private RecyclerView rvReviews;
    private TextView tvNoReviews;
    private RecyclerView rvRelatedCourses;
    private Button btnCancel;
    private Button btnBuy;
    private Button btnAddReview;
    private View loadingView;

    private ReviewAdapter reviewAdapter;
    private RelatedCourseAdapter relatedCourseAdapter;
    
    // Firestore reference
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static CourseDetailDialog newInstance(String courseId) {
        Log.d(TAG, "Creating new instance with courseId: " + courseId);
        CourseDetailDialog dialog = new CourseDetailDialog();
        Bundle args = new Bundle();
        args.putString(ARG_COURSE_ID, courseId);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_AppDevelopmentProjectFinal_FullScreenDialog);

        if (getArguments() != null) {
            courseId = getArguments().getString(ARG_COURSE_ID);
            Log.d(TAG, "Retrieved courseId from arguments: " + courseId);
        } else {
            Log.w(TAG, "No arguments provided to CourseDetailDialog");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        return inflater.inflate(R.layout.dialog_course_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated called");
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        toolbar.setNavigationOnClickListener(v -> {
            Log.d(TAG, "Navigation button clicked, dismissing dialog");
            dismiss();
        });

        if (courseId == null || courseId.isEmpty()) {
            Log.e(TAG, "courseId is null or empty, cannot load course details");
            Toast.makeText(requireContext(), getString(R.string.course_id_missing), Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }
        
        loadCourseDetails();
    }

    private void initViews(View view) {
        Log.d(TAG, "Initializing views");
        ivCourseImage = view.findViewById(R.id.ivCourseImage);
        collapsingToolbar = view.findViewById(R.id.collapsingToolbar);
        toolbar = view.findViewById(R.id.toolbar);
        tvModuleInfo = view.findViewById(R.id.tvModuleInfo);
        tvAuthor = view.findViewById(R.id.tvAuthor);
        ratingBar = view.findViewById(R.id.ratingBar);
        tvRating = view.findViewById(R.id.tvRating);
        tvDescription = view.findViewById(R.id.tvDescription);
        
        tvPreviewTitle = view.findViewById(R.id.tvPreviewTitle);
        tvPreviewContent = view.findViewById(R.id.tvPreviewContent);
        previewContainer = view.findViewById(R.id.previewContainer);
        btnExpandPreview = view.findViewById(R.id.btnExpandPreview);
        
        expandedPreviewContainer = view.findViewById(R.id.expandedPreviewContainer);
        tvExpandedChapterTitle = view.findViewById(R.id.tvExpandedChapterTitle);
        tvExpandedContent = view.findViewById(R.id.tvExpandedContent);
        tvVideoTitle = view.findViewById(R.id.tvVideoTitle);
        videoView = view.findViewById(R.id.videoView);
        webViewYoutube = view.findViewById(R.id.webViewYoutube);
        ivPlayButton = view.findViewById(R.id.ivPlayButton);
        videoContainer = view.findViewById(R.id.videoContainer);
        btnCollapsePreview = view.findViewById(R.id.btnCollapsePreview);
        
        rvReviews = view.findViewById(R.id.rvReviews);
        tvNoReviews = view.findViewById(R.id.tvNoReviews);
        rvRelatedCourses = view.findViewById(R.id.rvRelatedCourses);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnBuy = view.findViewById(R.id.btnBuy);
        btnAddReview = view.findViewById(R.id.btnAddReview);
        loadingView = view.findViewById(R.id.loadingView);
        
        setupPreviewExpansion();
        Log.d(TAG, "Views initialized successfully");
    }
    
    private void loadCourseDetails() {
        Log.i(TAG, "Loading course details for courseId: " + courseId);
        showLoading(true);
        
        db.collection("marketplace").document(courseId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    course = documentSnapshot.toObject(Course.class);
                    if (course != null) {
                        course.setId(documentSnapshot.getId());
                        Log.i(TAG, "Course loaded successfully: " + course.getName());
                        displayCourseDetails();
                        setupButtons();
                        showLoading(false);
                    } else {
                        Log.e(TAG, "Error parsing course data");
                        Toast.makeText(requireContext(), getString(R.string.course_not_found), Toast.LENGTH_SHORT).show();
                        showLoading(false);
                        dismiss();
                    }
                } else {
                    Log.e(TAG, "Course document not found");
                    Toast.makeText(requireContext(), getString(R.string.course_not_found), Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    dismiss();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading course: " + e.getMessage());
                Toast.makeText(requireContext(), getString(R.string.course_not_found), Toast.LENGTH_SHORT).show();
                showLoading(false);
                dismiss();
            });
    }
    
    private void showLoading(boolean isLoading) {
        Log.v(TAG, "Loading state changed: " + isLoading);
        if (loadingView != null) {
            if (isLoading) {
                loadingView.setVisibility(View.VISIBLE);
            } else {
                loadingView.setVisibility(View.GONE);
            }
        } else {
            Log.e(TAG, "loadingView is null in showLoading method");
        }
    }

    private void displayCourseDetails() {
        Log.i(TAG, "Displaying details for course: " + course.getName());
        
        collapsingToolbar.setTitle(course.getName());
        setCourseImage();
        setModuleInfo();
        tvAuthor.setText(getString(R.string.by_author, course.getAuthor()));
        setRatingInfo();
        tvDescription.setText(course.getDescription());
        setPreviewContent();
        setReviewsSection();
        loadRelatedCourses();
        configureBuyButton();
        
        Log.d(TAG, "Course details displayed successfully");
    }
    
    private void setCourseImage() {
        int colorCode;
        if (course.getRelatedModule() != null) {
            int hash = course.getRelatedModule().hashCode();
            int[] colors = {
                0xFF4CAF50, // Green
                0xFF2196F3, // Blue
                0xFFFF9800, // Orange
                0xFF9C27B0, // Purple
                0xFFE91E63  // Pink
            };
            colorCode = colors[Math.abs(hash) % colors.length];
            Log.v(TAG, "Setting course image color based on module hash: " + Integer.toHexString(colorCode));
        } else {
            colorCode = 0xFF4CAF50; // Green
            Log.v(TAG, "Using default green color for course image");
        }
        
        ivCourseImage.setBackgroundColor(colorCode);
    }
    
    private void setModuleInfo() {
        // Get module info from Firestore
        if (course.getRelatedModule() != null) {
            db.collection("modules").whereEqualTo("code", course.getRelatedModule())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        String code = doc.getString("code");
                        String name = doc.getString("name");
                        if (code != null && name != null) {
                            String moduleInfo = String.format("%s: %s", code, name);
                            Log.v(TAG, "Setting module info: " + moduleInfo);
                            tvModuleInfo.setText(moduleInfo);
                        } else {
                            tvModuleInfo.setText(course.getRelatedModule());
                        }
                    } else {
                        Log.v(TAG, "Module not found, using module code only: " + course.getRelatedModule());
                        tvModuleInfo.setText(course.getRelatedModule());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching module info: " + e.getMessage());
                    tvModuleInfo.setText(course.getRelatedModule());
                });
        } else {
            tvModuleInfo.setText("General");
        }
    }
    
    private void setRatingInfo() {
        double averageRating = course.getAverageRating();
        int numReviews = course.getReviews() != null ? course.getReviews().size() : 0;
        
        Log.v(TAG, String.format("Setting rating info: %.1f stars from %d reviews", averageRating, numReviews));
        ratingBar.setRating((float) averageRating);
        tvRating.setText(getString(R.string.rating_count, averageRating, numReviews));
    }
    
    private void setPreviewContent() {
        Log.d(TAG, "Setting up preview content");
        if (course.getContent() == null || course.getContent().getPreview() == null) {
            Log.w(TAG, "No preview content available for this course");
            previewContainer.setVisibility(View.GONE);
            return;
        }
        
        Course.Preview preview = course.getContent().getPreview();
        if (preview.getTitle() == null || preview.getTitle().isEmpty()) {
            Log.v(TAG, "Using default preview title");
            tvPreviewTitle.setText(getString(R.string.preview));
        } else {
            Log.v(TAG, "Using provided preview title: " + preview.getTitle());
            tvPreviewTitle.setText(preview.getTitle());
        }
        
        if (preview.getItems() == null || preview.getItems().isEmpty()) {
            Log.w(TAG, "Preview has no items, hiding preview section");
            previewContainer.setVisibility(View.GONE);
            return;
        }
        
        StringBuilder previewText = new StringBuilder();
        int textItemsFound = 0;
        int videoCount = 0;
        int imageCount = 0;
        
        // Count content types and gather text content
        for (Course.ContentItem item : preview.getItems()) {
            String itemType = item.getType();
            if (itemType == null) {
                Log.w(TAG, "Found preview item with null type");
                continue;
            }
            
            Log.v(TAG, "Processing preview item of type: " + itemType);
            
            if ("text".equals(itemType) && item.getContent() != null) {
                if (textItemsFound > 0) {
                    previewText.append("\n\n");
                }
                previewText.append(item.getContent());
                textItemsFound++;
                
                Log.v(TAG, "Added text item #" + textItemsFound);
            } else if ("video".equals(itemType)) {
                videoCount++;
                Log.v(TAG, "Found video item #" + videoCount);
            } else if ("image".equals(itemType)) {
                imageCount++;
                Log.v(TAG, "Found image item #" + imageCount);
            }
        }
        
        Log.d(TAG, String.format("Preview content summary: %d text items, %d videos, %d images", 
                textItemsFound, videoCount, imageCount));
        
        if (textItemsFound == 0) {
            if (videoCount > 0 || imageCount > 0) {
                StringBuilder contentDesc = new StringBuilder(getString(R.string.preview_contains));
                
                if (videoCount > 0) {
                    contentDesc.append(" ").append(videoCount).append(" ");
                    String videoText = videoCount == 1 ? 
                                     getString(R.string.video) : 
                                     getString(R.string.videos);
                    contentDesc.append(videoText);
                }
                
                if (videoCount > 0 && imageCount > 0) {
                    contentDesc.append(" ").append(getString(R.string.and)).append(" ");
                }
                
                if (imageCount > 0) {
                    contentDesc.append(" ").append(imageCount).append(" ");
                    String imageText = imageCount == 1 ? 
                                     getString(R.string.image) : 
                                     getString(R.string.images);
                    contentDesc.append(imageText);
                }
                
                Log.v(TAG, "Using media count description: " + contentDesc);
                previewText.append(contentDesc);
            } else {
                Log.w(TAG, "No usable content found in preview");
                previewText.append(getString(R.string.preview_tap_to_see));
            }
        }
        
        if (previewText.length() > 0) {
            Log.v(TAG, "Setting preview text with " + previewText.length() + " characters");
            tvPreviewContent.setText(previewText.toString());
        } else {
            Log.v(TAG, "Using default preview text");
            tvPreviewContent.setText(getString(R.string.preview_tap_to_see));
        }
        
        previewContainer.setVisibility(View.VISIBLE);
    }
    
    private void setReviewsSection() {
        Log.d(TAG, "Setting up reviews section");
        if (course.getReviews() == null || course.getReviews().isEmpty()) {
            Log.w(TAG, "No reviews available for this course");
            rvReviews.setVisibility(View.GONE);
            tvNoReviews.setVisibility(View.VISIBLE);
            return;
        }
        
        Log.i(TAG, "Displaying " + course.getReviews().size() + " reviews");
        reviewAdapter = new ReviewAdapter(course.getReviews());
        rvReviews.setAdapter(reviewAdapter);
        tvNoReviews.setVisibility(View.GONE);
    }
    
    private void loadRelatedCourses() {
        Log.i(TAG, "Loading related courses for module: " + course.getRelatedModule());
        showRelatedCoursesLoading(true);
        
        db.collection("marketplace")
            .whereEqualTo("relatedModule", course.getRelatedModule())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Course> courses = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    Course relatedCourse = doc.toObject(Course.class);
                    if (relatedCourse != null) {
                        relatedCourse.setId(doc.getId());
                        if (!relatedCourse.getId().equals(courseId)) {
                            courses.add(relatedCourse);
                            Log.v(TAG, "Added related course: " + relatedCourse.getName());
                        }
                    }
                }
                
                if (courses.isEmpty()) {
                    Log.w(TAG, "No related courses found");
                    rvRelatedCourses.setVisibility(View.GONE);
                } else {
                    Log.i(TAG, "Displaying " + courses.size() + " related courses");
                    relatedCourseAdapter = new RelatedCourseAdapter(courses, CourseDetailDialog.this::showRelatedCourseDetail);
                    rvRelatedCourses.setAdapter(relatedCourseAdapter);
                    rvRelatedCourses.setVisibility(View.VISIBLE);
                }
                
                showRelatedCoursesLoading(false);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading related courses: " + e.getMessage());
                rvRelatedCourses.setVisibility(View.GONE);
                showRelatedCoursesLoading(false);
            });
    }
    
    private void showRelatedCoursesLoading(boolean isLoading) {
        Log.v(TAG, "Related courses loading state: " + isLoading);
        // Placeholder for future implementation of related courses loading indicator
        // This method is called in loadRelatedCourses() but doesn't affect the UI currently
    }
    
    private void configureBuyButton() {
        Log.d(TAG, "Configuring buy button");
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        format.setCurrency(Currency.getInstance("EUR"));
        String formattedPrice = format.format(course.getPrice());
        
        Log.v(TAG, "Setting buy button price: " + formattedPrice);
        btnBuy.setText(getString(R.string.buy_price, formattedPrice));
        btnBuy.setEnabled(true);
        
        // Check Firebase user first
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Log.w(TAG, "Firebase user is null, showing login required message");
            btnBuy.setText(getString(R.string.login_to_buy));
            btnBuy.setEnabled(false);
            return;
        }
        
        // Cache formatted strings for use in callback
        final String buyButtonText = getString(R.string.buy_price, formattedPrice);
        final String ownedText = getString(R.string.owned);
        final String yourCourseText = getString(R.string.your_course);
        
        // Check if this is the user's own authored course
        if (course.getAuthorId() != null && course.getAuthorId().equals(firebaseUser.getUid())) {
            Log.i(TAG, "This is user's own authored course, disabling buy button");
            btnBuy.setText(yourCourseText);
            btnBuy.setEnabled(false);
            return;
        }
        
        // Check if the user owns this course
        checkIfUserOwnsCourse(firebaseUser.getUid(), course.getId(), isOwned -> {
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "Fragment detached, skipping UI update");
                return;
            }
            
            if (isOwned) {
                Log.i(TAG, "User already owns this course, disabling buy button");
                btnBuy.setText(ownedText);
                btnBuy.setEnabled(false);
            } else {
                Log.v(TAG, "Course is available for purchase");
                btnBuy.setText(buyButtonText);
                btnBuy.setEnabled(true);
            }
        });
    }

    private void setupButtons() {
        Log.d(TAG, "Setting up button actions");
        btnCancel.setOnClickListener(v -> {
            Log.d(TAG, "Cancel button clicked, dismissing dialog");
            dismiss();
        });

        btnBuy.setOnClickListener(v -> {
            Log.d(TAG, "Buy button clicked");
            
            // Get user from FirebaseAuth
            FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentFirebaseUser == null) {
                Log.e(TAG, "Cannot purchase: Firebase user is null");
                Toast.makeText(requireContext(), getString(R.string.user_not_found), Toast.LENGTH_SHORT).show();
                return;
            }
            
            String uid = currentFirebaseUser.getUid();
            
            // Check if user is attempting to buy their own course
            if (course.getAuthorId() != null && course.getAuthorId().equals(uid)) {
                Log.w(TAG, "User attempting to buy their own course: " + course.getId());
                Toast.makeText(requireContext(), getString(R.string.cannot_buy_own_course), Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Check if user already owns this course
            checkIfUserOwnsCourse(uid, course.getId(), isOwned -> {
                if (isOwned) {
                    Log.w(TAG, "User already owns course: " + course.getId());
                    Toast.makeText(requireContext(), getString(R.string.already_owned), Toast.LENGTH_SHORT).show();
                } else {
                    // User doesn't own course, show purchase dialog
                    showPurchaseConfirmationDialog();
                }
            });
        });
        
        btnAddReview.setOnClickListener(v -> {
            Log.d(TAG, "Add review button clicked");
            
            // Check if user is authenticated
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(requireContext(), getString(R.string.login_to_review), Toast.LENGTH_SHORT).show();
                return;
            }
            
            showAddReviewDialog();
        });
    }
    
    private void showAddReviewDialog() {
        Log.i(TAG, "Showing add review dialog");
        
        try {
            // Inflate custom dialog layout
            View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_review, null);
            
            RatingBar ratingBar = view.findViewById(R.id.ratingBarReview);
            EditText etComment = view.findViewById(R.id.etReviewComment);
            Button btnCancel = view.findViewById(R.id.btnCancelReview);
            Button btnSubmit = view.findViewById(R.id.btnSubmitReview);
            
            Dialog reviewDialog = new MaterialAlertDialogBuilder(requireContext())
                    .setView(view)
                    .setCancelable(true)
                    .create();
            
            btnCancel.setOnClickListener(v -> {
                Log.d(TAG, "Review cancelled");
                reviewDialog.dismiss();
            });
            
            btnSubmit.setOnClickListener(v -> {
                float rating = ratingBar.getRating();
                String comment = etComment.getText().toString().trim();
                
                if (rating == 0) {
                    Toast.makeText(requireContext(), getString(R.string.rating_required), Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (comment.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.comment_required), Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Get current user ID for review
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    Toast.makeText(requireContext(), getString(R.string.user_not_found), Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Use user email for display, but store review with UID for consistency
                submitReview(currentUser.getEmail(), currentUser.getUid(), rating, comment);
                reviewDialog.dismiss();
            });
            
            reviewDialog.show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing add review dialog: " + e.getMessage(), e);
            Toast.makeText(requireContext(), getString(R.string.dialog_error), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void submitReview(String userEmail, String userId, float rating, String comment) {
        Log.i(TAG, "Submitting review: userId=" + userId + ", rating=" + rating + ", comment length=" + comment.length());
        showLoading(true);
        
        // Create new review
        Course.Review review = new Course.Review();
        review.setUser(userEmail); // Display email for UI purposes
        review.setUserId(userId); // Store UID for consistency and lookups
        review.setRating(rating);
        review.setComment(comment);
        
        DocumentReference courseRef = db.collection("marketplace").document(course.getId());
        
        // Get current reviews
        courseRef.get().addOnSuccessListener(documentSnapshot -> {
            Course courseData = documentSnapshot.toObject(Course.class);
            List<Course.Review> reviews = new ArrayList<>();
            
            if (courseData != null && courseData.getReviews() != null) {
                reviews = new ArrayList<>(courseData.getReviews());
            }
            
            // Add new review
            reviews.add(review);
            
            // Calculate new average rating
            double totalRating = 0;
            for (Course.Review r : reviews) {
                totalRating += r.getRating();
            }
            double newAvgRating = totalRating / reviews.size();
            
            // Update course document
            Map<String, Object> updates = new HashMap<>();
            updates.put("reviews", reviews);
            updates.put("averageRating", newAvgRating);
            
            courseRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Review added successfully");
                    
                    // Update local course object
                    if (course.getReviews() == null) {
                        course.setReviews(new ArrayList<>());
                    }
                    course.getReviews().add(review);
                    course.setAverageRating(newAvgRating);
                    
                    // Update UI
                    setRatingInfo();
                    setReviewsSection();
                    
                    showLoading(false);
                    Toast.makeText(requireContext(), getString(R.string.review_added), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding review: " + e.getMessage());
                    showLoading(false);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error getting current reviews: " + e.getMessage());
            showLoading(false);
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    
    private void setupPreviewExpansion() {
        Log.d(TAG, "Setting up preview expansion controls");
        
        btnExpandPreview.setOnClickListener(v -> {
            Log.d(TAG, "Expanding preview content");
            previewContainer.setVisibility(View.GONE);
            expandedPreviewContainer.setVisibility(View.VISIBLE);
            prepareExpandedPreview();
        });
        
        btnCollapsePreview.setOnClickListener(v -> {
            Log.d(TAG, "Collapsing preview content");
            expandedPreviewContainer.setVisibility(View.GONE);
            previewContainer.setVisibility(View.VISIBLE);
            
            if (videoView.isPlaying()) {
                Log.v(TAG, "Stopping video playback on collapse");
                videoView.stopPlayback();
            }
            
            // Also reset the YouTube player if it's visible
            if (webViewYoutube.getVisibility() == View.VISIBLE) {
                Log.v(TAG, "Resetting YouTube player on collapse");
                webViewYoutube.loadUrl("about:blank");
            }
        });
        
        ivPlayButton.setOnClickListener(v -> {
            Log.d(TAG, "Play button clicked");
            playVideo();
        });
    }
    
    private void prepareExpandedPreview() {
        if (course == null || course.getContent() == null) {
            expandedPreviewContainer.setVisibility(View.GONE);
            return;
        }
        
        if (course.getContent().getPreview() != null && 
            course.getContent().getPreview().getItems() != null &&
            !course.getContent().getPreview().getItems().isEmpty()) {
            
            Course.Preview preview = course.getContent().getPreview();
            tvExpandedChapterTitle.setText(preview.getTitle());
            
            StringBuilder contentBuilder = new StringBuilder();
            String videoTitle = preview.getTitle();
            String videoUrl = null;
            
            for (Course.ContentItem item : preview.getItems()) {
                String itemType = item.getType();
                if (itemType == null) {
                    continue;
                }
                
                if ("text".equals(itemType) && item.getContent() != null) {
                    contentBuilder.append(item.getContent()).append("\n\n");
                } else if ("video".equals(itemType)) {
                    if (item.getTitle() != null) {
                        videoTitle = item.getTitle();
                    }
                    
                    if (item.getUrl() != null) {
                        videoUrl = item.getUrl();
                    }
                }
            }
            
            if (videoTitle != null && !videoTitle.isEmpty()) {
                tvVideoTitle.setText(videoTitle);
                tvVideoTitle.setVisibility(View.VISIBLE);
            } else {
                tvVideoTitle.setVisibility(View.GONE);
            }
            
            if (videoUrl != null) {
                videoContainer.setVisibility(View.VISIBLE);
                setUpVideoView(videoUrl);
            } else {
                videoContainer.setVisibility(View.GONE);
            }
            
            if (contentBuilder.length() > 0) {
                tvExpandedContent.setText(contentBuilder.toString());
                tvExpandedContent.setVisibility(View.VISIBLE);
            } else {
                tvExpandedContent.setVisibility(View.GONE);
            }
        }
        else if (course.getContent().getChapters() != null && !course.getContent().getChapters().isEmpty()) {
            Course.Chapter firstChapter = course.getContent().getChapters().get(0);
            tvExpandedChapterTitle.setText(firstChapter.getTitle());
            
            StringBuilder contentBuilder = new StringBuilder();
            String videoTitle = "";
            String videoUrl = null;
            
            if (firstChapter.getItems() != null) {
                for (Course.ContentItem item : firstChapter.getItems()) {
                    String itemType = item.getType();
                    if (itemType == null) {
                        continue;
                    }
                    
                    switch (itemType) {
                        case "text":
                            if (item.getContent() != null) {
                                contentBuilder.append(item.getContent()).append("\n\n");
                            }
                            break;
                            
                        case "video":
                            if (item.getTitle() != null) {
                                videoTitle = item.getTitle();
                            }
                            
                            if (item.getUrl() != null) {
                                videoUrl = item.getUrl();
                            }
                            break;
                    }
                }
            }
            
            if (!videoTitle.isEmpty()) {
                tvVideoTitle.setText(videoTitle);
                tvVideoTitle.setVisibility(View.VISIBLE);
            } else {
                tvVideoTitle.setVisibility(View.GONE);
            }
            
            if (videoUrl != null) {
                videoContainer.setVisibility(View.VISIBLE);
                setUpVideoView(videoUrl);
            } else {
                videoContainer.setVisibility(View.GONE);
            }
            
            if (contentBuilder.length() > 0) {
                tvExpandedContent.setText(contentBuilder.toString());
                tvExpandedContent.setVisibility(View.VISIBLE);
            } else {
                tvExpandedContent.setVisibility(View.GONE);
            }
        } else {
            tvVideoTitle.setVisibility(View.GONE);
            videoContainer.setVisibility(View.GONE);
            tvExpandedContent.setVisibility(View.GONE);
        }
    }
    
    private void setUpVideoView(String videoUrl) {
        Log.i(TAG, "Setting up video view for URL: " + videoUrl);
        
        try {
            long startTime = System.currentTimeMillis();
            
            if (videoUrl == null || videoUrl.trim().isEmpty()) {
                Log.e(TAG, "Video URL is null or empty");
                videoContainer.setVisibility(View.GONE);
                return;
            }
            
            String trimmedUrl = videoUrl.trim();
            
            if (YouTubeHelper.isYoutubeUrl(trimmedUrl)) {
                videoView.setVisibility(View.GONE);
                webViewYoutube.setVisibility(View.VISIBLE);
                
                String embedUrl = YouTubeHelper.convertToEmbedUrl(trimmedUrl);
                setupYouTubePlayer(embedUrl);
            } else {
                webViewYoutube.setVisibility(View.GONE);
                videoView.setVisibility(View.VISIBLE);
                
                String resourcePath = findLocalVideoResourceForUrl(trimmedUrl);
                videoView.setVideoURI(Uri.parse(resourcePath));
                
                videoView.setOnCompletionListener(mp -> {
                    ivPlayButton.setVisibility(View.VISIBLE);
                });
                
                setupVideoListeners();
            }
            
            ivPlayButton.setVisibility(View.VISIBLE);
            Log.d(TAG, "Video setup completed in " + (System.currentTimeMillis() - startTime) + "ms");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set up video view: " + e.getMessage(), e);
            videoContainer.setVisibility(View.GONE);
        }
    }
    
    private void setupYouTubePlayer(String youtubeUrl) {
        Log.d(TAG, "Setting up YouTube player for URL: " + youtubeUrl);
        YouTubeHelper.loadYoutubeVideo(webViewYoutube, youtubeUrl, false);
    }
    
    private String findLocalVideoResourceForUrl(String videoUrl) {
        String defaultResourcePath = "android.resource://" + requireContext().getPackageName() + "/raw/course_preview_demo";
        
        if (videoUrl == null || videoUrl.isEmpty()) {
            return defaultResourcePath;
        }
        
        try {
            String filename = getFilenameFromUrl(videoUrl);
            if (filename != null && !filename.isEmpty()) {
                String resourceName = convertToResourceName(filename);
                
                int resourceId = findResourceId(resourceName);
                
                if (resourceId != 0) {
                    return "android.resource://" + requireContext().getPackageName() + "/" + resourceId;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding local video resource: " + e.getMessage(), e);
        }
        
        return defaultResourcePath;
    }
    
    private String getFilenameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        int lastSlashIndex = url.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < url.length() - 1) {
            return url.substring(lastSlashIndex + 1);
        }
        
        return url;
    }
    
    private String convertToResourceName(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "course_preview_demo";
        }
        
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex != -1) {
            filename = filename.substring(0, dotIndex);
        }
        
        return filename.toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }
    
    private int findResourceId(String resourceName) {
        android.content.res.Resources resources = requireContext().getResources();
        String packageName = requireContext().getPackageName();
        
        int resourceId = resources.getIdentifier(resourceName, "raw", packageName);
        if (resourceId != 0) {
            return resourceId;
        }
        
        resourceId = resources.getIdentifier(resourceName, "drawable", packageName);
        if (resourceId != 0) {
            return resourceId;
        }
        
        return 0;
    }
    
    private void playVideo() {
        Log.i(TAG, "Starting video playback");
        ivPlayButton.setVisibility(View.GONE);
        
        try {
            long startTime = System.currentTimeMillis();
            
            if (webViewYoutube.getVisibility() == View.VISIBLE) {
                String videoUrl = findVideoUrl();
                
                if (!videoUrl.isEmpty()) {
                    if (YouTubeHelper.isYoutubeUrl(videoUrl)) {
                        Log.i(TAG, "Loading YouTube video with URL: " + videoUrl);
                        YouTubeHelper.loadYoutubeVideo(webViewYoutube, videoUrl, true);
                    } else {
                        Log.w(TAG, "Not a valid YouTube URL: " + videoUrl);
                        Toast.makeText(requireContext(), 
                                getString(R.string.video_loading_error_message, "Invalid YouTube URL"),
                                Toast.LENGTH_SHORT).show();
                        ivPlayButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    Log.w(TAG, "No video URL found");
                    Toast.makeText(requireContext(), 
                            getString(R.string.video_loading_error_message, "No video URL found"),
                            Toast.LENGTH_SHORT).show();
                    ivPlayButton.setVisibility(View.VISIBLE);
                }
            } else {
                videoView.requestFocus();
                videoView.start();
            }
            
            Log.d(TAG, "Video playback started in " + (System.currentTimeMillis() - startTime) + "ms");
        } catch (Exception e) {
            Log.e(TAG, "Error playing video: " + e.getMessage(), e);
            Toast.makeText(requireContext(), 
                    getString(R.string.video_loading_error_message, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
            ivPlayButton.setVisibility(View.VISIBLE);
        }
    }
    
    private String findVideoUrl() {
        if (course == null || course.getContent() == null) {
            return "";
        }
        
        String videoUrl = "";
        
        if (course.getContent().getPreview() != null && 
            course.getContent().getPreview().getItems() != null &&
            !course.getContent().getPreview().getItems().isEmpty()) {
            
            for (Course.ContentItem item : course.getContent().getPreview().getItems()) {
                if ("video".equals(item.getType()) && item.getUrl() != null && !item.getUrl().isEmpty()) {
                    return item.getUrl();
                }
            }
        }
        
        if (course.getContent().getChapters() != null && 
            !course.getContent().getChapters().isEmpty()) {
            
            Course.Chapter firstChapter = course.getContent().getChapters().get(0);
            
            if (firstChapter.getItems() != null) {
                for (Course.ContentItem item : firstChapter.getItems()) {
                    if ("video".equals(item.getType()) && item.getUrl() != null && !item.getUrl().isEmpty()) {
                        return item.getUrl();
                    }
                }
            }
        }
        
        return videoUrl;
    }
    
    private void setupVideoListeners() {
        Log.d(TAG, "Setting up video player listeners");
        
        videoView.setOnPreparedListener(mp -> {
            int width = mp.getVideoWidth();
            int height = mp.getVideoHeight();
            int durationMs = mp.getDuration();
            
            Log.i(TAG, String.format("Video prepared: %dx%d, duration: %.2fs", 
                    width, height, durationMs/1000f));
            
            mp.setLooping(false);
            mp.setVolume(1.0f, 1.0f);
            videoView.start();
        });
        
        videoView.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, String.format("Video playback error: what=%d, extra=%d", what, extra));
            Toast.makeText(requireContext(), 
                    getString(R.string.video_error_message), 
                    Toast.LENGTH_SHORT).show();
            ivPlayButton.setVisibility(View.VISIBLE);
            return true;
        });
    }

    private void checkIfUserOwnsCourse(String userId, String courseId, OnCourseOwnershipCheckListener listener) {
        if (userId == null || userId.isEmpty() || courseId == null || courseId.isEmpty()) {
            if (listener != null) {
                listener.onResult(false);
            }
            return;
        }
        
        final boolean wasAttached = isAdded();
        
        Log.d(TAG, "Checking if user " + userId + " owns course " + courseId);
        
        db.collection("users").document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!wasAttached) {
                    Log.w(TAG, "Fragment detached, skipping callback");
                    return;
                }
                
                if (documentSnapshot.exists()) {
                    List<String> ownedCourses = (List<String>) documentSnapshot.get("ownedCourses");
                    boolean isOwned = ownedCourses != null && ownedCourses.contains(courseId);
                    Log.d(TAG, "User ownership check: " + isOwned);
                    if (listener != null) {
                        listener.onResult(isOwned);
                    }
                } else {
                    Log.w(TAG, "User document not found for ID: " + userId);
                    if (listener != null) {
                        listener.onResult(false);
                    }
                }
            })
            .addOnFailureListener(e -> {
                if (!wasAttached) {
                    Log.w(TAG, "Fragment detached, skipping error callback");
                    return;
                }
                
                Log.e(TAG, "Error checking course ownership: " + e.getMessage());
                if (listener != null) {
                    listener.onResult(false);
                }
            });
    }
    
    private void getUserWalletBalance(String userId, OnWalletBalanceListener listener) {
        if (userId == null || userId.isEmpty()) {
            if (listener != null) {
                listener.onResult(0.0);
            }
            return;
        }
        
        final boolean wasAttached = isAdded();
        
        Log.d(TAG, "Getting wallet balance for user: " + userId);
        
        db.collection("users").document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!wasAttached) {
                    Log.w(TAG, "Fragment detached, skipping wallet callback");
                    return;
                }
                
                if (documentSnapshot.exists()) {
                    Double balance = documentSnapshot.getDouble("wallet");
                    if (balance != null) {
                        Log.d(TAG, "User wallet balance: " + balance);
                        if (listener != null) {
                            listener.onResult(balance);
                        }
                    } else {
                        Log.w(TAG, "User has no wallet balance");
                        if (listener != null) {
                            listener.onResult(0.0);
                        }
                    }
                } else {
                    Log.w(TAG, "User document not found for ID: " + userId);
                    if (listener != null) {
                        listener.onResult(0.0);
                    }
                }
            })
            .addOnFailureListener(e -> {
                if (!wasAttached) {
                    Log.w(TAG, "Fragment detached, skipping wallet error callback");
                    return;
                }
                
                Log.e(TAG, "Error getting wallet balance: " + e.getMessage());
                if (listener != null) {
                    listener.onResult(0.0);
                }
            });
    }
    
    private void showPurchaseConfirmationDialog() {
        Log.i(TAG, "Showing purchase confirmation dialog");
        
        if (!isAdded() || getContext() == null) {
            Log.e(TAG, "Cannot show purchase dialog: Fragment not attached to context");
            return;
        }
        
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Log.e(TAG, "Cannot show purchase dialog: Firebase user is null");
            Toast.makeText(requireContext(), getString(R.string.user_not_found), Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (course == null) {
            Log.e(TAG, "Cannot show purchase dialog: course is null");
            return;
        }
        
        // Cache string resources needed in callback
        final String insufficientFundsMessage = getString(R.string.insufficient_funds);
        
        getUserWalletBalance(firebaseUser.getUid(), (walletBalance) -> {
            if (!isAdded() || getContext() == null) {
                Log.e(TAG, "Fragment detached, skipping dialog creation");
                return;
            }
            
            // Continue with purchase dialog setup
            try {
                View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_purchase_confirmation, null);
                Log.v(TAG, "Purchase confirmation dialog layout inflated");
                
                TextView tvPurchaseCourseName = view.findViewById(R.id.tvPurchaseCourseName);
                TextView tvPurchasePrice = view.findViewById(R.id.tvPurchasePrice);
                TextView tvPurchaseBalance = view.findViewById(R.id.tvPurchaseBalance);
                TextView tvPurchaseNewBalance = view.findViewById(R.id.tvPurchaseNewBalance);
                Button btnCancelPurchase = view.findViewById(R.id.btnCancelPurchase);
                Button btnConfirmPurchase = view.findViewById(R.id.btnConfirmPurchase);
                
                btnCancelPurchase.setText(getString(R.string.cancel));
                btnConfirmPurchase.setText(getString(R.string.confirm));
        
                NumberFormat format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
                format.setCurrency(Currency.getInstance("EUR"));
                
                double currentBalance = walletBalance;
                double price = course.getPrice();
                double newBalance = currentBalance - price;
                
                Log.v(TAG, String.format("Purchase details: balance=%.2f, price=%.2f, newBalance=%.2f", 
                        currentBalance, price, newBalance));
                
                if (currentBalance < price) {
                    Log.w(TAG, "Insufficient funds for purchase: " + currentBalance + " < " + price);
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(requireContext(), insufficientFundsMessage, 
                                       Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
        
                tvPurchaseCourseName.setText(course.getName());
                tvPurchasePrice.setText(format.format(price));
                tvPurchaseBalance.setText(format.format(currentBalance));
                tvPurchaseNewBalance.setText(format.format(newBalance));
                
                Dialog confirmationDialog = new MaterialAlertDialogBuilder(requireContext())
                        .setView(view)
                        .setCancelable(true)
                        .create();
        
                btnCancelPurchase.setOnClickListener(v -> {
                    Log.d(TAG, "Purchase cancelled by user");
                    confirmationDialog.dismiss();
                });
                
                btnConfirmPurchase.setOnClickListener(v -> {
                    Log.d(TAG, "Purchase confirmed, proceeding with transaction");
                    confirmationDialog.dismiss();
                    purchaseCourse(firebaseUser.getUid(), course.getId(), course.getPrice());
                });
        
                confirmationDialog.show();
                Log.i(TAG, "Purchase confirmation dialog displayed");
                
            } catch (Exception e) {
                Log.e(TAG, "Error showing purchase confirmation dialog: " + e.getMessage(), e);
                Toast.makeText(requireContext(), getString(R.string.dialog_error), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void purchaseCourse(String userId, String courseId, double price) {
        Log.i(TAG, String.format("Purchasing course: userId=%s, courseId=%s, price=%.2f", 
                userId, courseId, price));
        showLoading(true);
        
        // Get a reference to the Firestore database
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Run as a transaction to ensure both user and course are updated atomically
        db.runTransaction(transaction -> {
            try {
                // Get both document references and read them at the start of transaction
                DocumentReference userRef = db.collection("users").document(userId);
                DocumentReference courseRef = db.collection("marketplace").document(courseId);
                
                DocumentSnapshot userSnapshot = transaction.get(userRef);
                DocumentSnapshot courseSnapshot = transaction.get(courseRef);
                
                // Process the retrieved data
                if (!userSnapshot.exists()) {
                    Log.e(TAG, "User not found in transaction");
                    return "User not found";
                }
                
                // Get the current wallet balance
                Double currentBalance = userSnapshot.getDouble("wallet");
                if (currentBalance == null) {
                    currentBalance = 0.0;
                }
                
                // Check if user has enough funds
                if (currentBalance < price) {
                    Log.e(TAG, "Insufficient funds: " + currentBalance + " < " + price);
                    return "Insufficient funds";
                }
                
                // Get the owned courses array
                List<String> ownedCourses = (List<String>) userSnapshot.get("ownedCourses");
                if (ownedCourses == null) {
                    ownedCourses = new ArrayList<>();
                }
                
                // Check if the user already owns this course
                if (ownedCourses.contains(courseId)) {
                    Log.e(TAG, "User already owns this course");
                    return "User already owns this course";
                }
                
                // Add the course ID to the owned courses array
                ownedCourses.add(courseId);
                
                // Calculate the new balance
                double newBalance = currentBalance - price;
                
                // Update the user document
                transaction.update(userRef, "wallet", newBalance);
                transaction.update(userRef, "ownedCourses", ownedCourses);
                
                // Update course statistics if the course exists
                if (courseSnapshot.exists()) {
                    transaction.update(courseRef, "statistics.purchasesToday", FieldValue.increment(1));
                    transaction.update(courseRef, "statistics.totalPurchases", FieldValue.increment(1));
                }
                
                // Return null to indicate success
                return null;
            } catch (Exception e) {
                Log.e(TAG, "Transaction failed with exception: " + e.getMessage(), e);
                return "Transaction error: " + e.getMessage();
            }
        }).addOnSuccessListener(result -> {
            showLoading(false);
            
            if (result == null) {
                // Null result means success
                Log.i(TAG, "Purchase completed successfully");
                Toast.makeText(requireContext(), getString(R.string.purchase_successful), Toast.LENGTH_SHORT).show();
                
                // Update the UI
                configureBuyButton();
                
                // Notify listener
                if (purchaseCompletedListener != null) {
                    purchaseCompletedListener.onPurchaseCompleted();
                }
                
                // Navigate to the Owned tab after purchase
                try {
                    if (getActivity() != null) {
                        Log.i(TAG, "Navigating to Owned tab after successful purchase");
                        
                        // Already notified the listener above, no need to do it twice
                        
                        // Create a new StoreFragment instance that starts with the Owned tab
                        StoreFragment storeFragment = StoreFragment.newInstance(1); // 1 is the index for the Owned tab
                        
                        // Replace the current fragment with this new one
                        getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.frame_layout, storeFragment)
                            .commit();
                    }
                    
                    // Close the dialog
                    dismiss();
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating to Owned tab: " + e.getMessage(), e);
                    dismiss(); // Fallback - just dismiss the dialog
                }
            } else {
                // Non-null result means error with message
                Log.e(TAG, "Purchase failed: " + result);
                Toast.makeText(requireContext(), 
                             getString(R.string.purchase_failed) + ": " + result, 
                             Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Purchase failed with exception: " + e.getMessage(), e);
            showLoading(false);
            Toast.makeText(requireContext(), 
                         getString(R.string.purchase_failed) + ": " + e.getMessage(), 
                         Toast.LENGTH_SHORT).show();
        });
    }

    private void showRelatedCourseDetail(Course relatedCourse) {
        Log.i(TAG, "Opening related course: " + relatedCourse.getName() + " (" + relatedCourse.getId() + ")");
        dismiss();

        CourseDetailDialog dialog = CourseDetailDialog.newInstance(relatedCourse.getId());
        dialog.setPurchaseCompletedListener(purchaseCompletedListener);
        dialog.show(getParentFragmentManager(), "RelatedCourseDetail");
    }

    public void setPurchaseCompletedListener(PurchaseCompletedListener listener) {
        Log.d(TAG, "Setting purchase completed listener");
        this.purchaseCompletedListener = listener;
    }
    
    /**
     * Interface for course ownership check callback
     */
    public interface OnCourseOwnershipCheckListener {
        void onResult(boolean isOwned);
    }
    
    /**
     * Interface for wallet balance callback
     */
    public interface OnWalletBalanceListener {
        void onResult(double balance);
    }

    private static class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
        private static final String TAG = "ReviewAdapter";
        private List<Course.Review> reviews;

        public ReviewAdapter(List<Course.Review> reviews) {
            Log.v(TAG, "Creating adapter with " + (reviews != null ? reviews.size() : 0) + " reviews");
            this.reviews = reviews;
        }

        @NonNull
        @Override
        public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Log.v(TAG, "Creating review view holder");
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_review, parent, false);
            return new ReviewViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
            Course.Review review = reviews.get(position);
            Log.v(TAG, "Binding review at position " + position + ": " + review.getUser());
            holder.bind(review);
        }

        @Override
        public int getItemCount() {
            return reviews.size();
        }

        static class ReviewViewHolder extends RecyclerView.ViewHolder {
            TextView tvReviewerName;
            RatingBar rbReviewRating;
            TextView tvReviewComment;

            public ReviewViewHolder(@NonNull View itemView) {
                super(itemView);
                tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
                rbReviewRating = itemView.findViewById(R.id.rbReviewRating);
                tvReviewComment = itemView.findViewById(R.id.tvReviewComment);
            }

            public void bind(Course.Review review) {
                tvReviewerName.setText(review.getUser());
                rbReviewRating.setRating((float) review.getRating());
                tvReviewComment.setText(review.getComment());
            }
        }
    }

    private static class RelatedCourseAdapter extends RecyclerView.Adapter<RelatedCourseAdapter.RelatedCourseViewHolder> {
        private static final String TAG = "RelatedCourseAdapter";
        private List<Course> relatedCourses;
        private OnRelatedCourseClickListener listener;

        interface OnRelatedCourseClickListener {
            void onRelatedCourseClicked(Course course);
        }

        public RelatedCourseAdapter(List<Course> relatedCourses, OnRelatedCourseClickListener listener) {
            Log.v(TAG, "Creating adapter with " + (relatedCourses != null ? relatedCourses.size() : 0) + " related courses");
            this.relatedCourses = relatedCourses;
            this.listener = listener;
        }

        @NonNull
        @Override
        public RelatedCourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Log.v(TAG, "Creating related course view holder");
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_related_course, parent, false);
            return new RelatedCourseViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RelatedCourseViewHolder holder, int position) {
            Course course = relatedCourses.get(position);
            Log.v(TAG, "Binding related course at position " + position + ": " + course.getName());
            holder.bind(course);
        }

        @Override
        public int getItemCount() {
            return relatedCourses.size();
        }

        class RelatedCourseViewHolder extends RecyclerView.ViewHolder {
            ImageView ivRelatedCourseLogo;
            TextView tvRelatedCourseTitle;
            TextView tvRelatedCoursePrice;

            public RelatedCourseViewHolder(@NonNull View itemView) {
                super(itemView);
                ivRelatedCourseLogo = itemView.findViewById(R.id.ivRelatedCourseLogo);
                tvRelatedCourseTitle = itemView.findViewById(R.id.tvRelatedCourseTitle);
                tvRelatedCoursePrice = itemView.findViewById(R.id.tvRelatedCoursePrice);

                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        Log.d(TAG, "Related course clicked at position " + position);
                        listener.onRelatedCourseClicked(relatedCourses.get(position));
                    }
                });
            }

            public void bind(Course course) {
                tvRelatedCourseTitle.setText(course.getName());

                NumberFormat format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
                format.setCurrency(Currency.getInstance("EUR"));
                tvRelatedCoursePrice.setText(format.format(course.getPrice()));

                // Set color based on course module for consistency
                int colorCode;
                if (course.getRelatedModule() != null) {
                    int hash = course.getRelatedModule().hashCode();
                    int[] colors = {
                        0xFF4CAF50, // Green
                        0xFF2196F3, // Blue
                        0xFFFF9800, // Orange
                        0xFF9C27B0, // Purple
                        0xFFE91E63  // Pink
                    };
                    colorCode = colors[Math.abs(hash) % colors.length];
                } else {
                    colorCode = 0xFF2196F3; // Blue placeholder
                }
                
                ivRelatedCourseLogo.setBackgroundColor(colorCode);
            }
        }
    }
}