package com.example.appdevelopmentprojectfinal;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.appdevelopmentprojectfinal.marketplace.MarketplaceFirestoreManager;
import com.example.appdevelopmentprojectfinal.marketplace.AllCoursesFragment;
import com.example.appdevelopmentprojectfinal.marketplace.OwnedCoursesFragment;
import com.example.appdevelopmentprojectfinal.marketplace.AuthoredCoursesFragment;
import com.example.appdevelopmentprojectfinal.marketplace.CourseCreationDialog;
import com.example.appdevelopmentprojectfinal.model.User;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

// Shows marketplace with tabs for different course categories
public class StoreFragment extends Fragment {
    private static final String TAG = "TimetableApp:StoreFragment";
    
    // Tab index argument key
    private static final String ARG_TAB_INDEX = "tab_index";

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private int initialTabIndex = 0;

    public StoreFragment() {
        // Required empty public constructor
    }
    
    /**
     * Creates a new instance with specified tab to show
     * @param tabIndex index of tab to show initially (0 for All, 1 for Owned)
     * @return new fragment instance
     */
    public static StoreFragment newInstance(int tabIndex) {
        StoreFragment fragment = new StoreFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAB_INDEX, tabIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if we should open a specific tab
        if (getArguments() != null) {
            initialTabIndex = getArguments().getInt(ARG_TAB_INDEX, 0);
            Log.d(TAG, "Creating StoreFragment with initial tab index: " + initialTabIndex);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_store, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "Initializing StoreFragment");

        // Initialize views
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);
        
        // Initialize FABs
        View fabAddCourse = requireActivity().findViewById(R.id.fabAddCourse);
        if (fabAddCourse != null) {
            fabAddCourse.setVisibility(View.GONE); // Hidden by default
            fabAddCourse.setOnClickListener(v -> {
                // Open course creation dialog if we're on the authored tab
                if (viewPager.getCurrentItem() == 2) {
                    CourseCreationDialog dialog = new CourseCreationDialog();
                    dialog.show(getChildFragmentManager(), "CourseCreation");
                }
            });
        }

        // Initialize user data from Firestore
        initializeUserData();
    }
    
    private void initializeUserData() {
        Log.i(TAG, "Loading user data from Firestore");
        
        // Get current Firebase user
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Log.e(TAG, "Cannot initialize user data: no authenticated user");
            Toast.makeText(getContext(), "Please login to access the marketplace", Toast.LENGTH_SHORT).show();
            setUpViewPager();
            return;
        }
        
        String userId = firebaseUser.getUid();
        
        // Fetch the latest user data directly from Firestore to ensure we have up-to-date information
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        // Update cached user in MarketplaceFirestoreManager
                        MarketplaceFirestoreManager.getInstance().setCurrentUser(user);
                        Log.i(TAG, "User loaded and cached successfully from Firestore: " + user.getEmail());
                        
                        // Set up ViewPager with tabs
                        setUpViewPager();
                    } else {
                        Log.e(TAG, "Failed to parse user data from Firestore");
                        Toast.makeText(getContext(), "Error loading marketplace data", Toast.LENGTH_SHORT).show();
                        setUpViewPager();
                    }
                } else {
                    Log.e(TAG, "User document not found in Firestore");
                    Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                    setUpViewPager();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading user data: " + e.getMessage());
                Toast.makeText(getContext(), "Error loading marketplace data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                setUpViewPager();
            });
    }

    // Setup tabs for viewpager
    private void setUpViewPager() {
        if (!isAdded()) {
            return;
        }
        
        Log.d(TAG, "Setting up ViewPager and TabLayout");
        
        StoreTabAdapter adapter = new StoreTabAdapter(requireActivity().getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(adapter);

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(getString(R.string.all));
                    break;
                case 1:
                    tab.setText(getString(R.string.owned));
                    break;
                case 2:
                    tab.setText(getString(R.string.authored));
                    break;
            }
        }).attach();
        
        // Set initial tab if specified
        if (initialTabIndex > 0 && initialTabIndex < adapter.getItemCount()) {
            Log.d(TAG, "Setting initial tab to index: " + initialTabIndex);
            viewPager.postDelayed(() -> {
                viewPager.setCurrentItem(initialTabIndex, false);
                
                // If initial tab is Authored Courses (index 2), show the Add Course FAB
                if (initialTabIndex == 2) {
                    View fabSearch = requireActivity().findViewById(R.id.fabSearch);
                    View fabAddCourse = requireActivity().findViewById(R.id.fabAddCourse);
                    
                    if (fabSearch != null) fabSearch.setVisibility(View.GONE);
                    if (fabAddCourse != null) fabAddCourse.setVisibility(View.VISIBLE);
                }
            }, 100); // Short delay to ensure ViewPager is fully initialized
        }
        
        // Add a tab selection listener to refresh fragments when tab is selected
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.d(TAG, "Tab selected: " + tab.getPosition());
                int position = tab.getPosition();
                
                // Handle FAB visibility based on selected tab
                View fabSearch = requireActivity().findViewById(R.id.fabSearch);
                View fabAddCourse = requireActivity().findViewById(R.id.fabAddCourse);
                
                if (position == 2) { // Authored tab
                    // Show Add Course FAB, hide Search FAB
                    if (fabSearch != null) fabSearch.setVisibility(View.GONE);
                    if (fabAddCourse != null) fabAddCourse.setVisibility(View.VISIBLE);
                } else {
                    // Show Search FAB, hide Add Course FAB
                    if (fabSearch != null) fabSearch.setVisibility(View.VISIBLE);
                    if (fabAddCourse != null) fabAddCourse.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // When a tab is reselected, force refresh its fragment
                int position = tab.getPosition();
                Log.d(TAG, "Tab reselected: " + position);
                
                if (position == 0) {
                    // Refresh All Courses tab
                    Fragment fragment = getChildFragmentManager().findFragmentByTag("f" + position);
                    if (fragment instanceof AllCoursesFragment) {
                        ((AllCoursesFragment) fragment).refreshData();
                    }
                } else if (position == 1) {
                    // Refresh Owned Courses tab
                    Fragment fragment = getChildFragmentManager().findFragmentByTag("f" + position);
                    if (fragment instanceof OwnedCoursesFragment) {
                        ((OwnedCoursesFragment) fragment).refreshData();
                    }
                } else if (position == 2) {
                    // Refresh Authored Courses tab
                    Fragment fragment = getChildFragmentManager().findFragmentByTag("f" + position);
                    if (fragment instanceof AuthoredCoursesFragment) {
                        ((AuthoredCoursesFragment) fragment).refreshData();
                    }
                }
            }
        });
    }

    // Tab adapter for marketplace sections
    private static class StoreTabAdapter extends FragmentStateAdapter {
        public StoreTabAdapter(@NonNull FragmentManager fragmentManager, @NonNull androidx.lifecycle.Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        // Create fragment based on tab position
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Log.d(TAG, "Creating fragment for tab position: " + position);
            switch (position) {
                case 0:
                    return new AllCoursesFragment();
                case 1:
                    return new OwnedCoursesFragment();
                case 2:
                    return new AuthoredCoursesFragment();
                default:
                    return new AllCoursesFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3; // Number of tabs (All, Owned, and Authored)
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "StoreFragment resumed");
        
        // Refresh user data when fragment is resumed
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            Log.d(TAG, "Refreshing user data on resume: " + userId);
            
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // Update cached user in MarketplaceFirestoreManager
                            MarketplaceFirestoreManager.getInstance().setCurrentUser(user);
                            Log.d(TAG, "User data refreshed on resume");
                        }
                    }
                })
                .addOnFailureListener(e -> 
                    Log.e(TAG, "Error refreshing user data on resume: " + e.getMessage()));
        }
    }
}