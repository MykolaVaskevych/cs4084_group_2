package com.example.appdevelopmentprojectfinal;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.appdevelopmentprojectfinal.model.Module;
import com.example.appdevelopmentprojectfinal.model.ModuleSchedule;
import com.example.appdevelopmentprojectfinal.model.TimeSlot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimetableFragment extends Fragment {
    private static final String TAG = "TimetableFragment";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    private List<ModuleSchedule> moduleSchedules = new ArrayList<>();
    private TableLayout timetableGrid;
    private TextView emptyView;

    // Define time slots for the timetable
    private final String[] TIME_SLOTS = {
            "09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00",
            "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00"
    };

    // Define days for the timetable
    private final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

    // Colors for different modules (just a simple example)
    private final int[] MODULE_COLORS = {
            Color.parseColor("#FFCDD2"), // Light Red
            Color.parseColor("#C8E6C9"), // Light Green
            Color.parseColor("#BBDEFB"), // Light Blue
            Color.parseColor("#FFE0B2"), // Light Orange
            Color.parseColor("#E1BEE7")  // Light Purple
    };

    public TimetableFragment() {
        // Required empty public constructor
    }

    public static TimetableFragment newInstance(String param1, String param2) {
        TimetableFragment fragment = new TimetableFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_timetable, container, false);

        timetableGrid = view.findViewById(R.id.timetable_grid);
        emptyView = view.findViewById(R.id.empty_view);

        // Load timetable data
        loadTimetableData();

        // Display the timetable
        displayTimetable();

        return view;
    }

    private void displayTimetable() {
        Log.d(TAG, "Displaying timetable with " + moduleSchedules.size() + " schedules");

        if (moduleSchedules.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        emptyView.setVisibility(View.GONE);

        // Create a map to store modules by time and day
        Map<String, Map<String, List<ModuleSchedule>>> timetableMap = new HashMap<>();

        // Initialize the map for all time slots and days
        for (String timeSlot : TIME_SLOTS) {
            timetableMap.put(timeSlot, new HashMap<>());
            for (String day : DAYS) {
                timetableMap.get(timeSlot).put(day, new ArrayList<>());
            }
        }

        // Populate the map with module schedules
        for (ModuleSchedule schedule : moduleSchedules) {
            TimeSlot slot = schedule.getTimeSlot();
            String day = slot.getDay();
            String startTime = slot.getStartTime();
            String endTime = slot.getEndTime();

            // Find all time slots that this module spans
            for (String timeSlot : TIME_SLOTS) {
                String[] times = timeSlot.split("-");
                String slotStart = times[0];

                // If this time slot is within the module's time range
                if (isTimeInRange(slotStart, startTime, endTime)) {
                    timetableMap.get(timeSlot).get(day).add(schedule);
                }
            }
        }

        // Create the timetable rows
        LayoutInflater inflater = getLayoutInflater();

        for (String timeSlot : TIME_SLOTS) {
            TableRow row = new TableRow(getContext());

            // Add time label
            TextView timeLabel = new TextView(getContext());
            timeLabel.setText(timeSlot);
            timeLabel.setPadding(8, 8, 8, 8);
            timeLabel.setWidth(240);
            row.addView(timeLabel);

            // Add cells for each day
            for (String day : DAYS) {
                List<ModuleSchedule> schedulesForSlot = timetableMap.get(timeSlot).get(day);

                if (schedulesForSlot.isEmpty()) {
                    // Empty cell
                    View emptyCell = new View(getContext());
                    TableRow.LayoutParams params = new TableRow.LayoutParams(300, 150);
                    params.setMargins(2, 2, 2, 2);
                    emptyCell.setLayoutParams(params);
                    emptyCell.setBackgroundColor(Color.LTGRAY);
                    row.addView(emptyCell);
                } else {
                    // Module cell
                    ModuleSchedule schedule = schedulesForSlot.get(0); // Just take the first one if multiple
                    Module module = schedule.getModule();

                    // Inflate the module item layout
                    View moduleView = inflater.inflate(R.layout.item_timetable_module, null);

                    // Set module details
                    TextView codeText = moduleView.findViewById(R.id.module_code);
                    TextView nameText = moduleView.findViewById(R.id.module_name);
                    TextView locationText = moduleView.findViewById(R.id.module_location);

                    codeText.setText(module.getCode());
                    nameText.setText(module.getName());
                    locationText.setText(schedule.getTimeSlot().getLocation());

                    // Set a background color based on the module code
                    CardView cardView = (CardView) moduleView;
                    int colorIndex = Math.abs(module.getCode().hashCode()) % MODULE_COLORS.length;
                    cardView.setCardBackgroundColor(MODULE_COLORS[colorIndex]);

                    // Set layout parameters
                    TableRow.LayoutParams params = new TableRow.LayoutParams(300, 150);
                    params.setMargins(2, 2, 2, 2);
                    moduleView.setLayoutParams(params);

                    // Add click listener for rescheduling
                    if (schedule.isMovable()) {
                        moduleView.setOnClickListener(v -> handleModuleClick(schedule));
                    }

                    row.addView(moduleView);
                }
            }

            timetableGrid.addView(row);
        }
    }

    private boolean isTimeInRange(String timeToCheck, String startTime, String endTime) {
        // Simple string comparison for HH:MM format
        // This assumes all times are in 24-hour format
        return timeToCheck.compareTo(startTime) >= 0 && timeToCheck.compareTo(endTime) < 0;
    }

    private void handleModuleClick(ModuleSchedule schedule) {
        // For now, just log. We'll implement rescheduling later
        Log.d(TAG, "Module clicked: " + schedule.getModule().getCode());
        // We'll implement a dialog or other UI for rescheduling in the next step
    }

    private void loadTimetableData() {
        try {
            // Read the JSON file from assets
            String jsonString = loadJSONFromAsset("timetable.json");
            if (jsonString == null) {
                Log.e(TAG, "Could not load JSON file");
                return;
            }

            // Parse the JSON
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray modulesArray = jsonObject.getJSONArray("modules");

            // Clear existing data
            moduleSchedules.clear();

            // Process each module
            for (int i = 0; i < modulesArray.length(); i++) {
                JSONObject moduleObj = modulesArray.getJSONObject(i);

                // Create module
                Module module = new Module(
                        moduleObj.getString("code"),
                        moduleObj.getString("name"),
                        moduleObj.getString("lecturer")
                );

                // Process current slots
                JSONArray slotsArray = moduleObj.getJSONArray("slots");
                for (int j = 0; j < slotsArray.length(); j++) {
                    JSONObject slotObj = slotsArray.getJSONObject(j);

                    TimeSlot timeSlot = new TimeSlot(
                            slotObj.getString("day"),
                            slotObj.getString("startTime"),
                            slotObj.getString("endTime"),
                            slotObj.getString("location")
                    );

                    boolean isMovable = slotObj.getBoolean("isMovable");

                    moduleSchedules.add(new ModuleSchedule(module, timeSlot, isMovable));
                }
            }

            Log.d(TAG, "Loaded " + moduleSchedules.size() + " module schedules");

            // Debug logging
            for (ModuleSchedule schedule : moduleSchedules) {
                Log.d(TAG, "Module: " + schedule.getModule().getCode() +
                        " Day: " + schedule.getTimeSlot().getDay() +
                        " Time: " + schedule.getTimeSlot().getStartTime() + "-" +
                        schedule.getTimeSlot().getEndTime());
            }

        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error: " + e.getMessage());
        }
    }

    private String loadJSONFromAsset(String fileName) {
        String json = null;
        try {
            InputStream inputStream = getActivity().getAssets().open(fileName);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            Log.e(TAG, "Error reading JSON file: " + e.getMessage());
            return null;
        }
        return json;
    }
}