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
        
        // Check if we need to create sample modules
        firestore.collection(COLLECTION_MODULES).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && (task.getResult() == null || task.getResult().isEmpty() || task.getResult().size() <= 1)) {
                Log.i(TAG, "No modules found, creating sample modules");
                createSampleModules();
            } else {
                Log.i(TAG, "Modules already exist in the database");
            }
        });
    }
    
    /**
     * Create sample departments, courses, years, and modules in Firebase
     */
    private void createSampleModules() {
        Log.i(TAG, "Creating sample modules in Firebase");
        
        // Create Computer Science department
        Map<String, Object> csDept = new HashMap<>();
        csDept.put("name", "Computer Science & Information Systems");
        csDept.put("code", "CS");
        csDept.put("description", "Department of Computer Science and Information Systems");
        
        firestore.collection(COLLECTION_DEPARTMENTS).document("cs_dept").set(csDept)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "CS Department created");
                
                // Create BSc in Computer Science
                Map<String, Object> csCourse = new HashMap<>();
                csCourse.put("name", "Computer Science");
                csCourse.put("code", "LM051");
                csCourse.put("departmentId", "cs_dept");
                csCourse.put("description", "Bachelor of Science in Computer Science");
                csCourse.put("durationYears", 4);
                
                firestore.collection(COLLECTION_COURSES).document("cs_bsc").set(csCourse)
                    .addOnSuccessListener(courseVoid -> {
                        Log.d(TAG, "CS Course created");
                        
                        // Create years 1-4
                        for (int year = 1; year <= 4; year++) {
                            final int yearNum = year;
                            Map<String, Object> yearData = new HashMap<>();
                            yearData.put("courseId", "cs_bsc");
                            yearData.put("yearNumber", year);
                            yearData.put("description", "Year " + year + " of BSc in Computer Science");
                            
                            firestore.collection(COLLECTION_YEARS).document("cs_bsc_y" + year).set(yearData)
                                .addOnSuccessListener(yearVoid -> {
                                    Log.d(TAG, "Created Year " + yearNum);
                                    
                                    // Create modules for each year
                                    if (yearNum == 1) {
                                        createModule("cs1000", "Programming Fundamentals", 6, "cs_bsc_y1", "cs_dept", 
                                                    "Introduction to programming concepts and problem solving", 1, true);
                                        createModule("cs1001", "Computer Systems", 6, "cs_bsc_y1", "cs_dept", 
                                                    "Introduction to computer architecture and systems", 1, true);
                                    } else if (yearNum == 2) {
                                        createModule("cs2000", "Data Structures and Algorithms", 6, "cs_bsc_y2", "cs_dept", 
                                                    "Advanced data structures and algorithm design", 1, true);
                                        createModule("cs2001", "Database Systems", 6, "cs_bsc_y2", "cs_dept", 
                                                    "Relational database theory and SQL", 2, true);
                                    } else if (yearNum == 3) {
                                        createModule("cs3000", "Software Engineering", 6, "cs_bsc_y3", "cs_dept", 
                                                    "Software development lifecycle and team projects", 1, true);
                                        createModule("cs3001", "Web Development", 6, "cs_bsc_y3", "cs_dept", 
                                                    "Web technologies and frameworks", 2, true);
                                    } else if (yearNum == 4) {
                                        createModule("cs4084", "Mobile Application Development", 6, "cs_bsc_y4", "cs_dept", 
                                                    "Native mobile app development for Android", 2, true);
                                        createModule("cs4106", "Machine Learning", 6, "cs_bsc_y4", "cs_dept", 
                                                    "Fundamentals of machine learning and AI", 1, true);
                                        createModule("cs4116", "Software Development Project", 6, "cs_bsc_y4", "cs_dept", 
                                                    "Capstone project course", 1, true);
                                        createModule("cs4187", "Graph Theory", 6, "cs_bsc_y4", "cs_dept", 
                                                    "Study of graphs and networks", 2, false);
                                    }
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error creating year " + yearNum, e));
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error creating CS course", e));
            })
            .addOnFailureListener(e -> Log.e(TAG, "Error creating CS department", e));
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
    
    /**
     * Creates sample marketplace courses in Firebase with content and YouTube videos
     * This method should be used only once to populate the database
     */
    public void createSampleCoursesInFirebase(final OnCourseOperationListener listener) {
        Log.i(TAG, "Creating sample marketplace courses in Firebase...");
        
        // Get a reference to the marketplace collection
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final CollectionReference coursesCollection = db.collection("marketplace");
        
        // Demo YouTube video URL (make sure this is the correct embed format)
        final String demoVideoUrl = "https://www.youtube.com/embed/bhwPhcFJU7E?si=S5KGzdEw8ie4IUq1";
        
        // Create sample marketplace courses directly (not tied to module loading)
        // Create courses matching the modules from logs (cs4013, cs4141)
        // Plus additional related CS modules
        String[][] coursesData = {
            {"cs4013", "Object Oriented Development Masterclass"},
            {"cs4141", "Introduction to Programming"},
            {"cs4111", "Computer Architecture and Organization"},
            {"cs4084", "Mobile Application Development"},
            {"cs4116", "Software Development Project"}
        };
        
        // Keep track of successful course creations
        final int[] successCount = {0};
        final int[] failCount = {0};
        final int totalToCreate = coursesData.length;
        
        for (String[] courseData : coursesData) {
            // Create a marketplace course
            createMarketplaceCourse(
                courseData[0],    // Module code
                courseData[1],    // Course name
                coursesCollection,
                demoVideoUrl,
                new OnCourseOperationListener() {
                    @Override
                    public void onSuccess() {
                        successCount[0]++;
                        Log.i(TAG, "Created course " + successCount[0] + " of " + totalToCreate);
                        checkCompletion();
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        failCount[0]++;
                        Log.e(TAG, "Failed to create course: " + errorMessage);
                        checkCompletion();
                    }
                    
                    private void checkCompletion() {
                        if (successCount[0] + failCount[0] >= totalToCreate) {
                            Log.i(TAG, "Completed creating " + successCount[0] + " courses with " + 
                                      failCount[0] + " failures");
                            if (listener != null) {
                                if (failCount[0] > 0) {
                                    listener.onError("Created " + successCount[0] + " courses with " + 
                                                  failCount[0] + " failures");
                                } else {
                                    listener.onSuccess();
                                }
                            }
                        }
                    }
                }
            );
        }
    }
    
    /**
     * Creates a marketplace course given a module code and course name
     */
    private void createMarketplaceCourse(String moduleCode, String courseName, 
                                        CollectionReference coursesCollection, 
                                        String demoVideoUrl, OnCourseOperationListener listener) {
        Log.i(TAG, "Creating marketplace course: " + courseName + " (Module: " + moduleCode + ")");
        
        // Create a new course document
        String courseId = coursesCollection.document().getId();
        
        // Create a new course object
        com.example.appdevelopmentprojectfinal.model.Course course = 
            new com.example.appdevelopmentprojectfinal.model.Course();
        
        course.setId(courseId);
        course.setName(courseName);
        course.setRelatedModule(moduleCode);
        course.setDescription("A comprehensive course covering all aspects of " + courseName + 
                             ". Learn the fundamentals and advanced techniques needed to excel in this subject.");
        course.setPrice((double)((int)(Math.random() * 8000) + 1000) / 100); // Random price between 10.00 and 89.99
        course.setLogo("https://picsum.photos/200"); // Random image placeholder
        course.setAuthor("Professor " + getRandomName());
        
        // Set course tags
        List<String> tags = new ArrayList<>();
        tags.add(moduleCode.toUpperCase());
        tags.add("Computer Science");
        tags.add("Programming");
        tags.add(courseName.split(" ")[0]); // First word of course name
        course.setTags(tags);
        
        // Create course statistics
        com.example.appdevelopmentprojectfinal.model.Course.CourseStatistics statistics = 
            new com.example.appdevelopmentprojectfinal.model.Course.CourseStatistics();
        statistics.setTotalPurchases((int)(Math.random() * 500) + 100);  // More purchases to make them appear in trending
        statistics.setViewsToday((int)(Math.random() * 200) + 50);
        statistics.setPurchasesToday((int)(Math.random() * 50) + 10);
        course.setStatistics(statistics);
        
        // Set rating (between 4.0 and 5.0)
        course.setAverageRating(4.0 + Math.random());
        
        // Add some reviews
        List<com.example.appdevelopmentprojectfinal.model.Course.Review> reviews = new ArrayList<>();
        // Add 3-5 reviews
        int numReviews = (int)(Math.random() * 3) + 3;
        for (int i = 0; i < numReviews; i++) {
            com.example.appdevelopmentprojectfinal.model.Course.Review review = 
                new com.example.appdevelopmentprojectfinal.model.Course.Review();
            review.setUser(getRandomName());
            review.setRating(4.0 + Math.random());  // Rating between 4.0 and 5.0
            review.setComment("Great course! I learned a lot about " + courseName + ". The explanations are clear and concise.");
            reviews.add(review);
        }
        course.setReviews(reviews);
        
        // Create course content
        com.example.appdevelopmentprojectfinal.model.Course.CourseContent content = 
            new com.example.appdevelopmentprojectfinal.model.Course.CourseContent();
        
        // Create preview
        com.example.appdevelopmentprojectfinal.model.Course.Preview preview = 
            new com.example.appdevelopmentprojectfinal.model.Course.Preview();
        preview.setTitle("Course Preview");
        
        List<com.example.appdevelopmentprojectfinal.model.Course.ContentItem> previewItems = new ArrayList<>();
        
        // Add preview video
        com.example.appdevelopmentprojectfinal.model.Course.ContentItem previewVideo = 
            new com.example.appdevelopmentprojectfinal.model.Course.ContentItem();
        previewVideo.setType("video");
        previewVideo.setTitle("Introduction to " + courseName);
        previewVideo.setUrl(demoVideoUrl);
        previewVideo.setCaption("A brief introduction to the course");
        previewItems.add(previewVideo);
        
        // Add preview text
        com.example.appdevelopmentprojectfinal.model.Course.ContentItem previewText = 
            new com.example.appdevelopmentprojectfinal.model.Course.ContentItem();
        previewText.setType("text");
        previewText.setTitle("About this Course");
        previewText.setContent("This course will teach you everything you need to know about " + courseName + 
                              ". You'll learn key concepts, practical skills, and gain hands-on experience through " +
                              "projects and examples. By the end of this course, you'll have mastered the subject and " +
                              "will be able to apply your knowledge to real-world situations.");
        previewItems.add(previewText);
        
        preview.setItems(previewItems);
        content.setPreview(preview);
        
        // Create chapters (3-5 chapters per course)
        List<com.example.appdevelopmentprojectfinal.model.Course.Chapter> chapters = new ArrayList<>();
        int numChapters = (int)(Math.random() * 3) + 3; // 3-5 chapters
        
        String[] chapterTitles = {
            "Introduction to " + courseName,
            "Fundamentals of " + moduleCode,
            "Advanced Concepts and Techniques",
            "Practical Applications",
            "Best Practices and Design Patterns"
        };
        
        for (int i = 1; i <= numChapters; i++) {
            com.example.appdevelopmentprojectfinal.model.Course.Chapter chapter = 
                new com.example.appdevelopmentprojectfinal.model.Course.Chapter();
            
            chapter.setTitle("Chapter " + i + ": " + chapterTitles[i-1]);
            
            // Create lessons (3-5 lessons per chapter)
            List<com.example.appdevelopmentprojectfinal.model.Course.ContentItem> lessons = new ArrayList<>();
            int numLessons = (int)(Math.random() * 3) + 3; // 3-5 lessons
            
            String[] lessonTitles = {
                "Getting Started",
                "Core Concepts",
                "Advanced Techniques",
                "Practical Examples",
                "Case Study"
            };
            
            for (int j = 1; j <= numLessons; j++) {
                // Add a video lesson
                com.example.appdevelopmentprojectfinal.model.Course.ContentItem videoLesson = 
                    new com.example.appdevelopmentprojectfinal.model.Course.ContentItem();
                videoLesson.setType("video");
                videoLesson.setTitle("Lesson " + j + ": " + lessonTitles[j-1]);
                videoLesson.setUrl(demoVideoUrl);
                videoLesson.setCaption("Chapter " + i + " - Lesson " + j);
                lessons.add(videoLesson);
                
                // Add a text lesson
                com.example.appdevelopmentprojectfinal.model.Course.ContentItem textLesson = 
                    new com.example.appdevelopmentprojectfinal.model.Course.ContentItem();
                textLesson.setType("text");
                textLesson.setTitle("Lesson " + j + " Reading Material: " + lessonTitles[j-1]);
                textLesson.setContent("# " + lessonTitles[j-1] + "\n\n" +
                                     "This lesson covers important concepts related to " + courseName + ".\n\n" +
                                     "In computer science, a data structure is a data organization, management, " +
                                     "and storage format that enables efficient access and modification. More precisely, " +
                                     "a data structure is a collection of data values, the relationships among them, " +
                                     "and the functions or operations that can be applied to the data.\n\n" +
                                     "## Example Code\n\n```java\npublic class Example {\n    " +
                                     "public static void main(String[] args) {\n        " +
                                     "System.out.println(\"This is an example for " + courseName + "\");\n        " +
                                     "// Code implementation here\n    }\n}\n```\n\n" +
                                     "### Key Takeaways\n\n" +
                                     "1. Understand the fundamental concepts\n" +
                                     "2. Learn how to implement the techniques\n" +
                                     "3. Practice with real-world examples\n");
                lessons.add(textLesson);
            }
            
            chapter.setItems(lessons);
            chapters.add(chapter);
        }
        
        content.setChapters(chapters);
        course.setContent(content);
        
        // Save the course to Firestore
        coursesCollection.document(courseId).set(course)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Marketplace course created successfully: " + courseId);
                if (listener != null) {
                    listener.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating marketplace course: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            });
    }
    
    /**
     * Creates a course for a specific module (old method, kept for reference)
     */
    private void createCourseForModule(Module module, CollectionReference coursesCollection, 
                                      String demoVideoUrl, OnCourseOperationListener listener) {
        Log.i(TAG, "Creating course for module: " + module.getCode() + " - " + module.getName());
        
        // Create a new course document
        String courseId = coursesCollection.document().getId();
        
        // Create a new course object
        com.example.appdevelopmentprojectfinal.model.Course course = 
            new com.example.appdevelopmentprojectfinal.model.Course();
        
        course.setId(courseId);
        course.setName("Mastering " + module.getName());
        course.setRelatedModule(module.getCode());
        course.setDescription("A comprehensive course covering all aspects of " + module.getName() + 
                             ". Learn the fundamentals and advanced techniques needed to excel in this module.");
        course.setPrice(19.99);
        course.setLogo("https://picsum.photos/200"); // Random image placeholder
        course.setAuthor("Professor " + getRandomName());
        
        // Set course tags
        List<String> tags = new ArrayList<>();
        tags.add(module.getCode().toUpperCase());
        tags.add("Computer Science");
        tags.add("Programming");
        tags.add(module.getName().split(" ")[0]); // First word of module name
        course.setTags(tags);
        
        // Create course statistics
        com.example.appdevelopmentprojectfinal.model.Course.CourseStatistics statistics = 
            new com.example.appdevelopmentprojectfinal.model.Course.CourseStatistics();
        statistics.setTotalPurchases((int)(Math.random() * 50) + 10);
        statistics.setViewsToday((int)(Math.random() * 100) + 20);
        statistics.setPurchasesToday((int)(Math.random() * 10) + 1);
        course.setStatistics(statistics);
        
        // Set default rating
        course.setAverageRating(4.5);
        
        // Create course content
        com.example.appdevelopmentprojectfinal.model.Course.CourseContent content = 
            new com.example.appdevelopmentprojectfinal.model.Course.CourseContent();
        
        // Create preview
        com.example.appdevelopmentprojectfinal.model.Course.Preview preview = 
            new com.example.appdevelopmentprojectfinal.model.Course.Preview();
        preview.setTitle("Course Preview");
        
        List<com.example.appdevelopmentprojectfinal.model.Course.ContentItem> previewItems = new ArrayList<>();
        
        // Add preview video
        com.example.appdevelopmentprojectfinal.model.Course.ContentItem previewVideo = 
            new com.example.appdevelopmentprojectfinal.model.Course.ContentItem();
        previewVideo.setType("video");
        previewVideo.setTitle("Introduction to " + module.getName());
        previewVideo.setUrl(demoVideoUrl);
        previewVideo.setCaption("A brief introduction to the course");
        previewItems.add(previewVideo);
        
        // Add preview text
        com.example.appdevelopmentprojectfinal.model.Course.ContentItem previewText = 
            new com.example.appdevelopmentprojectfinal.model.Course.ContentItem();
        previewText.setType("text");
        previewText.setTitle("About this Course");
        previewText.setContent("This course will teach you everything you need to know about " + module.getName() + 
                              ". You'll learn key concepts, practical skills, and gain hands-on experience through " +
                              "projects and examples.");
        previewItems.add(previewText);
        
        preview.setItems(previewItems);
        content.setPreview(preview);
        
        // Create chapters (2-3 chapters per course)
        List<com.example.appdevelopmentprojectfinal.model.Course.Chapter> chapters = new ArrayList<>();
        int numChapters = (int)(Math.random() * 2) + 2; // 2-3 chapters
        
        for (int i = 1; i <= numChapters; i++) {
            com.example.appdevelopmentprojectfinal.model.Course.Chapter chapter = 
                new com.example.appdevelopmentprojectfinal.model.Course.Chapter();
            
            if (i == 1) {
                chapter.setTitle("Chapter 1: Introduction to " + module.getName());
            } else {
                chapter.setTitle("Chapter " + i + ": Advanced Concepts");
            }
            
            // Create lessons (2-3 lessons per chapter)
            List<com.example.appdevelopmentprojectfinal.model.Course.ContentItem> lessons = new ArrayList<>();
            int numLessons = (int)(Math.random() * 2) + 2; // 2-3 lessons
            
            for (int j = 1; j <= numLessons; j++) {
                // Add a video lesson
                com.example.appdevelopmentprojectfinal.model.Course.ContentItem videoLesson = 
                    new com.example.appdevelopmentprojectfinal.model.Course.ContentItem();
                videoLesson.setType("video");
                videoLesson.setTitle("Lesson " + j + ": Video Lecture");
                videoLesson.setUrl(demoVideoUrl);
                videoLesson.setCaption("Chapter " + i + " - Lesson " + j);
                lessons.add(videoLesson);
                
                // Add a text lesson
                com.example.appdevelopmentprojectfinal.model.Course.ContentItem textLesson = 
                    new com.example.appdevelopmentprojectfinal.model.Course.ContentItem();
                textLesson.setType("text");
                textLesson.setTitle("Lesson " + j + ": Reading Material");
                textLesson.setContent("This lesson covers important concepts related to " + module.getName() + 
                                     ".\n\nIn computer science, a data structure is a data organization, management, " +
                                     "and storage format that enables efficient access and modification. More precisely, " +
                                     "a data structure is a collection of data values, the relationships among them, " +
                                     "and the functions or operations that can be applied to the data.\n\n" +
                                     "## Example Code\n\n```java\npublic class Example {\n    " +
                                     "public static void main(String[] args) {\n        " +
                                     "System.out.println(\"This is an example for " + module.getName() + "\");\n    }\n}\n```");
                lessons.add(textLesson);
            }
            
            chapter.setItems(lessons);
            chapters.add(chapter);
        }
        
        content.setChapters(chapters);
        course.setContent(content);
        
        // Save the course to Firestore
        coursesCollection.document(courseId).set(course)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Course created successfully: " + courseId);
                if (listener != null) {
                    listener.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating course: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            });
    }
    
    // Helper method for creating course content
    private String getRandomName() {
        String[] firstNames = {"John", "Jane", "David", "Michael", "Sarah"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Jones", "Brown"};
        
        String firstName = firstNames[(int)(Math.random() * firstNames.length)];
        String lastName = lastNames[(int)(Math.random() * lastNames.length)];
        
        return firstName + " " + lastName;
    }
    
    /**
     * Listener interface for course operations
     */
    public interface OnCourseOperationListener {
        void onSuccess();
        void onError(String errorMessage);
    }
}