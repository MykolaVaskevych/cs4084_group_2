package com.example.appdevelopmentprojectfinal;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.RatingBar;
import android.view.Gravity;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.example.appdevelopmentprojectfinal.timetable.Module;
import com.example.appdevelopmentprojectfinal.timetable.TimeSlot;
import com.example.appdevelopmentprojectfinal.calendar.Event;
import com.example.appdevelopmentprojectfinal.databinding.FragmentHomepageBinding;
import com.example.appdevelopmentprojectfinal.model.Course;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.Collections;

public class HomepageFragment extends Fragment {

    private LinearLayout moduleListContainer;
    private LinearLayout eventListContainer;
    private ProgressBar loadingView;
    private LinearLayout emptyView;
    private TextView tvEmptyMessage;
    private FirebaseFirestore db;
    private String currentUserId;
    private FragmentHomepageBinding binding;

    private static final int[] MODULE_COLORS = {
            Color.parseColor("#FFCDD2"),
            Color.parseColor("#C8E6C9"),
            Color.parseColor("#BBDEFB"),
            Color.parseColor("#FFE0B2"),
            Color.parseColor("#E1BEE7"),
            Color.parseColor("#F8BBD0"),
            Color.parseColor("#D7CCC8"),
            Color.parseColor("#CFD8DC"),
            Color.parseColor("#B2EBF2"),
            Color.parseColor("#B3E5FC")
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_homepage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentHomepageBinding.bind(view);

        // Initialize views
        moduleListContainer = binding.moduleListContainer;
        eventListContainer = binding.eventListContainer;
        loadingView = binding.loadingView;
        emptyView = binding.emptyView;
        tvEmptyMessage = binding.tvEmptyMessage;

        // Load content in order: events, modules, then recommended course
        loadEvents();
        loadModules();
        loadRecommendedCourse();
    }

    private void loadModules() {
        // Show loading state
        binding.loadingView.setVisibility(View.VISIBLE);
        binding.emptyView.setVisibility(View.GONE);
        binding.moduleListContainer.removeAllViews();

        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d("HomepageFragment", "No user logged in");
            showEmptyView("Please log in to view your modules");
            return;
        }

        String userEmail = currentUser.getEmail();
        Log.d("HomepageFragment", "Current user email: " + userEmail);

        // Get current day and time
        Calendar now = Calendar.getInstance();
        int currentDay = now.get(Calendar.DAY_OF_WEEK);
        String currentTime = String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));

        // Convert Calendar day to our day format (Monday = 1, etc.)
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        String currentDayName = days[currentDay - 1];

        Log.d("HomepageFragment", "Current day: " + currentDayName + ", Current time: " + currentTime);

        // Get modules from Firestore
        FirebaseFirestore.getInstance()
                .collection("modules")
                .whereEqualTo("userId", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("HomepageFragment", "Number of modules found: " + queryDocumentSnapshots.size());

                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<Module> allModules = new ArrayList<>();
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Module module = document.toObject(Module.class);
                            if (module != null) {
                                module.setDocumentId(document.getId());
                                allModules.add(module);
                                Log.d("HomepageFragment", "Found module: " + module.getCode() +
                                        " with " + (module.getTimeSlotList() != null ? module.getTimeSlotList().size() : 0) + " time slots");
                            }
                        }

                        // Find the next upcoming module
                        Module nextModule = findNextModule(allModules, currentDayName, currentTime);
                        if (nextModule != null) {
                            Log.d("HomepageFragment", "Next module found: " + nextModule.getCode());
                            addModuleView(nextModule);
                        } else {
                            Log.d("HomepageFragment", "No upcoming modules found");
                            showEmptyView("No upcoming modules this week");
                        }
                    } else {
                        Log.d("HomepageFragment", "No modules found in Firestore");
                        showEmptyView("No modules found");
                    }
                    binding.loadingView.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e("HomepageFragment", "Error loading modules", e);
                    showEmptyView("Failed to load modules");
                    binding.loadingView.setVisibility(View.GONE);
                });
    }

    private Module findNextModule(List<Module> modules, String currentDay, String currentTime) {
        Log.d("HomepageFragment", "Finding next module from " + modules.size() + " modules");
        Module nextModule = null;
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        int currentDayIndex = -1;

        // Find current day index
        for (int i = 0; i < days.length; i++) {
            if (days[i].equals(currentDay)) {
                currentDayIndex = i;
                break;
            }
        }

        Log.d("HomepageFragment", "Current day index: " + currentDayIndex);

        // Check all days starting from current day
        for (int i = 0; i < days.length; i++) {
            int dayIndex = (currentDayIndex + i) % days.length;
            String dayToCheck = days[dayIndex];

            // If it's not the current day, reset time to "00:00"
            String timeToCheck = (i == 0) ? currentTime : "00:00";

            Log.d("HomepageFragment", "Checking day: " + dayToCheck + " with time: " + timeToCheck);

            for (Module module : modules) {
                List<TimeSlot> slots = module.getTimeSlotList();
                if (slots != null) {
                    for (TimeSlot slot : slots) {
                        Log.d("HomepageFragment", "Checking slot: " + slot.getDay() + " " + slot.getStartTime());
                        if (slot.getDay().equals(dayToCheck)) {
                            // If it's today, check if the time is in the future
                            if (i == 0 && slot.getStartTime().compareTo(timeToCheck) <= 0) {
                                Log.d("HomepageFragment", "Skipping past module: " + module.getCode());
                                continue;
                            }
                            // Found the next module
                            Log.d("HomepageFragment", "Found next module: " + module.getCode());
                            return module;
                        }
                    }
                }
            }
        }

        return nextModule;
    }

    private void loadRecommendedCourse() {
        // Show loading state
        binding.loadingView.setVisibility(View.VISIBLE);

        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d("HomepageFragment", "No user logged in for recommended course");
            binding.loadingView.setVisibility(View.GONE);
            return;
        }

        String userId = currentUser.getUid();
        Log.d("HomepageFragment", "Loading recommended course for user: " + userId);

        // First get user's purchased courses
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDocument -> {
                    final List<String> ownedCourseIds;
                    if (userDocument.exists() && userDocument.contains("ownedCourses")) {
                        ownedCourseIds = (List<String>) userDocument.get("ownedCourses");
                        Log.d("HomepageFragment", "User has " + ownedCourseIds.size() + " owned courses");
                    } else {
                        ownedCourseIds = new ArrayList<>();
                    }

                    // Get highest rated courses from marketplace
                    FirebaseFirestore.getInstance()
                            .collection("marketplace")
                            .orderBy("averageRating", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                Course recommendedCourse = null;

                                // Find the highest rated course not owned by the user
                                for (DocumentSnapshot document : queryDocumentSnapshots) {
                                    Course course = document.toObject(Course.class);
                                    if (course != null) {
                                        // Set the course ID from the document ID
                                        course.setId(document.getId());

                                        if (course.getAverageRating() >= 3.5) {
                                            Log.d("HomepageFragment", "Checking course: " + course.getName() +
                                                    " (ID: " + course.getId() + ")");
                                            Log.d("HomepageFragment", "Owned courses: " + ownedCourseIds);
                                            if (!ownedCourseIds.contains(course.getId())) {
                                                recommendedCourse = course;
                                                Log.d("HomepageFragment", "Selected course: " + course.getName() +
                                                        " (ID: " + course.getId() + ")");
                                                break;
                                            } else {
                                                Log.d("HomepageFragment", "Skipping owned course: " + course.getName() +
                                                        " (ID: " + course.getId() + ")");
                                            }
                                        }
                                    }
                                }

                                if (recommendedCourse != null) {
                                    Log.d("HomepageFragment", "Found recommended course: " + recommendedCourse.getName() +
                                            " with rating: " + recommendedCourse.getAverageRating());

                                    // Inflate the course card layout
                                    LayoutInflater inflater = LayoutInflater.from(requireContext());
                                    View courseCard = inflater.inflate(R.layout.item_course, binding.recommendedCourseContainer, false);

                                    // Set course details
                                    TextView tvCourseTitle = courseCard.findViewById(R.id.tvCourseTitle);
                                    TextView tvModuleCode = courseCard.findViewById(R.id.tvModuleCode);
                                    TextView tvAuthor = courseCard.findViewById(R.id.tvAuthor);
                                    TextView tvPrice = courseCard.findViewById(R.id.tvPrice);
                                    RatingBar ratingBar = courseCard.findViewById(R.id.ratingBar);
                                    TextView tvRating = courseCard.findViewById(R.id.tvRating);

                                    tvCourseTitle.setText(recommendedCourse.getName());
                                    tvModuleCode.setText(recommendedCourse.getRelatedModule());
                                    tvAuthor.setText("By: " + recommendedCourse.getAuthor());
                                    tvPrice.setText(String.format("€%.2f", recommendedCourse.getPrice()));
                                    ratingBar.setRating((float) recommendedCourse.getAverageRating());

                                    int numReviews = recommendedCourse.getReviews() != null ? recommendedCourse.getReviews().size() : 0;
                                    tvRating.setText(String.format("%.1f (%d)", recommendedCourse.getAverageRating(), numReviews));

                                    // Add the course card to the container
                                    binding.recommendedCourseContainer.addView(courseCard);
                                } else {
                                    Log.d("HomepageFragment", "No highly rated courses available that user doesn't own");
                                    TextView noCoursesText = new TextView(requireContext());
                                    noCoursesText.setText("You have all the courses we recommend!");
                                    noCoursesText.setTextSize(16);
                                    noCoursesText.setGravity(Gravity.CENTER);
                                    noCoursesText.setPadding(16, 16, 16, 16);
                                    binding.recommendedCourseContainer.addView(noCoursesText);
                                }
                                binding.loadingView.setVisibility(View.GONE);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("HomepageFragment", "Error loading recommended course: " + e.getMessage());
                                binding.loadingView.setVisibility(View.GONE);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("HomepageFragment", "Error loading user data: " + e.getMessage());
                    binding.loadingView.setVisibility(View.GONE);
                });
    }

    private void addModuleView(Module module) {
        // Inflate the module homepage layout
        View moduleView = LayoutInflater.from(requireContext()).inflate(R.layout.item_module_homepage, null);

        // Set up the card view
        CardView cardView = (CardView) moduleView;
        int colorIndex = (Integer.parseInt(module.getCode().replaceAll("[^0-9]", "")) % 100) % MODULE_COLORS.length;
        cardView.setCardBackgroundColor(MODULE_COLORS[colorIndex]);

        // Set module title (code and name)
        TextView titleText = moduleView.findViewById(R.id.module_title);
        titleText.setText(module.getCode() + ": " + module.getName());

        // Set lecturer
        TextView lecturerText = moduleView.findViewById(R.id.module_lecturer);
        lecturerText.setText(module.getLecturer());

        // Set schedule
        TextView scheduleText = moduleView.findViewById(R.id.module_schedule);
        TimeSlot timeSlot = module.getTimeSlotList().get(0); // Get first time slot
        scheduleText.setText(timeSlot.getDay() + " " +
                timeSlot.getStartTime() + "-" + timeSlot.getEndTime() +
                " @ " + timeSlot.getLocation());

        // Add click listener to show details
        cardView.setOnClickListener(v -> showModuleDetailsDialog(module));

        // Add to container
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(16, 8, 16, 8);
        moduleView.setLayoutParams(params);
        moduleListContainer.addView(moduleView);
    }

    private void showModuleDetailsDialog(Module module) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Module Details");

        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_module_details, null);
        builder.setView(dialogView);

        // Find views in dialog
        TextView textViewCode = dialogView.findViewById(R.id.textView_module_code);
        TextView textViewName = dialogView.findViewById(R.id.textView_module_name);
        TextView textViewLecturer = dialogView.findViewById(R.id.textView_module_lecturer);
        TextView textViewType = dialogView.findViewById(R.id.textView_module_type);
        TextView textViewSchedule = dialogView.findViewById(R.id.textView_module_schedule);

        // Set values
        textViewCode.setText(module.getCode());
        textViewName.setText(module.getName());
        textViewLecturer.setText(module.getLecturer());
        textViewType.setText(module.getType());

        // Format schedule
        StringBuilder scheduleBuilder = new StringBuilder();
        for (TimeSlot slot : module.getTimeSlotList()) {
            scheduleBuilder.append(slot.getDay())
                    .append(" ")
                    .append(slot.getStartTime())
                    .append("-")
                    .append(slot.getEndTime())
                    .append(" @ ")
                    .append(slot.getLocation())
                    .append("\n");
        }
        textViewSchedule.setText(scheduleBuilder.toString().trim());

        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showExampleModules() {
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        moduleListContainer.removeAllViews();

        // Example module 1
        Module module1 = new Module("CS4084", "Mobile Application Development", "Dr. Smith", true);
        TimeSlot timeSlot1 = new TimeSlot("Monday", "09:00", "11:00", "CSG-001");
        List<TimeSlot> slots1 = new ArrayList<>();
        slots1.add(timeSlot1);
        module1.setTimeSlotList(slots1);
        addModuleView(module1);

        // Example module 2
        Module module2 = new Module("CS4085", "Software Engineering", "Dr. Jones", true);
        TimeSlot timeSlot2 = new TimeSlot("Tuesday", "14:00", "16:00", "CSG-002");
        List<TimeSlot> slots2 = new ArrayList<>();
        slots2.add(timeSlot2);
        module2.setTimeSlotList(slots2);
        addModuleView(module2);
    }

    private void showExampleEvents() {
        // Create example events
        Calendar calendar = Calendar.getInstance();

        // Event 1: Today's meeting
        calendar.set(Calendar.HOUR_OF_DAY, 14);
        calendar.set(Calendar.MINUTE, 0);
        Event event1 = new Event("Team Meeting", "Weekly project update", calendar.getTime(), Event.TYPE_EVENT);
        addEventView(event1);

        // Event 2: Tomorrow's deadline
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        Event event2 = new Event("Project Deadline", "Submit final report", calendar.getTime(), Event.TYPE_EVENT);
        addEventView(event2);
    }

    private void addEventView(Event event) {
        // Inflate the event layout
        View eventView = LayoutInflater.from(requireContext()).inflate(R.layout.item_event, null);

        // Set up the card view
        CardView cardView = (CardView) eventView;

        // Set event type indicator color
        View typeIndicator = eventView.findViewById(R.id.view_event_type_indicator);
        typeIndicator.setBackgroundColor(getResources().getColor(R.color.red_500));

        // Set title
        TextView titleText = eventView.findViewById(R.id.tv_event_title);
        titleText.setText(event.getTitle());

        // Set description
        TextView descriptionText = eventView.findViewById(R.id.tv_event_description);
        descriptionText.setText(event.getDescription());

        // Set time
        TextView timeText = eventView.findViewById(R.id.tv_event_time);
        timeText.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(event.getDate()));

        // Set countdown
        TextView countdownText = eventView.findViewById(R.id.tv_event_countdown);
        String countdown = getCountdownText(event.getDate());
        countdownText.setText(countdown);
        setCountdownColor(countdownText, event.getDate());

        // Hide checkbox (we don't need it for events)
        eventView.findViewById(R.id.checkbox_todo_completed).setVisibility(View.GONE);

        // Add click listener to show details
        cardView.setOnClickListener(v -> showEventDetailsDialog(event));

        // Add to container
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(16, 8, 16, 8);
        eventView.setLayoutParams(params);
        eventListContainer.addView(eventView);
    }

    private void showEventDetailsDialog(Event event) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(event.isEvent() ? "Event Details" : "Todo Details");

        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_event_details, null);
        builder.setView(dialogView);

        // Find views in dialog
        TextView textViewTitle = dialogView.findViewById(R.id.textView_details_title);
        TextView textViewDescription = dialogView.findViewById(R.id.textView_details_description);
        TextView textViewDate = dialogView.findViewById(R.id.textView_details_date);
        TextView textViewTime = dialogView.findViewById(R.id.textView_details_time);

        // Set values
        textViewTitle.setText(event.getTitle());
        textViewDescription.setText(event.getDescription());

        // Format date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        textViewDate.setText(dateFormat.format(event.getDate()));
        textViewTime.setText(timeFormat.format(event.getDate()));

        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private String getCountdownText(Date eventDate) {
        Calendar now = Calendar.getInstance();

        // If date is in the past, just show "Expired"
        if (eventDate.getTime() < now.getTimeInMillis()) {
            return "Expired";
        }

        // Calculate time difference in milliseconds
        long diffInMillis = eventDate.getTime() - now.getTimeInMillis();

        // Convert to days, hours, minutes
        long days = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis) - TimeUnit.DAYS.toHours(days);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(diffInMillis));

        if (days > 0) {
            return days + " days";
        } else if (hours > 0) {
            return hours + " hours";
        } else {
            return minutes + " minutes";
        }
    }

    private void setCountdownColor(TextView textView, Date eventDate) {
        // Check if event is today but already expired
        if (eventDate.getTime() < System.currentTimeMillis()) {
            textView.setTextColor(getResources().getColor(R.color.gray_500));
            return;
        }

        // Calculate time difference in milliseconds
        long diffInMillis = eventDate.getTime() - System.currentTimeMillis();

        // Convert to days, hours
        long days = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis) - TimeUnit.DAYS.toHours(days);

        if (days == 0 && hours <= 3) {
            textView.setTextColor(getResources().getColor(R.color.red_500));
        } else if (days == 0) {
            textView.setTextColor(getResources().getColor(R.color.orange_500));
        } else if (days <= 2) {
            textView.setTextColor(getResources().getColor(R.color.orange_300));
        } else {
            textView.setTextColor(getResources().getColor(R.color.blue_500));
        }
    }

    private void showEmptyView(String message) {
        binding.emptyView.setVisibility(View.VISIBLE);
        binding.tvEmptyMessage.setText(message);
    }

    private void loadEvents() {
        // Show loading state
        binding.loadingView.setVisibility(View.VISIBLE);
        binding.eventListContainer.removeAllViews();

        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d("HomepageFragment", "No user logged in for events");
            return;
        }

        String userId = currentUser.getUid();
        Log.d("HomepageFragment", "Loading events for user ID: " + userId);

        // Get current time
        Calendar now = Calendar.getInstance();
        Log.d("HomepageFragment", "Current time for event filtering: " + now.getTime());

        // Get events from Firestore
        FirebaseFirestore.getInstance()
                .collection("events")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("HomepageFragment", "Number of events found: " + queryDocumentSnapshots.size());

                    List<Event> upcomingEvents = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        if (event != null) {
                            Log.d("HomepageFragment", "Found event: " + event.getTitle() +
                                    " with date: " + event.getDate() +
                                    " and userId: " + event.getUserId());

                            // Only show events that haven't happened yet
                            if (event.getDate().getTime() >= now.getTimeInMillis()) {
                                upcomingEvents.add(event);
                                Log.d("HomepageFragment", "Added upcoming event: " + event.getTitle());
                            } else {
                                Log.d("HomepageFragment", "Skipping expired event: " + event.getTitle());
                            }
                        }
                    }

                    // Sort events by date
                    Collections.sort(upcomingEvents, (e1, e2) -> e1.getDate().compareTo(e2.getDate()));
                    Log.d("HomepageFragment", "Sorted " + upcomingEvents.size() + " upcoming events");

                    // Show the next 2 upcoming events
                    for (int i = 0; i < Math.min(2, upcomingEvents.size()); i++) {
                        addEventView(upcomingEvents.get(i));
                    }

                    if (upcomingEvents.isEmpty()) {
                        Log.d("HomepageFragment", "No upcoming events found");
                    }

                    binding.loadingView.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e("HomepageFragment", "Error loading events: " + e.getMessage());
                    binding.loadingView.setVisibility(View.GONE);
                });
    }
}