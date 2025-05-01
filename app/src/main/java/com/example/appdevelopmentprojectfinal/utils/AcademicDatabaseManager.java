package com.example.appdevelopmentprojectfinal.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages academic database operations for departments, courses, years, and modules
 * 
 * @author Mykola Vaskevych (22372199)
 */
public class AcademicDatabaseManager {
    private static final String TAG = "AcademicDbManager";
    
    // Collection names
    private static final String COLLECTION_DEPARTMENTS = "departments";
    private static final String COLLECTION_COURSES = "courses";
    private static final String COLLECTION_YEARS = "years";
    private static final String COLLECTION_MODULES = "modules_by_Mykola";
    
    // Singleton instance
    private static AcademicDatabaseManager instance;
    
    // Firestore reference
    private final FirebaseFirestore firestore;
    
    // Data caching
    private List<Department> departmentsCache;
    private Map<String, List<Course>> coursesCache;
    private Map<String, List<Year>> yearsCache;
    private Map<String, List<Module>> modulesCache;
    
    /**
     * Department data model
     */
    public static class Department {
        private String id;
        private String name;
        private String code;
        private String description;
        
        public Department() {
            // Required empty constructor for Firestore
        }
        
        public Department(String id, String name, String code, String description) {
            this.id = id;
            this.name = name;
            this.code = code;
            this.description = description;
        }
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    /**
     * Course data model
     */
    public static class Course {
        private String id;
        private String name;
        private String code;
        private String departmentId;
        private String description;
        private int durationYears;
        
        public Course() {
            // Required empty constructor for Firestore
        }
        
        public Course(String id, String name, String code, String departmentId, String description, int durationYears) {
            this.id = id;
            this.name = name;
            this.code = code;
            this.departmentId = departmentId;
            this.description = description;
            this.durationYears = durationYears;
        }
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getDepartmentId() { return departmentId; }
        public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public int getDurationYears() { return durationYears; }
        public void setDurationYears(int durationYears) { this.durationYears = durationYears; }
        
        @Override
        public String toString() {
            return code + " - " + name;
        }
    }
    
    /**
     * Year data model
     */
    public static class Year {
        private String id;
        private String courseId;
        private int yearNumber;
        private String description;
        
        public Year() {
            // Required empty constructor for Firestore
        }
        
        public Year(String id, String courseId, int yearNumber, String description) {
            this.id = id;
            this.courseId = courseId;
            this.yearNumber = yearNumber;
            this.description = description;
        }
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getCourseId() { return courseId; }
        public void setCourseId(String courseId) { this.courseId = courseId; }
        
        public int getYearNumber() { return yearNumber; }
        public void setYearNumber(int yearNumber) { this.yearNumber = yearNumber; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        @Override
        public String toString() {
            return "Year " + yearNumber;
        }
    }
    
    /**
     * Module data model
     */
    public static class Module {
        private String id;
        private String code;
        private String name;
        private int credits;
        private String yearId;
        private String departmentId;
        private String description;
        private int semester;
        private boolean isRequired;
        private List<String> prerequisites;
        
        public Module() {
            // Required empty constructor for Firestore
            prerequisites = new ArrayList<>();
        }
        
        public Module(String id, String code, String name, int credits, String yearId, 
                     String departmentId, String description, int semester, boolean isRequired) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.credits = credits;
            this.yearId = yearId;
            this.departmentId = departmentId;
            this.description = description;
            this.semester = semester;
            this.isRequired = isRequired;
            this.prerequisites = new ArrayList<>();
        }
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public int getCredits() { return credits; }
        public void setCredits(int credits) { this.credits = credits; }
        
        public String getYearId() { return yearId; }
        public void setYearId(String yearId) { this.yearId = yearId; }
        
        public String getDepartmentId() { return departmentId; }
        public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public int getSemester() { return semester; }
        public void setSemester(int semester) { this.semester = semester; }
        
        public boolean isRequired() { return isRequired; }
        public void setRequired(boolean required) { isRequired = required; }
        
        public List<String> getPrerequisites() { return prerequisites; }
        public void setPrerequisites(List<String> prerequisites) { this.prerequisites = prerequisites; }
        
        @Override
        public String toString() {
            return code + " - " + name;
        }
    }
    
    /**
     * Callback interfaces for database operations
     */
    public interface OnDepartmentsLoadedListener {
        void onDepartmentsLoaded(List<Department> departments);
        void onError(String errorMessage);
    }
    
    public interface OnCoursesLoadedListener {
        void onCoursesLoaded(List<Course> courses);
        void onError(String errorMessage);
    }
    
    public interface OnYearsLoadedListener {
        void onYearsLoaded(List<Year> years);
        void onError(String errorMessage);
    }
    
    public interface OnModulesLoadedListener {
        void onModulesLoaded(List<Module> modules);
        void onError(String errorMessage);
    }
    
    /**
     * Private constructor for singleton pattern
     */
    private AcademicDatabaseManager() {
        firestore = FirebaseFirestore.getInstance();
        departmentsCache = new ArrayList<>();
        coursesCache = new HashMap<>();
        yearsCache = new HashMap<>();
        modulesCache = new HashMap<>();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized AcademicDatabaseManager getInstance() {
        if (instance == null) {
            instance = new AcademicDatabaseManager();
        }
        return instance;
    }
    
    /**
     * Database Structure Documentation:
     * 
     * Collection: departments
     * - Document ID: departmentId (e.g., "cs_dept")
     * - Fields:
     *   - name: String (e.g., "Computer Science & Information Systems")
     *   - code: String (e.g., "CS")
     *   - description: String
     * 
     * Collection: courses
     * - Document ID: courseId (e.g., "cs_bsc")
     * - Fields:
     *   - name: String (e.g., "Computer Science")
     *   - code: String (e.g., "LM051")
     *   - departmentId: String (reference to department)
     *   - description: String
     *   - durationYears: Number
     * 
     * Collection: years
     * - Document ID: yearId (e.g., "cs_bsc_y1")
     * - Fields:
     *   - courseId: String (reference to course)
     *   - yearNumber: Number
     *   - description: String
     * 
     * Collection: modules_by_Mykola
     * - Document ID: module code (e.g., "cs4084")
     * - Fields:
     *   - code: String
     *   - name: String
     *   - credits: Number
     *   - yearId: String (reference to year)
     *   - departmentId: String (reference to department)
     *   - description: String
     *   - semester: Number (1 or 2)
     *   - isRequired: Boolean
     *   - prerequisites: Array<String> (module codes)
     */

    /**
     * Initialize the database (checks and creates collections only)
     */
    public void initializeDatabaseIfNeeded() {
        Log.i(TAG, "Verifying database collections");
        
        // Just make sure collections exist - creates empty collections if needed
        firestore.collection(COLLECTION_DEPARTMENTS).document("dummy").get();
        firestore.collection(COLLECTION_COURSES).document("dummy").get();
        firestore.collection(COLLECTION_YEARS).document("dummy").get();
        firestore.collection(COLLECTION_MODULES).document("dummy").get();
    }
    
    /**
     * Helper method to create a module in Firebase
     * Example: createModule("cs4084", "Mobile App Development", 6, "cs_bsc_y4", "cs_dept", 
     *                       "Description...", 2, true);
     */
    public void createModule(String code, String name, int credits, String yearId, String departmentId,
                             String description, int semester, boolean isRequired) {
        Map<String, Object> module = new HashMap<>();
        module.put("code", code);
        module.put("name", name);
        module.put("credits", credits);
        module.put("yearId", yearId);
        module.put("departmentId", departmentId);
        module.put("description", description);
        module.put("semester", semester);
        module.put("isRequired", isRequired);
        module.put("prerequisites", new ArrayList<String>());
        
        firestore.collection(COLLECTION_MODULES).document(code).set(module)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Module created: " + code))
            .addOnFailureListener(e -> Log.e(TAG, "Error creating module: " + code, e));
    }
    
    /**
     * Load all departments
     */
    public void loadDepartments(final OnDepartmentsLoadedListener listener) {
        Log.d(TAG, "Loading departments from Firestore");
        
        // Check if we have cached data
        if (!departmentsCache.isEmpty()) {
            Log.d(TAG, "Returning " + departmentsCache.size() + " departments from cache");
            listener.onDepartmentsLoaded(departmentsCache);
            return;
        }
        
        firestore.collection(COLLECTION_DEPARTMENTS)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        List<Department> departments = new ArrayList<>();
                        
                        for (DocumentSnapshot document : task.getResult()) {
                            Department dept = document.toObject(Department.class);
                            if (dept != null) {
                                dept.setId(document.getId());
                                departments.add(dept);
                            }
                        }
                        
                        // Cache the results
                        departmentsCache = departments;
                        
                        Log.d(TAG, "Loaded " + departments.size() + " departments");
                        listener.onDepartmentsLoaded(departments);
                    } else {
                        Log.e(TAG, "Error loading departments", task.getException());
                        listener.onError("Failed to load departments: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                }
            });
    }
    
    /**
     * Load courses for a specific department
     */
    public void loadCourses(final String departmentId, final OnCoursesLoadedListener listener) {
        Log.d(TAG, "Loading courses for department: " + departmentId);
        
        // Check cache first
        if (coursesCache.containsKey(departmentId)) {
            Log.d(TAG, "Returning " + coursesCache.get(departmentId).size() + " courses from cache");
            listener.onCoursesLoaded(coursesCache.get(departmentId));
            return;
        }
        
        firestore.collection(COLLECTION_COURSES)
            .whereEqualTo("departmentId", departmentId)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        List<Course> courses = new ArrayList<>();
                        
                        for (DocumentSnapshot document : task.getResult()) {
                            Course course = document.toObject(Course.class);
                            if (course != null) {
                                course.setId(document.getId());
                                courses.add(course);
                            }
                        }
                        
                        // Cache the results
                        coursesCache.put(departmentId, courses);
                        
                        Log.d(TAG, "Loaded " + courses.size() + " courses for department " + departmentId);
                        listener.onCoursesLoaded(courses);
                    } else {
                        Log.e(TAG, "Error loading courses", task.getException());
                        listener.onError("Failed to load courses: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                }
            });
    }
    
    /**
     * Load all years for a specific course
     */
    public void loadYears(final String courseId, final OnYearsLoadedListener listener) {
        Log.d(TAG, "Loading years for course: " + courseId);
        
        // Check cache first
        if (yearsCache.containsKey(courseId)) {
            Log.d(TAG, "Returning " + yearsCache.get(courseId).size() + " years from cache");
            listener.onYearsLoaded(yearsCache.get(courseId));
            return;
        }
        
        firestore.collection(COLLECTION_YEARS)
            .whereEqualTo("courseId", courseId)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        List<Year> years = new ArrayList<>();
                        
                        for (DocumentSnapshot document : task.getResult()) {
                            Year year = document.toObject(Year.class);
                            if (year != null) {
                                year.setId(document.getId());
                                years.add(year);
                            }
                        }
                        
                        // Sort by year number
                        years.sort((y1, y2) -> Integer.compare(y1.getYearNumber(), y2.getYearNumber()));
                        
                        // Cache the results
                        yearsCache.put(courseId, years);
                        
                        Log.d(TAG, "Loaded " + years.size() + " years for course " + courseId);
                        listener.onYearsLoaded(years);
                    } else {
                        Log.e(TAG, "Error loading years", task.getException());
                        listener.onError("Failed to load years: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                }
            });
    }
    
    /**
     * Load all modules for a specific year
     */
    public void loadModulesByYear(final String yearId, final OnModulesLoadedListener listener) {
        Log.d(TAG, "Loading modules for year: " + yearId);
        
        // Check cache first
        if (modulesCache.containsKey(yearId)) {
            Log.d(TAG, "Returning " + modulesCache.get(yearId).size() + " modules from cache");
            listener.onModulesLoaded(modulesCache.get(yearId));
            return;
        }
        
        firestore.collection(COLLECTION_MODULES)
            .whereEqualTo("yearId", yearId)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        List<Module> modules = new ArrayList<>();
                        
                        for (DocumentSnapshot document : task.getResult()) {
                            Module module = document.toObject(Module.class);
                            if (module != null) {
                                module.setId(document.getId());
                                modules.add(module);
                            }
                        }
                        
                        // Sort by module code
                        modules.sort((m1, m2) -> m1.getCode().compareTo(m2.getCode()));
                        
                        // Cache the results
                        modulesCache.put(yearId, modules);
                        
                        Log.d(TAG, "Loaded " + modules.size() + " modules for year " + yearId);
                        listener.onModulesLoaded(modules);
                    } else {
                        Log.e(TAG, "Error loading modules", task.getException());
                        listener.onError("Failed to load modules: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                }
            });
    }
    
    /**
     * Load all modules for a specific department
     */
    public void loadModulesByDepartment(final String departmentId, final OnModulesLoadedListener listener) {
        Log.d(TAG, "Loading modules for department: " + departmentId);
        
        firestore.collection(COLLECTION_MODULES)
            .whereEqualTo("departmentId", departmentId)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        List<Module> modules = new ArrayList<>();
                        
                        for (DocumentSnapshot document : task.getResult()) {
                            Module module = document.toObject(Module.class);
                            if (module != null) {
                                module.setId(document.getId());
                                modules.add(module);
                            }
                        }
                        
                        // Sort by module code
                        modules.sort((m1, m2) -> m1.getCode().compareTo(m2.getCode()));
                        
                        Log.d(TAG, "Loaded " + modules.size() + " modules for department " + departmentId);
                        listener.onModulesLoaded(modules);
                    } else {
                        Log.e(TAG, "Error loading modules by department", task.getException());
                        listener.onError("Failed to load modules: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                }
            });
    }
    
    /**
     * Search for modules by code or name
     */
    public void searchModules(final String query, final OnModulesLoadedListener listener) {
        Log.d(TAG, "Searching modules with query: " + query);
        
        // In Firestore we can't do partial text search directly, so we'll fetch all modules
        // and filter them on the client side
        firestore.collection(COLLECTION_MODULES)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        List<Module> modules = new ArrayList<>();
                        String lowercaseQuery = query.toLowerCase();
                        
                        for (DocumentSnapshot document : task.getResult()) {
                            Module module = document.toObject(Module.class);
                            if (module != null) {
                                module.setId(document.getId());
                                
                                // Check if module code or name contains the query
                                if (module.getCode().toLowerCase().contains(lowercaseQuery) || 
                                    module.getName().toLowerCase().contains(lowercaseQuery)) {
                                    modules.add(module);
                                }
                            }
                        }
                        
                        // Sort by module code
                        modules.sort((m1, m2) -> m1.getCode().compareTo(m2.getCode()));
                        
                        Log.d(TAG, "Found " + modules.size() + " modules matching query: " + query);
                        listener.onModulesLoaded(modules);
                    } else {
                        Log.e(TAG, "Error searching modules", task.getException());
                        listener.onError("Failed to search modules: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                }
            });
    }
    
    /**
     * Load a specific module by its code
     */
    public void getModuleByCode(final String moduleCode, final OnModulesLoadedListener listener) {
        Log.d(TAG, "Loading module with code: " + moduleCode);
        
        firestore.collection(COLLECTION_MODULES)
            .document(moduleCode)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Module module = document.toObject(Module.class);
                        if (module != null) {
                            module.setId(document.getId());
                            List<Module> modules = new ArrayList<>();
                            modules.add(module);
                            
                            Log.d(TAG, "Loaded module: " + moduleCode);
                            listener.onModulesLoaded(modules);
                        } else {
                            listener.onError("Failed to convert module document");
                        }
                    } else {
                        Log.w(TAG, "Module not found: " + moduleCode);
                        listener.onError("Module not found: " + moduleCode);
                    }
                } else {
                    Log.e(TAG, "Error loading module", task.getException());
                    listener.onError("Failed to load module: " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                }
            });
    }
    
    /**
     * Clear all cached data
     */
    public void clearCache() {
        departmentsCache.clear();
        coursesCache.clear();
        yearsCache.clear();
        modulesCache.clear();
        Log.d(TAG, "Cache cleared");
    }
    
    /**
     * Get Firestore collection references
     */
    public CollectionReference getDepartmentsCollection() {
        return firestore.collection(COLLECTION_DEPARTMENTS);
    }
    
    public CollectionReference getCoursesCollection() {
        return firestore.collection(COLLECTION_COURSES);
    }
    
    public CollectionReference getYearsCollection() {
        return firestore.collection(COLLECTION_YEARS);
    }
    
    public CollectionReference getModulesCollection() {
        return firestore.collection(COLLECTION_MODULES);
    }
    
    /**
     * Get the modules collection name
     */
    public String getModulesCollectionName() {
        return COLLECTION_MODULES;
    }
}