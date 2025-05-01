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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdevelopmentprojectfinal.R;
import com.example.appdevelopmentprojectfinal.model.Course;
import com.example.appdevelopmentprojectfinal.model.Module;
import com.example.appdevelopmentprojectfinal.model.User;
import com.example.appdevelopmentprojectfinal.utils.DataManager;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

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
    private ImageView ivPlayButton;
    private FrameLayout videoContainer;
    private ImageView btnCollapsePreview;
    
    private RecyclerView rvReviews;
    private TextView tvNoReviews;
    private RecyclerView rvRelatedCourses;
    private Button btnCancel;
    private Button btnBuy;
    private View loadingView;

    private ReviewAdapter reviewAdapter;
    private RelatedCourseAdapter relatedCourseAdapter;

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
        ivPlayButton = view.findViewById(R.id.ivPlayButton);
        videoContainer = view.findViewById(R.id.videoContainer);
        btnCollapsePreview = view.findViewById(R.id.btnCollapsePreview);
        
        rvReviews = view.findViewById(R.id.rvReviews);
        tvNoReviews = view.findViewById(R.id.tvNoReviews);
        rvRelatedCourses = view.findViewById(R.id.rvRelatedCourses);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnBuy = view.findViewById(R.id.btnBuy);
        loadingView = view.findViewById(R.id.loadingView);
        
        setupPreviewExpansion();
        Log.d(TAG, "Views initialized successfully");
    }
    
    private void loadCourseDetails() {
        Log.i(TAG, "Loading course details for courseId: " + courseId);
        showLoading(true);
        
        MarketplaceFirestoreManager.getInstance().loadCourseById(courseId, new MarketplaceFirestoreManager.OnCourseLoadedListener() {
            @Override
            public void onCourseLoaded(Course loadedCourse) {
                Log.i(TAG, "Course loaded successfully: " + loadedCourse.getName());
                course = loadedCourse;
                displayCourseDetails();
                setupButtons();
                showLoading(false);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading course: " + errorMessage);
                Toast.makeText(requireContext(), getString(R.string.course_not_found), Toast.LENGTH_SHORT).show();
                showLoading(false);
                dismiss();
            }
        });
    }
    
    private void showLoading(boolean isLoading) {
        Log.v(TAG, "Loading state changed: " + isLoading);
        if (isLoading) {
            loadingView.setVisibility(View.VISIBLE);
        } else {
            loadingView.setVisibility(View.GONE);
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
        Module module = DataManager.getInstance().getModuleByCode(course.getRelatedModule());
        if (module != null) {
            String moduleInfo = String.format("%s: %s", module.getCode(), module.getName());
            Log.v(TAG, "Setting module info: " + moduleInfo);
            tvModuleInfo.setText(moduleInfo);
        } else {
            Log.v(TAG, "Module not found, using module code only: " + course.getRelatedModule());
            tvModuleInfo.setText(course.getRelatedModule());
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
        
        MarketplaceFirestoreManager.getInstance().loadCoursesByModule(course.getRelatedModule(), new MarketplaceFirestoreManager.OnCoursesLoadedListener() {
            @Override
            public void onCoursesLoaded(List<Course> courses) {
                Log.d(TAG, "Loaded " + courses.size() + " courses for related module");
                List<Course> relatedCourses = new ArrayList<>();
                for (Course relatedCourse : courses) {
                    if (!relatedCourse.getId().equals(course.getId())) {
                        relatedCourses.add(relatedCourse);
                        Log.v(TAG, "Added related course: " + relatedCourse.getName());
                    }
                }
                
                if (relatedCourses.isEmpty()) {
                    Log.w(TAG, "No related courses found after filtering");
                    rvRelatedCourses.setVisibility(View.GONE);
                } else {
                    Log.i(TAG, "Displaying " + relatedCourses.size() + " related courses");
                    relatedCourseAdapter = new RelatedCourseAdapter(relatedCourses, CourseDetailDialog.this::showRelatedCourseDetail);
                    rvRelatedCourses.setAdapter(relatedCourseAdapter);
                    rvRelatedCourses.setVisibility(View.VISIBLE);
                }
                
                showRelatedCoursesLoading(false);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading related courses: " + errorMessage);
                rvRelatedCourses.setVisibility(View.GONE);
                showRelatedCoursesLoading(false);
            }
        });
    }
    
    private void showRelatedCoursesLoading(boolean isLoading) {
        Log.v(TAG, "Related courses loading state: " + isLoading);
        // Could add a specific loading indicator for related courses if needed
    }
    
    private void configureBuyButton() {
        Log.d(TAG, "Configuring buy button");
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        format.setCurrency(Currency.getInstance("EUR"));
        String formattedPrice = format.format(course.getPrice());
        
        Log.v(TAG, "Setting buy button price: " + formattedPrice);
        btnBuy.setText(getString(R.string.buy_price, formattedPrice));
        btnBuy.setEnabled(true);
        
        User currentUser = DataManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            if (currentUser.ownsModule(course.getId())) {
                Log.i(TAG, "User already owns this course, disabling buy button");
                btnBuy.setText(getString(R.string.owned));
                btnBuy.setEnabled(false);
            } else {
                Log.v(TAG, "Course is available for purchase");
            }
        } else {
            Log.w(TAG, "Current user is null, cannot check ownership status");
        }
    }

    private void setupButtons() {
        Log.d(TAG, "Setting up button actions");
        btnCancel.setOnClickListener(v -> {
            Log.d(TAG, "Cancel button clicked, dismissing dialog");
            dismiss();
        });

        btnBuy.setOnClickListener(v -> {
            Log.d(TAG, "Buy button clicked");
            User currentUser = DataManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "Cannot purchase: current user is null");
                Toast.makeText(requireContext(), getString(R.string.user_not_found), Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUser.ownsModule(course.getId())) {
                Log.w(TAG, "User already owns course: " + course.getId());
                Toast.makeText(requireContext(), getString(R.string.already_owned), Toast.LENGTH_SHORT).show();
                return;
            }

            showPurchaseConfirmationDialog();
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
        });
        
        ivPlayButton.setOnClickListener(v -> {
            Log.d(TAG, "Play button clicked");
            playVideo();
        });
    }
    
    private void prepareExpandedPreview() {
        Log.d(TAG, "Preparing expanded preview content");
        
        if (course == null || course.getContent() == null || course.getContent().getChapters() == null 
                || course.getContent().getChapters().isEmpty()) {
            Log.e(TAG, "Cannot prepare expanded preview: course content is missing");
            expandedPreviewContainer.setVisibility(View.GONE);
            return;
        }
            
        Course.Chapter firstChapter = course.getContent().getChapters().get(0);
        Log.i(TAG, "Using first chapter for preview: " + firstChapter.getTitle());
        tvExpandedChapterTitle.setText(firstChapter.getTitle());
        
        StringBuilder contentBuilder = new StringBuilder();
        String videoTitle = "";
        String videoUrl = null;
        
        if (firstChapter.getItems() != null) {
            Log.v(TAG, "Processing " + firstChapter.getItems().size() + " items in chapter");
            for (Course.ContentItem item : firstChapter.getItems()) {
                String itemType = item.getType();
                if (itemType == null) {
                    Log.w(TAG, "Skipping item with null type");
                    continue;
                }
                
                Log.v(TAG, "Processing item of type: " + itemType);
                switch (itemType) {
                    case "text":
                        if (item.getContent() != null) {
                            contentBuilder.append(item.getContent()).append("\n\n");
                            Log.v(TAG, "Added text content (" + item.getContent().length() + " chars)");
                        } else {
                            Log.w(TAG, "Skipping text item with null content");
                        }
                        break;
                        
                    case "video":
                        if (item.getTitle() != null) {
                            videoTitle = item.getTitle();
                            Log.v(TAG, "Found video title: " + videoTitle);
                        }
                        
                        if (item.getUrl() != null) {
                            videoUrl = item.getUrl();
                            Log.v(TAG, "Found video URL: " + videoUrl);
                        } else {
                            Log.w(TAG, "Video item has null URL");
                        }
                        break;
                        
                    default:
                        Log.w(TAG, "Unhandled content item type: " + itemType);
                        break;
                }
            }
        } else {
            Log.w(TAG, "Chapter has no content items");
        }
        
        if (!videoTitle.isEmpty()) {
            Log.v(TAG, "Setting video title: " + videoTitle);
            tvVideoTitle.setText(videoTitle);
            tvVideoTitle.setVisibility(View.VISIBLE);
        } else {
            Log.v(TAG, "No video title available, hiding title view");
            tvVideoTitle.setVisibility(View.GONE);
        }
        
        if (videoUrl != null) {
            Log.d(TAG, "Setting up video with URL: " + videoUrl);
            videoContainer.setVisibility(View.VISIBLE);
            setUpVideoView(videoUrl);
        } else {
            Log.w(TAG, "No video URL available, hiding video container");
            videoContainer.setVisibility(View.GONE);
        }
        
        if (contentBuilder.length() > 0) {
            Log.v(TAG, "Setting expanded content text (" + contentBuilder.length() + " chars)");
            tvExpandedContent.setText(contentBuilder.toString());
            tvExpandedContent.setVisibility(View.VISIBLE);
        } else {
            Log.w(TAG, "No content text available, hiding content view");
            tvExpandedContent.setVisibility(View.GONE);
        }
        
        Log.d(TAG, "Expanded preview prepared successfully");
    }
    
    private void setUpVideoView(String videoUrl) {
        Log.i(TAG, "Setting up video view for URL: " + videoUrl);
        
        try {
            long startTime = System.currentTimeMillis();
            String resourcePath = findLocalVideoResourceForUrl(videoUrl);
            Log.d(TAG, "Resolved video resource path: " + resourcePath);
            
            videoView.setVideoURI(Uri.parse(resourcePath));
            Log.v(TAG, "Video URI set successfully");
            
            videoView.setOnCompletionListener(mp -> {
                Log.d(TAG, "Video playback completed");
                ivPlayButton.setVisibility(View.VISIBLE);
            });
            
            setupVideoListeners();
            
            ivPlayButton.setVisibility(View.VISIBLE);
            long endTime = System.currentTimeMillis();
            Log.d(TAG, "Video setup completed in " + (endTime - startTime) + "ms");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set up video view: " + e.getMessage(), e);
            videoContainer.setVisibility(View.GONE);
        }
    }
    
    private String findLocalVideoResourceForUrl(String videoUrl) {
        String defaultResourcePath = "android.resource://" + requireContext().getPackageName() + "/raw/course_preview_demo";
        Log.d(TAG, "Finding local video resource for URL: " + videoUrl);
        
        if (videoUrl == null || videoUrl.isEmpty()) {
            Log.w(TAG, "Video URL is null or empty, using default resource");
            return defaultResourcePath;
        }
        
        try {
            String filename = getFilenameFromUrl(videoUrl);
            if (filename != null && !filename.isEmpty()) {
                String resourceName = convertToResourceName(filename);
                Log.v(TAG, "Looking for resource name: " + resourceName);
                
                int resourceId = findResourceId(resourceName);
                
                if (resourceId != 0) {
                    String resourcePath = "android.resource://" + requireContext().getPackageName() + "/" + resourceId;
                    Log.i(TAG, "Found matching resource: " + resourceName + " (id: " + resourceId + ")");
                    return resourcePath;
                } else {
                    Log.w(TAG, "Resource not found: " + resourceName);
                }
            } else {
                Log.w(TAG, "Could not extract filename from URL");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding local video resource: " + e.getMessage(), e);
        }
        
        Log.d(TAG, "Using default video resource");
        return defaultResourcePath;
    }
    
    private String getFilenameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        int lastSlashIndex = url.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < url.length() - 1) {
            String filename = url.substring(lastSlashIndex + 1);
            Log.v(TAG, "Extracted filename from URL: " + filename);
            return filename;
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
        
        String resourceName = filename.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        Log.v(TAG, "Converted to resource name: " + resourceName);
        return resourceName;
    }
    
    private int findResourceId(String resourceName) {
        android.content.res.Resources resources = requireContext().getResources();
        String packageName = requireContext().getPackageName();
        
        int resourceId = resources.getIdentifier(resourceName, "raw", packageName);
        if (resourceId != 0) {
            Log.v(TAG, "Found resource in 'raw' folder: " + resourceId);
            return resourceId;
        }
        
        resourceId = resources.getIdentifier(resourceName, "drawable", packageName);
        if (resourceId != 0) {
            Log.v(TAG, "Found resource in 'drawable' folder: " + resourceId);
            return resourceId;
        }
        
        Log.v(TAG, "Resource not found in any folder: " + resourceName);
        return 0;
    }
    
    private void playVideo() {
        Log.i(TAG, "Starting video playback");
        ivPlayButton.setVisibility(View.GONE);
        
        try {
            long startTime = System.currentTimeMillis();
            
            videoView.requestFocus();
            videoView.start();
            
            long endTime = System.currentTimeMillis();
            Log.d(TAG, "Video playback started in " + (endTime - startTime) + "ms");
        } catch (Exception e) {
            Log.e(TAG, "Error playing video: " + e.getMessage(), e);
            Toast.makeText(requireContext(), 
                    getString(R.string.video_loading_error_message, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
            ivPlayButton.setVisibility(View.VISIBLE);
        }
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

    private void showPurchaseConfirmationDialog() {
        Log.i(TAG, "Showing purchase confirmation dialog");
        
        User currentUser = DataManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Cannot show purchase dialog: current user is null");
            Toast.makeText(requireContext(), getString(R.string.user_not_found), Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (course == null) {
            Log.e(TAG, "Cannot show purchase dialog: course is null");
            return;
        }

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
            
            double currentBalance = currentUser.getWallet();
            double price = course.getPrice();
            double newBalance = currentBalance - price;
            
            Log.v(TAG, String.format("Purchase details: balance=%.2f, price=%.2f, newBalance=%.2f", 
                    currentBalance, price, newBalance));
            
            if (currentBalance < price) {
                Log.w(TAG, "Insufficient funds for purchase: " + currentBalance + " < " + price);
                Toast.makeText(requireContext(), getString(R.string.insufficient_funds), 
                               Toast.LENGTH_SHORT).show();
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
                purchaseCourse(currentUser.getEmail(), course.getId(), course.getPrice());
            });
    
            confirmationDialog.show();
            Log.i(TAG, "Purchase confirmation dialog displayed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing purchase confirmation dialog: " + e.getMessage(), e);
            Toast.makeText(requireContext(), getString(R.string.dialog_error), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void purchaseCourse(String userId, String courseId, double price) {
        Log.i(TAG, String.format("Purchasing course: userId=%s, courseId=%s, price=%.2f", 
                userId, courseId, price));
        showLoading(true);
        
        MarketplaceFirestoreManager.getInstance().purchaseCourse(userId, courseId, price, new MarketplaceFirestoreManager.OnPurchaseListener() {
            @Override
            public void onPurchaseSuccess() {
                Log.i(TAG, "Purchase completed successfully");
                showLoading(false);
                Toast.makeText(requireContext(), getString(R.string.purchase_successful), Toast.LENGTH_SHORT).show();
                
                if (purchaseCompletedListener != null) {
                    Log.d(TAG, "Notifying purchase completion listener");
                    purchaseCompletedListener.onPurchaseCompleted();
                }
                
                dismiss();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Purchase failed: " + errorMessage);
                showLoading(false);
                Toast.makeText(requireContext(), getString(R.string.purchase_failed) + ": " + errorMessage, 
                              Toast.LENGTH_SHORT).show();
            }
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