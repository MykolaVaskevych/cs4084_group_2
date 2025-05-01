package com.example.appdevelopmentprojectfinal;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.appdevelopmentprojectfinal.auth.AuthManager;
import com.example.appdevelopmentprojectfinal.auth.LoginActivity;
import com.example.appdevelopmentprojectfinal.model.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
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
    private TextInputEditText editDepartment;
    private TextInputEditText editCourse;
    private TextInputEditText editYear;
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
    
    // Firebase components
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private AuthManager authManager;
    
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
        
        // Initialize currency formatter
        currencyFormatter = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        currencyFormatter.setCurrency(Currency.getInstance("EUR"));
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
        editDepartment = view.findViewById(R.id.edit_department);
        editCourse = view.findViewById(R.id.edit_course);
        editYear = view.findViewById(R.id.edit_year);
        btnSaveAcademic = view.findViewById(R.id.btn_save_academic);
        
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
        
        // Set academic information
        editDepartment.setText(currentUser.getDepartment());
        editCourse.setText(currentUser.getCourse());
        editYear.setText(String.valueOf(currentUser.getYear()));
        
        // Set wallet balance
        updateWalletDisplay();
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
        
        String department = editDepartment.getText().toString().trim();
        String course = editCourse.getText().toString().trim();
        String yearStr = editYear.getText().toString().trim();
        
        // Validate input
        if (TextUtils.isEmpty(department)) {
            editDepartment.setError("Department is required");
            return;
        }
        
        if (TextUtils.isEmpty(course)) {
            editCourse.setError("Course is required");
            return;
        }
        
        if (TextUtils.isEmpty(yearStr)) {
            editYear.setError("Year is required");
            return;
        }
        
        int year;
        try {
            year = Integer.parseInt(yearStr);
            if (year < 1 || year > 6) {
                editYear.setError("Year must be between 1 and 6");
                return;
            }
        } catch (NumberFormatException e) {
            editYear.setError("Year must be a number");
            return;
        }
        
        // Update user object
        currentUser.setDepartment(department);
        currentUser.setCourse(course);
        currentUser.setYear(year);
        
        // Save to Firestore
        saveUserToFirestore("Academic information updated successfully");
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
        editDepartment.setEnabled(enabled);
        editCourse.setEnabled(enabled);
        editYear.setEnabled(enabled);
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