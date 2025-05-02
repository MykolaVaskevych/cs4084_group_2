package com.example.appdevelopmentprojectfinal.marketplace;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdevelopmentprojectfinal.R;
import com.example.appdevelopmentprojectfinal.model.Module;
import com.example.appdevelopmentprojectfinal.model.Course;
import com.example.appdevelopmentprojectfinal.utils.AcademicDatabaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseCreationDialog extends DialogFragment {

    private static final String TAG = "CourseCreationDialog";
    private static final String ARG_COURSE_ID = "course_id";

    private EditText etCourseName, etCourseDescription, etCoursePrice, etCourseTags;
    private EditText etPreviewTitle, etPreviewContent, etPreviewVideoUrl;
    private AutoCompleteTextView spinnerModuleCode;
    private RecyclerView rvLessons;
    private TextView tvNoLessons;
    private Button btnAddLesson, btnCancel, btnSave;

    private LessonAdapter lessonAdapter;
    private List<MarketplaceCourse.Lesson> lessons = new ArrayList<>();
    private String courseId;
    private MarketplaceCourse course;
    private OnCourseCreatedListener listener;

    public interface OnCourseCreatedListener {
        void onCourseCreated();
    }

    public CourseCreationDialog() {
        // Required empty constructor
    }

    public static CourseCreationDialog newInstance(String courseId) {
        CourseCreationDialog dialog = new CourseCreationDialog();
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
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_course_creation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "Initializing CourseCreationDialog");

        // Initialize views
        etCourseName = view.findViewById(R.id.etCourseName);
        etCourseDescription = view.findViewById(R.id.etCourseDescription);
        etCoursePrice = view.findViewById(R.id.etCoursePrice);
        etCourseTags = view.findViewById(R.id.etCourseTags);
        spinnerModuleCode = view.findViewById(R.id.spinnerModuleCode);
        etPreviewTitle = view.findViewById(R.id.etPreviewTitle);
        etPreviewContent = view.findViewById(R.id.etPreviewContent);
        etPreviewVideoUrl = view.findViewById(R.id.etPreviewVideoUrl);
        rvLessons = view.findViewById(R.id.rvLessons);
        tvNoLessons = view.findViewById(R.id.tvNoLessons);
        btnAddLesson = view.findViewById(R.id.btnAddLesson);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);

        // Set up module spinner
        setupModuleSpinner();

        // Set up lessons recycler view
        lessonAdapter = new LessonAdapter(lessons);
        rvLessons.setAdapter(lessonAdapter);

        // Check if empty and show appropriate view
        checkEmptyLessons();

        // Set up button listeners
        btnAddLesson.setOnClickListener(v -> addNewLesson());
        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveCourse());

        // Set dialog title based on edit/create mode
        TextView tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        tvDialogTitle.setText(courseId != null ? "Edit Course" : "Create Course");

        // If we're editing a course, load it
        if (courseId != null) {
            loadCourse();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void setupModuleSpinner() {
        // Create a list of sample module codes manually since we don't have access to getAllModules with context
        List<String> moduleCodes = new ArrayList<>();
        moduleCodes.add("CS4084");
        moduleCodes.add("CS4116");
        moduleCodes.add("CS4141");
        moduleCodes.add("CS4013");
        moduleCodes.add("CS4222");
        moduleCodes.add("CS4815");
        
        // Create and set the adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            moduleCodes
        );
        spinnerModuleCode.setAdapter(adapter);
        
        // If editing a course, set the selected module
        if (course != null && course.getRelatedModule() != null) {
            spinnerModuleCode.setText(course.getRelatedModule(), false);
        }
    }

    private void loadCourse() {
        Log.i(TAG, "Loading course with ID: " + courseId);
        
        // Show loading state
        setFormEnabled(false);
        
        MarketplaceFirestoreManager.getInstance().loadCourseById(courseId, new MarketplaceFirestoreManager.OnCourseLoadedListener() {
            @Override
            public void onCourseLoaded(Course loadedCourse) {
                // Create a new MarketplaceCourse with the same data
                try {
                    course = new MarketplaceCourse();
                    course.setId(loadedCourse.getId());
                    course.setName(loadedCourse.getName());
                    course.setDescription(loadedCourse.getDescription());
                    course.setRelatedModule(loadedCourse.getRelatedModule());
                    course.setPrice(loadedCourse.getPrice());
                    course.setTags(loadedCourse.getTags());
                    course.setAuthor(loadedCourse.getAuthor());
                    course.setAverageRating(loadedCourse.getAverageRating());
                    
                    // Create simple empty statistics if needed
                    MarketplaceCourse.CourseStatistics stats = new MarketplaceCourse.CourseStatistics();
                    stats.setTotalPurchases(0);
                    stats.setPurchasesToday(0);
                    stats.setViewsToday(0);
                    course.setStatistics(stats);
                    
                    // Create initial lessons list from any available data in the course
                    List<MarketplaceCourse.Lesson> lessons = new ArrayList<>();
                    
                    // Add a default lesson if needed - user will edit these
                    if (lessons.isEmpty()) {
                        MarketplaceCourse.Lesson lesson = new MarketplaceCourse.Lesson();
                        lesson.setTitle("Lesson 1");
                        lesson.setContent("Enter lesson content here");
                        lessons.add(lesson);
                    }
                    
                    course.setLessons(lessons);
                    
                    // Create a basic preview
                    MarketplaceCourse.Preview preview = new MarketplaceCourse.Preview();
                    preview.setTitle("Preview");
                    preview.setContent("Preview content");
                    course.setPreview(preview);
                    
                    // Populate the form with course data
                    populateForm();
                    setFormEnabled(true);
                } catch (Exception e) {
                    Log.e(TAG, "Error converting course data: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Error loading course data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading course: " + errorMessage);
                Toast.makeText(getContext(), "Failed to load course: " + errorMessage, Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });
    }

    private MarketplaceCourse.CourseStatistics convertStatistics(Object oldStats) {
        if (oldStats == null) {
            return null;
        }
        
        // Create new statistics if we can't convert
        MarketplaceCourse.CourseStatistics newStats = new MarketplaceCourse.CourseStatistics();
        newStats.setTotalPurchases(0);
        newStats.setPurchasesToday(0);
        newStats.setViewsToday(0);
        return newStats;
    }

    private void populateForm() {
        if (course == null) {
            return;
        }
        
        etCourseName.setText(course.getName());
        etCourseDescription.setText(course.getDescription());
        etCoursePrice.setText(String.valueOf(course.getPrice()));
        
        if (course.getTags() != null && !course.getTags().isEmpty()) {
            etCourseTags.setText(TextUtils.join(", ", course.getTags()));
        }
        
        if (course.getRelatedModule() != null) {
            spinnerModuleCode.setText(course.getRelatedModule(), false);
        }
        
        // Set preview data
        if (course.getPreview() != null) {
            etPreviewTitle.setText(course.getPreview().getTitle());
            etPreviewContent.setText(course.getPreview().getContent());
            etPreviewVideoUrl.setText(course.getPreview().getVideoUrl());
        }
        
        // Set lessons data
        if (course.getLessons() != null && !course.getLessons().isEmpty()) {
            lessons.clear();
            lessons.addAll(course.getLessons());
            lessonAdapter.notifyDataSetChanged();
            checkEmptyLessons();
        }
    }

    private void setFormEnabled(boolean enabled) {
        etCourseName.setEnabled(enabled);
        etCourseDescription.setEnabled(enabled);
        etCoursePrice.setEnabled(enabled);
        etCourseTags.setEnabled(enabled);
        spinnerModuleCode.setEnabled(enabled);
        etPreviewTitle.setEnabled(enabled);
        etPreviewContent.setEnabled(enabled);
        etPreviewVideoUrl.setEnabled(enabled);
        btnAddLesson.setEnabled(enabled);
        btnCancel.setEnabled(enabled);
        btnSave.setEnabled(enabled);
    }

    private void addNewLesson() {
        MarketplaceCourse.Lesson lesson = new MarketplaceCourse.Lesson();
        lesson.setTitle("New Lesson");
        lesson.setContent("");
        lessons.add(lesson);
        lessonAdapter.notifyItemInserted(lessons.size() - 1);
        checkEmptyLessons();
        
        // Scroll to the newly added lesson
        rvLessons.smoothScrollToPosition(lessons.size() - 1);
    }

    private void checkEmptyLessons() {
        if (lessons.isEmpty()) {
            rvLessons.setVisibility(View.GONE);
            tvNoLessons.setVisibility(View.VISIBLE);
        } else {
            rvLessons.setVisibility(View.VISIBLE);
            tvNoLessons.setVisibility(View.GONE);
        }
    }

    private void saveCourse() {
        // Validate form
        if (!validateForm()) {
            return;
        }
        
        // Create or update course object
        if (course == null) {
            course = new MarketplaceCourse();
        }
        
        // Set basic information
        course.setName(etCourseName.getText().toString().trim());
        course.setDescription(etCourseDescription.getText().toString().trim());
        course.setRelatedModule(spinnerModuleCode.getText().toString().trim());
        
        try {
            double price = Double.parseDouble(etCoursePrice.getText().toString());
            course.setPrice(price);
        } catch (NumberFormatException e) {
            course.setPrice(0);
        }
        
        // Set tags - ensure we have at least the module code as a tag
        String tagsText = etCourseTags.getText().toString().trim();
        List<String> tagsList = new ArrayList<>();
        
        // Always add module code as a tag
        String moduleCode = spinnerModuleCode.getText().toString().trim();
        if (!moduleCode.isEmpty()) {
            tagsList.add(moduleCode);
        }
        
        // Add user-specified tags
        if (!tagsText.isEmpty()) {
            String[] tagsArray = tagsText.split(",");
            for (String tag : tagsArray) {
                String trimmedTag = tag.trim();
                if (!trimmedTag.isEmpty() && !tagsList.contains(trimmedTag)) {
                    tagsList.add(trimmedTag);
                }
            }
        }
        
        // Always have at least one tag (course name as fallback)
        if (tagsList.isEmpty()) {
            String courseName = etCourseName.getText().toString().trim();
            if (!courseName.isEmpty()) {
                tagsList.add(courseName);
            } else {
                tagsList.add("Course"); // Absolute fallback
            }
        }
        
        course.setTags(tagsList);
        
        // Set preview
        MarketplaceCourse.Preview preview = new MarketplaceCourse.Preview();
        preview.setTitle(etPreviewTitle.getText().toString().trim());
        preview.setContent(etPreviewContent.getText().toString().trim());
        
        // Process the video URL - convert to embed format
        String previewVideoUrl = etPreviewVideoUrl.getText().toString().trim();
        if (!previewVideoUrl.isEmpty() && isYoutubeUrl(previewVideoUrl)) {
            previewVideoUrl = convertToEmbedUrl(previewVideoUrl);
        }
        preview.setVideoUrl(previewVideoUrl);
        
        course.setPreview(preview);
        
        // Set lessons (already updated in real-time)
        course.setLessons(lessons);
        
        // Get current user for authorship
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in to create a course", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Use display name if available, otherwise email, with UUID as fallback
        String authorName;
        if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
            authorName = currentUser.getDisplayName();
        } else if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
            authorName = currentUser.getEmail();
        } else {
            authorName = "User " + currentUser.getUid().substring(0, 5); // Shortened UUID
        }
        
        // Also store the author ID for permission checks
        String authorId = currentUser.getUid();
        
        // Set author info in our course object
        course.setAuthor(authorName);
        course.setAuthorId(authorId);  // We'll need to add this field to MarketplaceCourse
        
        // Convert to Course object for compatibility
        Course convertedCourse = new Course();
        convertedCourse.setId(course.getId());
        convertedCourse.setName(course.getName());
        convertedCourse.setDescription(course.getDescription());
        convertedCourse.setRelatedModule(course.getRelatedModule());
        convertedCourse.setPrice(course.getPrice());
        convertedCourse.setTags(course.getTags());
        convertedCourse.setAuthor(authorName);  // Save readable author name
        
        // We need to add authorId field to store the UUID for permissions
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("authorId", authorId);
        convertedCourse.setAdditionalFields(additionalFields);
        
        // Create statistics
        Course.CourseStatistics stats = new Course.CourseStatistics();
        stats.setTotalPurchases(0);
        stats.setPurchasesToday(0);
        stats.setViewsToday(0);
        convertedCourse.setStatistics(stats);
        
        // Add lessons as content
        if (course.getLessons() != null && !course.getLessons().isEmpty()) {
            Course.CourseContent content = new Course.CourseContent();
            
            // Convert lessons to chapters and content items
            List<Course.Chapter> chapters = new ArrayList<>();
            Course.Chapter chapter = new Course.Chapter();
            chapter.setTitle("Course Content");
            
            List<Course.ContentItem> items = new ArrayList<>();
            for (MarketplaceCourse.Lesson lesson : course.getLessons()) {
                Course.ContentItem item = new Course.ContentItem();
                item.setTitle(lesson.getTitle());
                item.setContent(lesson.getContent());
                item.setType("text");
                
                if (lesson.getVideoUrl() != null && !lesson.getVideoUrl().isEmpty()) {
                    item.setUrl(lesson.getVideoUrl());
                }
                
                items.add(item);
            }
            
            chapter.setItems(items);
            chapters.add(chapter);
            content.setChapters(chapters);
            
            // Create preview content
            Course.Preview coursePreview = new Course.Preview();
            coursePreview.setTitle(course.getPreview() != null ? course.getPreview().getTitle() : "Preview");
            
            List<Course.ContentItem> previewItems = new ArrayList<>();
            Course.ContentItem previewItem = new Course.ContentItem();
            previewItem.setType("text");
            previewItem.setTitle("Preview");
            previewItem.setContent(course.getPreview() != null ? course.getPreview().getContent() : "Preview content");
            
            if (course.getPreview() != null && course.getPreview().getVideoUrl() != null) {
                previewItem.setUrl(course.getPreview().getVideoUrl());
            }
            
            previewItems.add(previewItem);
            coursePreview.setItems(previewItems);
            content.setPreview(coursePreview);
            
            convertedCourse.setContent(content);
        }
        
        // Save course to Firestore
        setFormEnabled(false);
        
        MarketplaceFirestoreManager.getInstance().saveCourse(convertedCourse, new MarketplaceFirestoreManager.OnCourseOperationListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Course saved successfully");
                Toast.makeText(getContext(), "Course saved successfully", Toast.LENGTH_SHORT).show();
                
                // Notify listener
                if (listener != null) {
                    listener.onCourseCreated();
                }
                
                dismiss();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error saving course: " + errorMessage);
                Toast.makeText(getContext(), "Error saving course: " + errorMessage, Toast.LENGTH_SHORT).show();
                setFormEnabled(true);
            }
        });
    }

    private boolean validateForm() {
        boolean valid = true;
        
        // Validate course name
        if (TextUtils.isEmpty(etCourseName.getText())) {
            etCourseName.setError("Course name is required");
            valid = false;
        } else {
            etCourseName.setError(null);
        }
        
        // Validate course description
        if (TextUtils.isEmpty(etCourseDescription.getText())) {
            etCourseDescription.setError("Course description is required");
            valid = false;
        } else {
            etCourseDescription.setError(null);
        }
        
        // Validate related module
        if (TextUtils.isEmpty(spinnerModuleCode.getText())) {
            spinnerModuleCode.setError("Related module is required");
            valid = false;
        } else {
            spinnerModuleCode.setError(null);
        }
        
        // Validate price
        if (TextUtils.isEmpty(etCoursePrice.getText())) {
            etCoursePrice.setError("Price is required");
            valid = false;
        } else {
            try {
                double price = Double.parseDouble(etCoursePrice.getText().toString());
                if (price < 0) {
                    etCoursePrice.setError("Price cannot be negative");
                    valid = false;
                } else {
                    etCoursePrice.setError(null);
                }
            } catch (NumberFormatException e) {
                etCoursePrice.setError("Invalid price format");
                valid = false;
            }
        }
        
        // Validate preview title
        if (TextUtils.isEmpty(etPreviewTitle.getText())) {
            etPreviewTitle.setError("Preview title is required");
            valid = false;
        } else {
            etPreviewTitle.setError(null);
        }
        
        // Validate preview content
        if (TextUtils.isEmpty(etPreviewContent.getText())) {
            etPreviewContent.setError("Preview content is required");
            valid = false;
        } else {
            etPreviewContent.setError(null);
        }
        
        // Validate that at least one lesson is added
        if (lessons.isEmpty()) {
            Toast.makeText(getContext(), "Add at least one lesson", Toast.LENGTH_SHORT).show();
            valid = false;
        } else {
            // Check that each lesson has at least a title and content
            boolean hasInvalidLesson = false;
            for (int i = 0; i < lessons.size(); i++) {
                MarketplaceCourse.Lesson lesson = lessons.get(i);
                if (TextUtils.isEmpty(lesson.getTitle()) || TextUtils.isEmpty(lesson.getContent())) {
                    Toast.makeText(getContext(), "Lesson " + (i+1) + " must have a title and content", 
                            Toast.LENGTH_SHORT).show();
                    hasInvalidLesson = true;
                    valid = false;
                    break;
                }
                
                // Always validate the YouTube URL field
                String videoUrl = lesson.getVideoUrl();
                if (!TextUtils.isEmpty(videoUrl)) {
                    if (!isYoutubeUrl(videoUrl)) {
                        Toast.makeText(getContext(), "Lesson " + (i+1) + " has an invalid YouTube URL", 
                                Toast.LENGTH_SHORT).show();
                        hasInvalidLesson = true;
                        valid = false;
                        break;
                    }
                }
            }
        }
        
        return valid;
    }

    public void setCourseCreatedListener(OnCourseCreatedListener listener) {
        this.listener = listener;
    }
    
    /**
     * Strictly validate YouTube URLs - accept common YouTube formats
     * @param url URL to validate
     * @return true if URL is a valid YouTube link
     */
    private boolean isYoutubeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        String trimmedUrl = url.trim();
        
        // Match common YouTube URL patterns including:
        // - youtube.com/watch?v=ID
        // - youtu.be/ID
        // - youtube.com/v/ID
        // - youtube.com/embed/ID
        boolean isValid = trimmedUrl.matches("^(https?://)?(www\\.)?(youtube\\.com/(watch\\?v=|v/|embed/)|youtu\\.be/)[a-zA-Z0-9_-]{11}.*$");
        
        if (!isValid) {
            Log.w(TAG, "Invalid YouTube URL: " + trimmedUrl);
        }
        return isValid;
    }
    
    /**
     * Converts any YouTube URL format to the standard embed format
     * @param url The original YouTube URL
     * @return Standardized YouTube embed URL
     */
    private String convertToEmbedUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }
        
        String trimmedUrl = url.trim();
        String videoId = extractYouTubeVideoId(trimmedUrl);
        
        if (videoId != null && !videoId.isEmpty()) {
            return "https://www.youtube.com/embed/" + videoId;
        }
        
        // If we couldn't extract the ID properly, return the original URL
        return trimmedUrl;
    }
    
    /**
     * Extracts the YouTube video ID from various URL formats
     * @param url The YouTube URL
     * @return The video ID or empty string if not found
     */
    private String extractYouTubeVideoId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }
        
        String videoId = "";
        String trimmedUrl = url.trim();
        
        // First, try to match patterns
        if (trimmedUrl.contains("youtu.be/")) {
            // Format: youtu.be/ID
            videoId = trimmedUrl.split("youtu\\.be/")[1];
        } else if (trimmedUrl.contains("youtube.com/watch")) {
            // Format: youtube.com/watch?v=ID
            String[] queryParams = trimmedUrl.split("\\?")[1].split("&");
            for (String param : queryParams) {
                if (param.startsWith("v=")) {
                    videoId = param.substring(2);
                    break;
                }
            }
        } else if (trimmedUrl.contains("youtube.com/embed/") || trimmedUrl.contains("youtube.com/v/")) {
            // Format: youtube.com/embed/ID or youtube.com/v/ID
            String[] parts = trimmedUrl.split("(embed/|v/)");
            if (parts.length > 1) {
                videoId = parts[1];
            }
        }
        
        // Clean up the ID by removing any URL parameters or extra parts
        if (videoId.contains("?")) {
            videoId = videoId.split("\\?")[0];
        }
        if (videoId.contains("&")) {
            videoId = videoId.split("&")[0];
        }
        if (videoId.contains("#")) {
            videoId = videoId.split("#")[0];
        }
        
        return videoId;
    }

    private class LessonAdapter extends RecyclerView.Adapter<LessonAdapter.LessonViewHolder> {
        private List<MarketplaceCourse.Lesson> lessonList;

        public LessonAdapter(List<MarketplaceCourse.Lesson> lessonList) {
            this.lessonList = lessonList;
        }

        @NonNull
        @Override
        public LessonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lesson_edit, parent, false);
            return new LessonViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LessonViewHolder holder, int position) {
            MarketplaceCourse.Lesson lesson = lessonList.get(position);
            holder.bind(lesson, position);
        }

        @Override
        public int getItemCount() {
            return lessonList.size();
        }

        class LessonViewHolder extends RecyclerView.ViewHolder {
            private TextInputEditText etLessonTitle, etLessonContent, etLessonVideoUrl;
            private TextView tvLessonNumber;
            private ImageButton btnMoveUp, btnMoveDown, btnDeleteLesson;
            private int position;

            public LessonViewHolder(@NonNull View itemView) {
                super(itemView);
                etLessonTitle = itemView.findViewById(R.id.etLessonTitle);
                etLessonContent = itemView.findViewById(R.id.etLessonContent);
                etLessonVideoUrl = itemView.findViewById(R.id.etLessonVideoUrl);
                tvLessonNumber = itemView.findViewById(R.id.tvLessonNumber);
                btnMoveUp = itemView.findViewById(R.id.btnMoveUp);
                btnMoveDown = itemView.findViewById(R.id.btnMoveDown);
                btnDeleteLesson = itemView.findViewById(R.id.btnDeleteLesson);
            }

            public void bind(MarketplaceCourse.Lesson lesson, int position) {
                this.position = position;
                
                // Set lesson number
                tvLessonNumber.setText("Lesson " + (position + 1));
                
                // Set lesson data
                etLessonTitle.setText(lesson.getTitle());
                etLessonContent.setText(lesson.getContent());
                etLessonVideoUrl.setText(lesson.getVideoUrl());
                
                // Set up move buttons visibility
                btnMoveUp.setVisibility(position > 0 ? View.VISIBLE : View.INVISIBLE);
                btnMoveDown.setVisibility(position < lessonList.size() - 1 ? View.VISIBLE : View.INVISIBLE);
                
                // Set up button listeners
                btnMoveUp.setOnClickListener(v -> moveLesson(position, position - 1));
                btnMoveDown.setOnClickListener(v -> moveLesson(position, position + 1));
                btnDeleteLesson.setOnClickListener(v -> deleteLesson(position));
                
                // Setup TextWatchers to update lesson data in real-time
                etLessonTitle.setOnFocusChangeListener((v, hasFocus) -> {
                    if (!hasFocus) {
                        lessonList.get(position).setTitle(etLessonTitle.getText().toString());
                    }
                });
                
                etLessonContent.setOnFocusChangeListener((v, hasFocus) -> {
                    if (!hasFocus) {
                        lessonList.get(position).setContent(etLessonContent.getText().toString());
                    }
                });
                
                etLessonVideoUrl.setOnFocusChangeListener((v, hasFocus) -> {
                    if (!hasFocus) {
                        String videoUrl = etLessonVideoUrl.getText().toString().trim();
                        
                        // If URL is provided, validate that it's a YouTube URL
                        if (!videoUrl.isEmpty()) {
                            if (!isYoutubeUrl(videoUrl)) {
                                etLessonVideoUrl.setError("Please enter a valid YouTube URL");
                                return;
                            }
                            
                            // Convert to proper embed URL
                            videoUrl = convertToEmbedUrl(videoUrl);
                        }
                        
                        lessonList.get(position).setVideoUrl(videoUrl);
                    }
                });
            }

            private void moveLesson(int fromPosition, int toPosition) {
                if (fromPosition < 0 || toPosition < 0 || fromPosition >= lessonList.size() || toPosition >= lessonList.size()) {
                    return;
                }
                
                // Swap lessons
                MarketplaceCourse.Lesson lesson = lessonList.remove(fromPosition);
                lessonList.add(toPosition, lesson);
                
                // Update adapter
                notifyItemMoved(fromPosition, toPosition);
                notifyItemChanged(fromPosition);
                notifyItemChanged(toPosition);
            }

            private void deleteLesson(int position) {
                if (position < 0 || position >= lessonList.size()) {
                    return;
                }
                
                // Remove lesson
                lessonList.remove(position);
                
                // Update adapter
                notifyItemRemoved(position);
                
                // Update remaining items to refresh numbering
                for (int i = position; i < lessonList.size(); i++) {
                    notifyItemChanged(i);
                }
                
                // Check if empty
                checkEmptyLessons();
            }
        }
    }
}