package com.example.appdevelopmentprojectfinal.marketplace;

import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
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
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdevelopmentprojectfinal.R;
import com.example.appdevelopmentprojectfinal.model.Course;
import com.example.appdevelopmentprojectfinal.utils.YouTubeHelper;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CourseFullContentDialog extends DialogFragment {

    private static final String TAG = "CourseFullContent";
    private static final String ARG_COURSE_ID = "courseId";

    private String courseId;
    private Course course;

    // UI components
    private ImageView ivCourseImage;
    private CollapsingToolbarLayout collapsingToolbar;
    private Toolbar toolbar;
    private TextView tvModuleInfo;
    private TextView tvAuthor;
    private TextView tvDescription;
    private RatingBar ratingBar;
    private TextView tvRating;
    private RecyclerView rvChapters;
    private TextView tvNoChapters;
    private RecyclerView rvReviews;
    private TextView tvNoReviews;
    private Button btnAddReview;
    private View loadingView;

    // Lesson content section
    private LinearLayout lessonContentSection;
    private TextView tvLessonTitle;
    private TextView tvVideoTitle;
    private TextView tvLessonContent;
    private VideoView videoView;
    private WebView webViewYoutube;
    private ImageView ivPlayButton;
    private FrameLayout videoContainer;
    private Button btnBackToChapters;

    // Adapters
    private ChapterAdapter chapterAdapter;
    private ReviewAdapter reviewAdapter;

    // Firestore
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static CourseFullContentDialog newInstance(String courseId) {
        Log.d(TAG, "Creating new full content dialog instance for courseId: " + courseId);
        CourseFullContentDialog dialog = new CourseFullContentDialog();
        Bundle args = new Bundle();
        args.putString(ARG_COURSE_ID, courseId);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_AppDevelopmentProjectFinal_FullScreenDialog);
        
        if (getArguments() != null) {
            courseId = getArguments().getString(ARG_COURSE_ID);
            Log.d(TAG, "Retrieved courseId from arguments: " + courseId);
        } else {
            Log.w(TAG, "No arguments provided to CourseFullContentDialog");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_course_full_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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
        
        // Toolbar and header section
        ivCourseImage = view.findViewById(R.id.ivCourseImage);
        collapsingToolbar = view.findViewById(R.id.collapsingToolbar);
        toolbar = view.findViewById(R.id.toolbar);
        tvModuleInfo = view.findViewById(R.id.tvModuleInfo);
        tvAuthor = view.findViewById(R.id.tvAuthor);
        tvDescription = view.findViewById(R.id.tvDescription);
        
        // Rating section
        ratingBar = view.findViewById(R.id.ratingBar);
        tvRating = view.findViewById(R.id.tvRating);
        
        // Chapter section
        rvChapters = view.findViewById(R.id.rvChapters);
        tvNoChapters = view.findViewById(R.id.tvNoChapters);
        rvChapters.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Lesson content section
        lessonContentSection = view.findViewById(R.id.lessonContentSection);
        tvLessonTitle = view.findViewById(R.id.tvLessonTitle);
        tvVideoTitle = view.findViewById(R.id.tvVideoTitle);
        tvLessonContent = view.findViewById(R.id.tvLessonContent);
        videoView = view.findViewById(R.id.videoView);
        webViewYoutube = view.findViewById(R.id.webViewYoutube);
        ivPlayButton = view.findViewById(R.id.ivPlayButton);
        videoContainer = view.findViewById(R.id.videoContainer);
        btnBackToChapters = view.findViewById(R.id.btnBackToChapters);
        
        // Reviews section
        rvReviews = view.findViewById(R.id.rvReviews);
        tvNoReviews = view.findViewById(R.id.tvNoReviews);
        btnAddReview = view.findViewById(R.id.btnAddReview);
        
        // Loading view
        loadingView = view.findViewById(R.id.loadingView);
        
        // Setup back button
        btnBackToChapters.setOnClickListener(v -> {
            lessonContentSection.setVisibility(View.GONE);
            
            // Stop any running video
            if (videoView.isPlaying()) {
                videoView.stopPlayback();
            }
            
            // Also reset YouTube player if visible
            if (webViewYoutube.getVisibility() == View.VISIBLE) {
                webViewYoutube.loadUrl("about:blank");
            }
        });
        
        // Setup play button
        ivPlayButton.setOnClickListener(v -> playVideo());
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

    private void displayCourseDetails() {
        Log.i(TAG, "Displaying full details for course: " + course.getName());
        
        collapsingToolbar.setTitle(course.getName());
        setCourseImage();
        setModuleInfo();
        tvAuthor.setText(getString(R.string.by_author, course.getAuthor()));
        tvDescription.setText(course.getDescription());
        setRatingInfo();
        setChaptersSection();
        setReviewsSection();
        setupReviewButton();
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

    private void setChaptersSection() {
        if (course.getContent() == null || course.getContent().getChapters() == null || 
            course.getContent().getChapters().isEmpty()) {
            Log.w(TAG, "No chapters available for this course");
            rvChapters.setVisibility(View.GONE);
            tvNoChapters.setVisibility(View.VISIBLE);
            return;
        }
        
        List<Course.Chapter> chapters = course.getContent().getChapters();
        Log.i(TAG, "Displaying " + chapters.size() + " chapters");
        
        chapterAdapter = new ChapterAdapter(chapters);
        rvChapters.setAdapter(chapterAdapter);
        rvChapters.setVisibility(View.VISIBLE);
        tvNoChapters.setVisibility(View.GONE);
    }

    private void setReviewsSection() {
        if (course.getReviews() == null || course.getReviews().isEmpty()) {
            Log.w(TAG, "No reviews available for this course");
            rvReviews.setVisibility(View.GONE);
            tvNoReviews.setVisibility(View.VISIBLE);
            return;
        }
        
        Log.i(TAG, "Displaying " + course.getReviews().size() + " reviews");
        reviewAdapter = new ReviewAdapter(course.getReviews());
        rvReviews.setAdapter(reviewAdapter);
        rvReviews.setVisibility(View.VISIBLE);
        tvNoReviews.setVisibility(View.GONE);
    }

    private void setupReviewButton() {
        Log.d(TAG, "Setting up review button");
        
        btnAddReview.setOnClickListener(v -> {
            Log.d(TAG, "Add review button clicked");
            
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(requireContext(), getString(R.string.login_to_review), Toast.LENGTH_SHORT).show();
                return;
            }
            
            CourseDetailDialog detailDialog = CourseDetailDialog.newInstance(courseId);
            detailDialog.show(getParentFragmentManager(), "CourseDetail");
            dismiss(); // Close the full content dialog
        });
    }

    private void showLessonContent(Course.Chapter chapter, Course.ContentItem lesson) {
        Log.i(TAG, "Showing lesson content: " + lesson.getTitle());
        
        lessonContentSection.setVisibility(View.VISIBLE);
        
        // Set lesson title
        String lessonTitle = lesson.getTitle();
        if (lessonTitle != null && !lessonTitle.isEmpty()) {
            tvLessonTitle.setText(lessonTitle);
            tvLessonTitle.setVisibility(View.VISIBLE);
        } else {
            tvLessonTitle.setText(chapter.getTitle());
            tvLessonTitle.setVisibility(View.VISIBLE);
        }
        
        // Set video either from lesson or fallback to preview
        String videoUrl = lesson.getUrl();
        
        // If lesson doesn't have a video, try to get one from the preview
        if ((videoUrl == null || videoUrl.isEmpty()) && course.getContent() != null && 
            course.getContent().getPreview() != null && 
            course.getContent().getPreview().getItems() != null) {
            
            Log.d(TAG, "Lesson has no video, checking preview for video");
            
            // Look for video in preview items
            for (Course.ContentItem previewItem : course.getContent().getPreview().getItems()) {
                if ("video".equals(previewItem.getType()) && previewItem.getUrl() != null && !previewItem.getUrl().isEmpty()) {
                    videoUrl = previewItem.getUrl();
                    Log.d(TAG, "Found video in preview: " + videoUrl);
                    break;
                }
            }
        }
        
        // If we found a video URL, show the video
        if (videoUrl != null && !videoUrl.isEmpty()) {
            videoContainer.setVisibility(View.VISIBLE);
            setupVideo(videoUrl, lesson.getTitle());
        } else {
            // If no video was found in either lesson or preview, hide the video container
            Log.d(TAG, "No video URL found in lesson or preview");
            videoContainer.setVisibility(View.GONE);
        }
        
        // Set lesson content
        String content = lesson.getContent();
        if (content != null && !content.isEmpty()) {
            tvLessonContent.setText(content);
            tvLessonContent.setVisibility(View.VISIBLE);
        } else {
            tvLessonContent.setVisibility(View.GONE);
        }
    }
    

    private void setupVideo(String videoUrl, String videoTitle) {
        Log.i(TAG, "Setting up video with URL: " + videoUrl);
        
        if (videoTitle != null && !videoTitle.isEmpty()) {
            tvVideoTitle.setText(videoTitle);
            tvVideoTitle.setVisibility(View.VISIBLE);
        } else {
            tvVideoTitle.setVisibility(View.GONE);
        }
        
        try {
            if (videoUrl == null || videoUrl.trim().isEmpty()) {
                Log.e(TAG, "Video URL is null or empty");
                videoContainer.setVisibility(View.GONE);
                return;
            }
            
            String trimmedUrl = videoUrl.trim();
            
            if (YouTubeHelper.isYoutubeUrl(trimmedUrl)) {
                Log.d(TAG, "Setting up YouTube video: " + trimmedUrl);
                videoView.setVisibility(View.GONE);
                webViewYoutube.setVisibility(View.VISIBLE);
                
                String embedUrl = YouTubeHelper.convertToEmbedUrl(trimmedUrl);
                setupYouTubePlayer(embedUrl);
            } else {
                Log.d(TAG, "Setting up direct video URL: " + trimmedUrl);
                webViewYoutube.setVisibility(View.GONE);
                videoView.setVisibility(View.VISIBLE);
                
                // Use the URL directly
                Uri videoUri = Uri.parse(trimmedUrl);
                videoView.setVideoURI(videoUri);
                setupVideoListeners();
            }
            
            ivPlayButton.setVisibility(View.VISIBLE);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to set up video view: " + e.getMessage(), e);
            videoContainer.setVisibility(View.GONE);
        }
    }

    private void setupYouTubePlayer(String youtubeUrl) {
        Log.d(TAG, "Setting up YouTube player for URL: " + youtubeUrl);
        YouTubeHelper.loadYoutubeVideo(webViewYoutube, youtubeUrl, false);
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

    private void playVideo() {
        Log.i(TAG, "Starting video playback");
        ivPlayButton.setVisibility(View.GONE);
        
        try {
            if (webViewYoutube.getVisibility() == View.VISIBLE) {
                String webViewUrl = webViewYoutube.getUrl();
                if (webViewUrl != null && !webViewUrl.equals("about:blank")) {
                    Log.i(TAG, "Playing YouTube video");
                    YouTubeHelper.loadYoutubeVideo(webViewYoutube, webViewUrl, true);
                }
            } else {
                videoView.requestFocus();
                videoView.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing video: " + e.getMessage(), e);
            Toast.makeText(requireContext(), 
                    getString(R.string.video_loading_error_message, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
            ivPlayButton.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean isLoading) {
        Log.v(TAG, "Loading state changed: " + isLoading);
        if (loadingView != null) {
            loadingView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Chapter adapter for displaying course chapters
     */
    private class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {
        
        private List<Course.Chapter> chapters;
        private int expandedPosition = -1;
        
        public ChapterAdapter(List<Course.Chapter> chapters) {
            this.chapters = chapters;
        }
        
        @NonNull
        @Override
        public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chapter, parent, false);
            return new ChapterViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
            Course.Chapter chapter = chapters.get(position);
            holder.bind(chapter, position);
        }
        
        @Override
        public int getItemCount() {
            return chapters.size();
        }
        
        private class ChapterViewHolder extends RecyclerView.ViewHolder {
            TextView tvChapterTitle;
            ImageView ivExpandCollapse;
            LinearLayout lessonsContainer;
            RecyclerView rvLessons;
            
            public ChapterViewHolder(@NonNull View itemView) {
                super(itemView);
                tvChapterTitle = itemView.findViewById(R.id.tvChapterTitle);
                ivExpandCollapse = itemView.findViewById(R.id.ivExpandCollapse);
                lessonsContainer = itemView.findViewById(R.id.lessonsContainer);
                rvLessons = itemView.findViewById(R.id.rvLessons);
                
                // Set up recycler view for lessons
                rvLessons.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                
                // Set click listener for expanding/collapsing
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) return;
                    
                    // Toggle expanded state
                    if (expandedPosition == position) {
                        // Collapse if already expanded
                        expandedPosition = -1;
                        lessonsContainer.setVisibility(View.GONE);
                        ivExpandCollapse.setImageResource(android.R.drawable.arrow_down_float);
                    } else {
                        // Collapse previously expanded item
                        if (expandedPosition != -1) {
                            notifyItemChanged(expandedPosition);
                        }
                        
                        // Expand current item
                        expandedPosition = position;
                        lessonsContainer.setVisibility(View.VISIBLE);
                        ivExpandCollapse.setImageResource(android.R.drawable.arrow_up_float);
                        
                        // Set up lessons adapter
                        Course.Chapter chapter = chapters.get(position);
                        if (chapter.getItems() != null && !chapter.getItems().isEmpty()) {
                            LessonsAdapter adapter = new LessonsAdapter(chapter, chapter.getItems());
                            rvLessons.setAdapter(adapter);
                        }
                    }
                });
            }
            
            public void bind(Course.Chapter chapter, int position) {
                // Set chapter title
                String title = chapter.getTitle();
                if (title == null || title.isEmpty()) {
                    title = String.format(Locale.getDefault(), "Chapter %d", position + 1);
                }
                tvChapterTitle.setText(title);
                
                // Set expanded state
                boolean isExpanded = position == expandedPosition;
                lessonsContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                ivExpandCollapse.setImageResource(isExpanded ? 
                        android.R.drawable.arrow_up_float : 
                        android.R.drawable.arrow_down_float);
                
                // Set up lessons adapter if expanded
                if (isExpanded && chapter.getItems() != null && !chapter.getItems().isEmpty()) {
                    LessonsAdapter adapter = new LessonsAdapter(chapter, chapter.getItems());
                    rvLessons.setAdapter(adapter);
                }
            }
        }
    }

    /**
     * Lessons adapter for displaying chapter lessons
     */
    private class LessonsAdapter extends RecyclerView.Adapter<LessonsAdapter.LessonViewHolder> {
        
        private Course.Chapter parentChapter;
        private List<Course.ContentItem> lessons;
        
        public LessonsAdapter(Course.Chapter parentChapter, List<Course.ContentItem> lessons) {
            this.parentChapter = parentChapter;
            this.lessons = lessons;
        }
        
        @NonNull
        @Override
        public LessonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_lesson, parent, false);
            return new LessonViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull LessonViewHolder holder, int position) {
            Course.ContentItem lesson = lessons.get(position);
            holder.bind(lesson, position);
        }
        
        @Override
        public int getItemCount() {
            return lessons.size();
        }
        
        private class LessonViewHolder extends RecyclerView.ViewHolder {
            ImageView ivLessonType;
            TextView tvLessonTitle;
            
            public LessonViewHolder(@NonNull View itemView) {
                super(itemView);
                ivLessonType = itemView.findViewById(R.id.ivLessonType);
                tvLessonTitle = itemView.findViewById(R.id.tvLessonTitle);
                
                // Set click listener to view lesson
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) return;
                    
                    Course.ContentItem lesson = lessons.get(position);
                    showLessonContent(parentChapter, lesson);
                });
            }
            
            public void bind(Course.ContentItem lesson, int position) {
                // Set lesson title
                String title = lesson.getTitle();
                if (title == null || title.isEmpty()) {
                    title = String.format(Locale.getDefault(), "Lesson %d", position + 1);
                }
                tvLessonTitle.setText(title);
                
                // Set icon based on content type
                String type = lesson.getType();
                if (type != null) {
                    switch (type) {
                        case "video":
                            ivLessonType.setImageResource(android.R.drawable.ic_media_play);
                            break;
                        case "text":
                            ivLessonType.setImageResource(android.R.drawable.ic_menu_edit);
                            break;
                        case "image":
                            ivLessonType.setImageResource(android.R.drawable.ic_menu_gallery);
                            break;
                        default:
                            ivLessonType.setImageResource(android.R.drawable.ic_menu_info_details);
                            break;
                    }
                } else {
                    ivLessonType.setImageResource(android.R.drawable.ic_menu_info_details);
                }
            }
        }
    }

    /**
     * Review adapter for displaying course reviews
     */
    private static class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
        private List<Course.Review> reviews;

        public ReviewAdapter(List<Course.Review> reviews) {
            this.reviews = reviews;
        }

        @NonNull
        @Override
        public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_review, parent, false);
            return new ReviewViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
            Course.Review review = reviews.get(position);
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
}