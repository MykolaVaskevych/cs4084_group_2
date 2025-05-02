package com.example.appdevelopmentprojectfinal.marketplace;

import android.util.Log;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class for courses in the marketplace with expanded learning content structure
 */
@IgnoreExtraProperties
public class MarketplaceCourse {
    private static final String TAG = "MarketplaceCourse";
    
    private String id;
    private String name;
    private String relatedModule;
    private String description;
    private double price;
    private String logo;
    private List<String> tags;
    private String author;
    private String authorId;  // Store the user UUID for ownership/permissions
    private List<Review> reviews;
    private double averageRating;
    private CourseStatistics statistics;
    private List<Lesson> lessons;
    private Preview preview;

    public MarketplaceCourse() {
        // Empty constructor required for Firestore deserialization
    }

    // Getters and setters
    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelatedModule() {
        return relatedModule;
    }

    public void setRelatedModule(String relatedModule) {
        this.relatedModule = relatedModule;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getAuthorId() {
        return authorId;
    }
    
    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public CourseStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(CourseStatistics statistics) {
        this.statistics = statistics;
    }
    
    public List<Lesson> getLessons() {
        return lessons;
    }
    
    public void setLessons(List<Lesson> lessons) {
        this.lessons = lessons;
    }
    
    public Preview getPreview() {
        return preview;
    }
    
    public void setPreview(Preview preview) {
        this.preview = preview;
    }

    // Helper method to get the module code from the related module
    @Exclude
    public String getModuleCode() {
        String moduleCode = relatedModule != null ? relatedModule : "";
        Log.v(TAG, "Getting module code for course " + id + ": " + moduleCode);
        return moduleCode;
    }
    
    /**
     * Adds a new lesson to the course
     * @param lesson the lesson to add
     * @return true if added successfully, false otherwise
     */
    @Exclude
    public boolean addLesson(Lesson lesson) {
        if (lesson == null) {
            return false;
        }
        
        if (lessons == null) {
            lessons = new ArrayList<>();
        }
        
        return lessons.add(lesson);
    }
    
    /**
     * Removes a lesson at the specified index
     * @param index the index of the lesson to remove
     * @return the removed lesson or null if invalid index
     */
    @Exclude
    public Lesson removeLesson(int index) {
        if (lessons == null || index < 0 || index >= lessons.size()) {
            return null;
        }
        
        return lessons.remove(index);
    }
    
    /**
     * Updates an existing lesson at the specified index
     * @param index the index of the lesson to update
     * @param lesson the new lesson data
     * @return true if updated successfully, false otherwise
     */
    @Exclude
    public boolean updateLesson(int index, Lesson lesson) {
        if (lessons == null || lesson == null || index < 0 || index >= lessons.size()) {
            return false;
        }
        
        lessons.set(index, lesson);
        return true;
    }

    // Inner classes for nested objects

    @IgnoreExtraProperties
    public static class Lesson {
        private String title;
        private String content;
        private String videoUrl; // YouTube video URL - optional
        
        public Lesson() {
            // Empty constructor required for Firestore deserialization
        }
        
        public Lesson(String title, String content) {
            this.title = title;
            this.content = content;
        }
        
        public Lesson(String title, String content, String videoUrl) {
            this.title = title;
            this.content = content;
            this.videoUrl = videoUrl;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getVideoUrl() {
            return videoUrl;
        }
        
        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }
    }
    
    @IgnoreExtraProperties
    public static class Preview {
        private String title;
        private String content;
        private String videoUrl;
        
        public Preview() {
            // Empty constructor required for Firestore deserialization
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getVideoUrl() {
            return videoUrl;
        }
        
        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }
    }

    @IgnoreExtraProperties
    public static class Review {
        private String user;
        private String userId;
        private double rating;
        private String comment;

        public Review() {
            // Empty constructor required for Firestore deserialization
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }
        
        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public double getRating() {
            return rating;
        }

        public void setRating(double rating) {
            this.rating = rating;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

    @IgnoreExtraProperties
    public static class CourseStatistics {
        private int totalPurchases;
        private int viewsToday;
        private int purchasesToday;

        public CourseStatistics() {
            // Empty constructor required for Firestore deserialization
        }

        public int getTotalPurchases() {
            return totalPurchases;
        }

        public void setTotalPurchases(int totalPurchases) {
            this.totalPurchases = totalPurchases;
        }

        public int getViewsToday() {
            return viewsToday;
        }

        public void setViewsToday(int viewsToday) {
            this.viewsToday = viewsToday;
        }

        public int getPurchasesToday() {
            return purchasesToday;
        }

        public void setPurchasesToday(int purchasesToday) {
            this.purchasesToday = purchasesToday;
        }
    }
}