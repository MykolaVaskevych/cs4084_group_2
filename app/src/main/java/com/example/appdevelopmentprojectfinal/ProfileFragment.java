package com.example.appdevelopmentprojectfinal;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.appdevelopmentprojectfinal.auth.AuthManager;
import com.example.appdevelopmentprojectfinal.auth.LoginActivity;
import com.example.appdevelopmentprojectfinal.model.ModuleSelectionAdapter;
import com.example.appdevelopmentprojectfinal.model.User;
import com.example.appdevelopmentprojectfinal.model.UserModulesAdapter;
import com.example.appdevelopmentprojectfinal.utils.AcademicDatabaseManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment for displaying and editing user profile information.
 * Includes functionality for updating personal info, academic info,
 * changing password, topping up wallet, and logging out.
 * 
 * @author Mykola Vaskevych (22372199)
 */
public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    
    // UI Components
    private TextInputEditText editFirstName;
    private TextInputEditText editLastName;
    private TextInputEditText editEmail;
    private TextInputEditText editPhone;
    private AutoCompleteTextView dropdownDepartment;
    private AutoCompleteTextView dropdownCourse;
    private AutoCompleteTextView dropdownYear;
    private TextInputEditText editCurrentPassword;
    private TextInputEditText editNewPassword;
    private TextInputEditText editConfirmPassword;
    private TextInputEditText editTopupAmount;
    private TextView textWalletBalance;
    private Button btnSaveProfile;
    private Button btnSaveAcademic;
    private Button btnChangePassword;
    private Button btnTopup;
    private Button btnLogout;
    private Button btnAddModules;
    private RecyclerView recyclerUserModules;
    
    // Firebase components
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private AuthManager authManager;
    private AcademicDatabaseManager academicDbManager;
    
    // Adapters
    private UserModulesAdapter userModulesAdapter;
    
    // Academic data
    private List<AcademicDatabaseManager.Department> departments;
    private List<AcademicDatabaseManager.Course> courses;
    private List<AcademicDatabaseManager.Year> years;
    
    // Selected academic items
    private AcademicDatabaseManager.Department selectedDepartment;
    private AcademicDatabaseManager.Course selectedCourse;
    private AcademicDatabaseManager.Year selectedYear;
    
    // User data
    private User currentUser;
    private NumberFormat currencyFormatter;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize Firebase components
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        authManager = AuthManager.getInstance();
        academicDbManager = AcademicDatabaseManager.getInstance();
        
        // Initialize lists
        departments = new ArrayList<>();
        courses = new ArrayList<>();
        years = new ArrayList<>();
        
        // Initialize currency formatter
        currencyFormatter = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        currencyFormatter.setCurrency(Currency.getInstance("EUR"));
        
        // Initialize the academic database collections
        academicDbManager.initializeDatabaseIfNeeded();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize UI components
        initializeViews(view);
        
        // Set up button click listeners
        setupListeners();
        
        // Load user data
        loadUserData();
    }
    
    /**
     * Initialize all the UI components
     */
    private void initializeViews(View view) {
        // Personal information fields
        editFirstName = view.findViewById(R.id.edit_first_name);
        editLastName = view.findViewById(R.id.edit_last_name);
        editEmail = view.findViewById(R.id.edit_email);
        editPhone = view.findViewById(R.id.edit_phone);
        btnSaveProfile = view.findViewById(R.id.btn_save_profile);
        
        // Academic information fields
        dropdownDepartment = view.findViewById(R.id.dropdown_department);
        dropdownCourse = view.findViewById(R.id.dropdown_course);
        dropdownYear = view.findViewById(R.id.dropdown_year);
        btnSaveAcademic = view.findViewById(R.id.btn_save_academic);
        
        // Module fields
        recyclerUserModules = view.findViewById(R.id.recycler_user_modules);
        btnAddModules = view.findViewById(R.id.btn_add_modules);
        
        // Setup RecyclerView for user modules
        recyclerUserModules.setLayoutManager(new LinearLayoutManager(getContext()));
        userModulesAdapter = new UserModulesAdapter(this::removeUserModule);
        recyclerUserModules.setAdapter(userModulesAdapter);
        
        // Wallet fields
        textWalletBalance = view.findViewById(R.id.text_wallet_balance);
        editTopupAmount = view.findViewById(R.id.edit_topup_amount);
        btnTopup = view.findViewById(R.id.btn_topup);
        
        // Password change fields
        editCurrentPassword = view.findViewById(R.id.edit_current_password);
        editNewPassword = view.findViewById(R.id.edit_new_password);
        editConfirmPassword = view.findViewById(R.id.edit_confirm_password);
        btnChangePassword = view.findViewById(R.id.btn_change_password);
        
        // Logout button
        btnLogout = view.findViewById(R.id.btn_logout);
    }
    
    /**
     * Set up all button click listeners
     */
    private void setupListeners() {
        // Save personal information
        btnSaveProfile.setOnClickListener(v -> updatePersonalInfo());
        
        // Save academic information
        btnSaveAcademic.setOnClickListener(v -> updateAcademicInfo());
        
        // Change password
        btnChangePassword.setOnClickListener(v -> changePassword());
        
        // Top up wallet
        btnTopup.setOnClickListener(v -> topupWallet());
        
        // Logout
        btnLogout.setOnClickListener(v -> logoutUser());
        
        // Add modules
        btnAddModules.setOnClickListener(v -> showModuleSelectionDialog());
        
        // Setup dropdown selection listeners
        setupDropdownListeners();
    }
    
    /**
     * Set up dropdown menu listeners
     */
    private void setupDropdownListeners() {
        // Department dropdown
        dropdownDepartment.setOnItemClickListener((parent, view, position, id) -> {
            selectedDepartment = departments.get(position);
            loadCourses(selectedDepartment.getId());
            dropdownCourse.setText("", false);
            dropdownYear.setText("", false);
        });
        
        // Course dropdown
        dropdownCourse.setOnItemClickListener((parent, view, position, id) -> {
            selectedCourse = courses.get(position);
            loadYears(selectedCourse.getId());
            dropdownYear.setText("", false);
        });
        
        // Year dropdown
        dropdownYear.setOnItemClickListener((parent, view, position, id) -> {
            selectedYear = years.get(position);
        });
    }
    
    /**
     * Load user data from Firebase
     */
    private void loadUserData() {
        if (!authManager.isUserSignedIn()) {
            Log.w(TAG, "No user is signed in");
            redirectToLogin();
            return;
        }
        
        // Show loading state (could add progress indicator)
        setFieldsEnabled(false);
        
        authManager.loadUserData(new AuthManager.UserDataListener() {
            @Override
            public void onUserDataLoaded(User user) {
                if (getActivity() == null || !isAdded()) return;
                
                currentUser = user;
                populateUserData();
                setFieldsEnabled(true);
            }

            @Override
            public void onUserDataError(String errorMessage) {
                if (getActivity() == null || !isAdded()) return;
                
                Log.e(TAG, "Error loading user data: " + errorMessage);
                Toast.makeText(getContext(), "Error loading profile: " + errorMessage, Toast.LENGTH_SHORT).show();
                setFieldsEnabled(true);
            }
        });
    }
    
    /**
     * Populate form fields with user data
     */
    private void populateUserData() {
        if (currentUser == null) return;
        
        // Set personal information
        editFirstName.setText(currentUser.getFirstName());
        editLastName.setText(currentUser.getLastName());
        editEmail.setText(currentUser.getEmail());
        editPhone.setText(currentUser.getPhoneNumber());
        
        // Load departments for dropdowns
        loadDepartments();
        
        // Set wallet balance
        updateWalletDisplay();
        
        // Set user modules
        if (currentUser.getModules() != null) {
            userModulesAdapter.setModules(currentUser.getModules());
            
            // Load module details for each module code
            for (String moduleCode : currentUser.getModules()) {
                loadModuleDetails(moduleCode);
            }
        }
    }
    
    /**
     * Load departments for dropdown
     */
    private void loadDepartments() {
        academicDbManager.loadDepartments(new AcademicDatabaseManager.OnDepartmentsLoadedListener() {
            @Override
            public void onDepartmentsLoaded(List<AcademicDatabaseManager.Department> deptList) {
                if (!isAdded()) return;
                
                departments = deptList;
                
                // Create adapter for department dropdown
                ArrayAdapter<AcademicDatabaseManager.Department> adapter = 
                        new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, departments);
                dropdownDepartment.setAdapter(adapter);
                
                // Try to match user's department with one from the database
                if (currentUser.getDepartment() != null) {
                    for (int i = 0; i < departments.size(); i++) {
                        if (departments.get(i).getName().equals(currentUser.getDepartment())) {
                            dropdownDepartment.setText(departments.get(i).toString(), false);
                            selectedDepartment = departments.get(i);
                            loadCourses(selectedDepartment.getId());
                            break;
                        }
                    }
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                Log.e(TAG, "Error loading departments: " + errorMessage);
                Toast.makeText(getContext(), "Error loading departments", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Load courses for selected department
     */
    private void loadCourses(String departmentId) {
        academicDbManager.loadCourses(departmentId, new AcademicDatabaseManager.OnCoursesLoadedListener() {
            @Override
            public void onCoursesLoaded(List<AcademicDatabaseManager.Course> courseList) {
                if (!isAdded()) return;
                
                courses = courseList;
                
                // Create adapter for course dropdown
                ArrayAdapter<AcademicDatabaseManager.Course> adapter = 
                        new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, courses);
                dropdownCourse.setAdapter(adapter);
                
                // Try to match user's course with one from the database
                if (currentUser.getCourse() != null) {
                    for (int i = 0; i < courses.size(); i++) {
                        if (courses.get(i).getCode().equals(currentUser.getCourse())) {
                            dropdownCourse.setText(courses.get(i).toString(), false);
                            selectedCourse = courses.get(i);
                            loadYears(selectedCourse.getId());
                            break;
                        }
                    }
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                Log.e(TAG, "Error loading courses: " + errorMessage);
                Toast.makeText(getContext(), "Error loading courses", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Load years for selected course
     */
    private void loadYears(String courseId) {
        academicDbManager.loadYears(courseId, new AcademicDatabaseManager.OnYearsLoadedListener() {
            @Override
            public void onYearsLoaded(List<AcademicDatabaseManager.Year> yearList) {
                if (!isAdded()) return;
                
                years = yearList;
                
                // Create adapter for year dropdown
                ArrayAdapter<AcademicDatabaseManager.Year> adapter = 
                        new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, years);
                dropdownYear.setAdapter(adapter);
                
                // Try to match user's year with one from the database
                if (currentUser.getYear() > 0) {
                    for (int i = 0; i < years.size(); i++) {
                        if (years.get(i).getYearNumber() == currentUser.getYear()) {
                            dropdownYear.setText(years.get(i).toString(), false);
                            selectedYear = years.get(i);
                            break;
                        }
                    }
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                Log.e(TAG, "Error loading years: " + errorMessage);
                Toast.makeText(getContext(), "Error loading years", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Load module details by code
     */
    private void loadModuleDetails(String moduleCode) {
        academicDbManager.getModuleByCode(moduleCode, new AcademicDatabaseManager.OnModulesLoadedListener() {
            @Override
            public void onModulesLoaded(List<AcademicDatabaseManager.Module> modules) {
                if (!isAdded() || modules.isEmpty()) return;
                
                // Update the module data in the adapter
                userModulesAdapter.updateModuleData(modules.get(0));
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "Error loading module details for " + moduleCode + ": " + errorMessage);
                // Not showing error to user as this is not critical
            }
        });
    }
    
    /**
     * Update personal information
     */
    private void updatePersonalInfo() {
        if (currentUser == null || !isAdded()) return;
        
        String firstName = editFirstName.getText().toString().trim();
        String lastName = editLastName.getText().toString().trim();
        String phoneNumber = editPhone.getText().toString().trim();
        
        // Validate input
        if (TextUtils.isEmpty(firstName)) {
            editFirstName.setError("First name is required");
            return;
        }
        
        if (TextUtils.isEmpty(lastName)) {
            editLastName.setError("Last name is required");
            return;
        }
        
        // Update user object
        currentUser.setFirstName(firstName);
        currentUser.setLastName(lastName);
        currentUser.setPhoneNumber(phoneNumber);
        
        // Save to Firestore
        saveUserToFirestore("Personal information updated successfully");
    }
    
    /**
     * Update academic information
     */
    private void updateAcademicInfo() {
        if (currentUser == null || !isAdded()) return;
        
        // Validate input
        if (selectedDepartment == null) {
            dropdownDepartment.setError("Department is required");
            return;
        }
        
        if (selectedCourse == null) {
            dropdownCourse.setError("Course is required");
            return;
        }
        
        if (selectedYear == null) {
            dropdownYear.setError("Year is required");
            return;
        }
        
        // Update user object
        currentUser.setDepartment(selectedDepartment.getName());
        currentUser.setCourse(selectedCourse.getCode());
        currentUser.setYear(selectedYear.getYearNumber());
        
        // Save to Firestore
        saveUserToFirestore("Academic information updated successfully");
    }
    
    /**
     * Show module selection dialog
     */
    private void showModuleSelectionDialog() {
        if (currentUser == null) return;
        
        // Create dialog
        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_module_selection);
        
        // Initialize dialog views
        AutoCompleteTextView dialogDeptDropdown = dialog.findViewById(R.id.dropdown_department);
        AutoCompleteTextView dialogCourseDropdown = dialog.findViewById(R.id.dropdown_course);
        AutoCompleteTextView dialogYearDropdown = dialog.findViewById(R.id.dropdown_year);
        TextInputEditText editSearchModule = dialog.findViewById(R.id.edit_search_module);
        RecyclerView recyclerModules = dialog.findViewById(R.id.recycler_modules);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnAddSelected = dialog.findViewById(R.id.btn_add_selected);
        
        // Module selection adapter
        ModuleSelectionAdapter moduleSelectionAdapter = new ModuleSelectionAdapter();
        recyclerModules.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerModules.setAdapter(moduleSelectionAdapter);
        
        // Set user's current modules
        moduleSelectionAdapter.setUserModules(currentUser.getModules());
        
        // Populate department dropdown with existing data
        if (!departments.isEmpty()) {
            ArrayAdapter<AcademicDatabaseManager.Department> deptAdapter = 
                    new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, departments);
            dialogDeptDropdown.setAdapter(deptAdapter);
            
            // Set current selection if available
            if (selectedDepartment != null) {
                dialogDeptDropdown.setText(selectedDepartment.toString(), false);
                
                // Load courses for selected department
                loadDialogCourses(dialogCourseDropdown, dialogYearDropdown, selectedDepartment.getId());
            }
        }
        
        // Department selection listener
        dialogDeptDropdown.setOnItemClickListener((parent, view, position, id) -> {
            AcademicDatabaseManager.Department department = departments.get(position);
            loadDialogCourses(dialogCourseDropdown, dialogYearDropdown, department.getId());
        });
        
        // Course selection listener
        dialogCourseDropdown.setOnItemClickListener((parent, view, position, id) -> {
            AcademicDatabaseManager.Course course = 
                    (AcademicDatabaseManager.Course) parent.getItemAtPosition(position);
            loadDialogYears(dialogYearDropdown, course.getId());
        });
        
        // Year selection listener
        dialogYearDropdown.setOnItemClickListener((parent, view, position, id) -> {
            AcademicDatabaseManager.Year year = 
                    (AcademicDatabaseManager.Year) parent.getItemAtPosition(position);
            loadModulesForYear(moduleSelectionAdapter, year.getId());
        });
        
        // Search text change listener
        editSearchModule.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() >= 2) {
                    // Search for modules
                    searchModules(moduleSelectionAdapter, s.toString());
                }
            }
        });
        
        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        // Add selected button
        btnAddSelected.setOnClickListener(v -> {
            List<String> selectedModules = moduleSelectionAdapter.getSelectedModuleCodes();
            if (!selectedModules.isEmpty()) {
                // Add selected modules to user's modules
                if (currentUser.getModules() == null) {
                    currentUser.setModules(new ArrayList<>());
                }
                
                // Add new modules
                for (String moduleCode : selectedModules) {
                    if (!currentUser.getModules().contains(moduleCode)) {
                        currentUser.getModules().add(moduleCode);
                        loadModuleDetails(moduleCode);
                    }
                }
                
                // Update the adapter
                userModulesAdapter.setModules(currentUser.getModules());
                
                // Save to Firestore
                saveUserToFirestore("Modules updated successfully");
                
                // Dismiss dialog
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "No modules selected", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Show dialog
        dialog.show();
    }
    
    /**
     * Load courses for the module selection dialog
     */
    private void loadDialogCourses(AutoCompleteTextView courseDropdown, AutoCompleteTextView yearDropdown, String departmentId) {
        academicDbManager.loadCourses(departmentId, new AcademicDatabaseManager.OnCoursesLoadedListener() {
            @Override
            public void onCoursesLoaded(List<AcademicDatabaseManager.Course> courseList) {
                if (!isAdded()) return;
                
                // Create adapter for course dropdown
                ArrayAdapter<AcademicDatabaseManager.Course> adapter = 
                        new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, courseList);
                courseDropdown.setAdapter(adapter);
                
                // Clear year dropdown
                yearDropdown.setText("", false);
            }
            
            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                Log.e(TAG, "Error loading courses: " + errorMessage);
                Toast.makeText(getContext(), "Error loading courses", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Load years for the module selection dialog
     */
    private void loadDialogYears(AutoCompleteTextView yearDropdown, String courseId) {
        academicDbManager.loadYears(courseId, new AcademicDatabaseManager.OnYearsLoadedListener() {
            @Override
            public void onYearsLoaded(List<AcademicDatabaseManager.Year> yearList) {
                if (!isAdded()) return;
                
                // Create adapter for year dropdown
                ArrayAdapter<AcademicDatabaseManager.Year> adapter = 
                        new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, yearList);
                yearDropdown.setAdapter(adapter);
            }
            
            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                Log.e(TAG, "Error loading years: " + errorMessage);
                Toast.makeText(getContext(), "Error loading years", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Load modules for selected year
     */
    private void loadModulesForYear(ModuleSelectionAdapter adapter, String yearId) {
        academicDbManager.loadModulesByYear(yearId, new AcademicDatabaseManager.OnModulesLoadedListener() {
            @Override
            public void onModulesLoaded(List<AcademicDatabaseManager.Module> modules) {
                if (!isAdded()) return;
                adapter.setModules(modules);
            }
            
            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                Log.e(TAG, "Error loading modules: " + errorMessage);
                Toast.makeText(getContext(), "Error loading modules", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Search for modules by query
     */
    private void searchModules(ModuleSelectionAdapter adapter, String query) {
        academicDbManager.searchModules(query, new AcademicDatabaseManager.OnModulesLoadedListener() {
            @Override
            public void onModulesLoaded(List<AcademicDatabaseManager.Module> modules) {
                if (!isAdded()) return;
                adapter.setModules(modules);
            }
            
            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                Log.e(TAG, "Error searching modules: " + errorMessage);
                Toast.makeText(getContext(), "Error searching modules", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Remove a module from user's modules
     */
    private void removeUserModule(String moduleCode) {
        if (currentUser == null || currentUser.getModules() == null) return;
        
        // Show confirmation dialog
        new AlertDialog.Builder(getContext())
                .setTitle("Remove Module")
                .setMessage("Are you sure you want to remove " + moduleCode + " from your modules?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    // Remove module
                    currentUser.getModules().remove(moduleCode);
                    
                    // Update adapter
                    userModulesAdapter.setModules(currentUser.getModules());
                    
                    // Save to Firestore
                    saveUserToFirestore("Module removed successfully");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Change user password
     */
    private void changePassword() {
        if (!isAdded() || !authManager.isUserSignedIn()) return;
        
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get input values
        String currentPassword = editCurrentPassword.getText().toString().trim();
        String newPassword = editNewPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();
        
        // Validate input
        if (TextUtils.isEmpty(currentPassword)) {
            editCurrentPassword.setError("Current password is required");
            return;
        }
        
        if (TextUtils.isEmpty(newPassword)) {
            editNewPassword.setError("New password is required");
            return;
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            editConfirmPassword.setError("Please confirm your new password");
            return;
        }
        
        if (newPassword.length() < 6) {
            editNewPassword.setError("Password must be at least 6 characters");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            editConfirmPassword.setError("Passwords do not match");
            return;
        }
        
        // Show loading state (could add progress indicator)
        setFieldsEnabled(false);
        
        // Reauthenticate the user
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Update password
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid1 -> {
                                if (!isAdded()) return;
                                
                                Toast.makeText(getContext(), "Password updated successfully", Toast.LENGTH_SHORT).show();
                                
                                // Clear password fields
                                editCurrentPassword.setText("");
                                editNewPassword.setText("");
                                editConfirmPassword.setText("");
                                
                                setFieldsEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                
                                Log.e(TAG, "Error updating password", e);
                                Toast.makeText(getContext(), "Error updating password: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                                setFieldsEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    
                    Log.e(TAG, "Error reauthenticating user", e);
                    Toast.makeText(getContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show();
                    setFieldsEnabled(true);
                });
    }
    
    /**
     * Top up user wallet
     */
    private void topupWallet() {
        if (currentUser == null || !isAdded()) return;
        
        String amountStr = editTopupAmount.getText().toString().trim();
        
        // Validate input
        if (TextUtils.isEmpty(amountStr)) {
            editTopupAmount.setError("Amount is required");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                editTopupAmount.setError("Amount must be greater than zero");
                return;
            }
            if (amount > 1000) {
                editTopupAmount.setError("Amount cannot exceed €1000 per transaction");
                return;
            }
        } catch (NumberFormatException e) {
            editTopupAmount.setError("Please enter a valid amount");
            return;
        }
        
        // Update user's wallet
        currentUser.setWallet(currentUser.getWallet() + amount);
        
        // Update display
        updateWalletDisplay();
        
        // Save to Firestore
        saveUserToFirestore("Wallet topped up successfully");
        
        // Clear the amount field
        editTopupAmount.setText("");
    }
    
    /**
     * Update the wallet balance display
     */
    private void updateWalletDisplay() {
        if (currentUser != null && textWalletBalance != null) {
            String formattedBalance = currencyFormatter.format(currentUser.getWallet());
            textWalletBalance.setText("Current Balance: " + formattedBalance);
        }
    }
    
    /**
     * Save user data to Firestore
     */
    private void saveUserToFirestore(final String successMessage) {
        if (currentUser == null || !isAdded()) return;
        
        // Show loading state (could add progress indicator)
        setFieldsEnabled(false);
        
        // Get user ID (email for non-anonymous users)
        String userId = currentUser.getEmail();
        
        // Convert user to map for update
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", currentUser.getFirstName());
        updates.put("lastName", currentUser.getLastName());
        updates.put("phoneNumber", currentUser.getPhoneNumber());
        updates.put("department", currentUser.getDepartment());
        updates.put("course", currentUser.getCourse());
        updates.put("year", currentUser.getYear());
        updates.put("wallet", currentUser.getWallet());
        updates.put("modules", currentUser.getModules()); // Add modules to the update
        
        // Update Firestore document
        firestore.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    
                    Log.d(TAG, "User data updated successfully");
                    Toast.makeText(getContext(), successMessage, Toast.LENGTH_SHORT).show();
                    setFieldsEnabled(true);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    
                    Log.e(TAG, "Error updating user data", e);
                    Toast.makeText(getContext(), "Error updating data: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    setFieldsEnabled(true);
                });
    }
    
    /**
     * Set all input fields enabled/disabled
     */
    private void setFieldsEnabled(boolean enabled) {
        // Personal information fields
        editFirstName.setEnabled(enabled);
        editLastName.setEnabled(enabled);
        editPhone.setEnabled(enabled);
        btnSaveProfile.setEnabled(enabled);
        
        // Academic information fields
        dropdownDepartment.setEnabled(enabled);
        dropdownCourse.setEnabled(enabled);
        dropdownYear.setEnabled(enabled);
        btnSaveAcademic.setEnabled(enabled);
        
        // Wallet fields
        editTopupAmount.setEnabled(enabled);
        btnTopup.setEnabled(enabled);
        
        // Password change fields
        editCurrentPassword.setEnabled(enabled);
        editNewPassword.setEnabled(enabled);
        editConfirmPassword.setEnabled(enabled);
        btnChangePassword.setEnabled(enabled);
        
        // Logout button (always enabled)
        btnLogout.setEnabled(true);
    }
    
    /**
     * Log out the current user and redirect to login screen
     */
    private void logoutUser() {
        // Call AuthManager to sign out
        authManager.signOut();
        
        // Redirect to login activity
        redirectToLogin();
    }
    
    /**
     * Redirect to login activity
     */
    private void redirectToLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        
        // Close the current activity
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}