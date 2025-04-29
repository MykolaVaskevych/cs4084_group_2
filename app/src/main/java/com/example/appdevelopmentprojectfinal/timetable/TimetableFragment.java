package com.example.appdevelopmentprojectfinal.timetable;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdevelopmentprojectfinal.R;
import com.example.appdevelopmentprojectfinal.utils.JsonUtil;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;

public class TimetableFragment extends Fragment implements ModuleManagementAdapter.OnModuleVisibilityChangedListener {

    private final List<ModuleSchedule> moduleSchedules = new ArrayList<>();
    private TableLayout timetableGrid;
    private TextView emptyView;
    private RecyclerView moduleListView;
    private ModuleManagementAdapter moduleAdapter;

    private Button addModuleButton;
    LinearLayout linearLayoutSlots;

    private static final String[] TIME_SLOTS = {
            "09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00",
            "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00"
    };

    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

    private static final int[] MODULE_COLORS = {
            Color.parseColor("#FFCDD2"), // Light Red
            Color.parseColor("#C8E6C9"), // Light Green
            Color.parseColor("#BBDEFB"), // Light Blue
            Color.parseColor("#FFE0B2"), // Light Orange
            Color.parseColor("#E1BEE7")  // Light Purple
    };

    public TimetableFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_timetable, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        timetableGrid = view.findViewById(R.id.timetable_grid);
        emptyView = view.findViewById(R.id.empty_view);
        moduleListView = view.findViewById(R.id.module_list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext()) {
            @Override
            public boolean canScrollVertically() {
                return true;
            }
        };

        moduleListView.setLayoutManager(layoutManager);
        moduleListView.setHasFixedSize(true);
        moduleListView.setNestedScrollingEnabled(true);

        addModuleButton = view.findViewById(R.id.add_module_button);
        addModuleButton.setOnClickListener(v -> {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View dialogView = inflater.inflate(R.layout.dialog_add_module, null);

            linearLayoutSlots = dialogView.findViewById(R.id.linear_layout_slots);

            EditText codeInput = dialogView.findViewById(R.id.input_code);
            EditText nameInput = dialogView.findViewById(R.id.input_name);
            EditText lecturerInput = dialogView.findViewById(R.id.input_lecturer);
            EditText typeInput = dialogView.findViewById(R.id.input_type);
            typeInput.setVisibility(View.GONE);
            Button confirmButton = dialogView.findViewById(R.id.btn_add_module);
            Spinner typeSpinner = dialogView.findViewById(R.id.type_spinner);

            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setView(dialogView)
                    .create();

            dialog.show();

            Button btnAddAnotherSlot = dialogView.findViewById(R.id.btn_add_slot);
            btnAddAnotherSlot.setOnClickListener(innerView -> {

                LinearLayout newSlotLayout = new LinearLayout(requireContext());
                newSlotLayout.setOrientation(LinearLayout.VERTICAL);
                newSlotLayout.setTag("mainSlotLayout");

                LinearLayout slotContainer = new LinearLayout(requireContext());
                slotContainer.setOrientation(LinearLayout.VERTICAL);
                slotContainer.setTag("slotContainer"); // Add tag to identify slot containers

                Spinner daySpinner = new Spinner(requireContext());
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                daySpinner.setLayoutParams(layoutParams);

                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                        requireContext(),
                        R.array.days_of_week,
                        android.R.layout.simple_spinner_item
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                daySpinner.setAdapter(adapter);

                newSlotLayout.addView(daySpinner);

                Spinner startTimeSpinner = new Spinner(requireContext());
                LinearLayout.LayoutParams startTimeLlayoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                startTimeSpinner.setLayoutParams(startTimeLlayoutParams);

                ArrayAdapter<CharSequence> startTimeAdapter = ArrayAdapter.createFromResource(
                        requireContext(),
                        R.array.start_time_slots,
                        android.R.layout.simple_spinner_item
                );
                startTimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                startTimeSpinner.setAdapter(startTimeAdapter);
                startTimeSpinner.setTag("startTimeSpinner");

                newSlotLayout.addView(startTimeSpinner);



                Spinner endTimeSpinner = new Spinner(requireContext());
                LinearLayout.LayoutParams endTimeLlayoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                endTimeSpinner.setLayoutParams(endTimeLlayoutParams);

                ArrayAdapter<CharSequence> endTimeAdapter = ArrayAdapter.createFromResource(
                        requireContext(),
                        R.array.end_time_slots,
                        android.R.layout.simple_spinner_item
                );
                endTimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                endTimeSpinner.setAdapter(endTimeAdapter);
                endTimeSpinner.setTag("endTimeSpinner");

                newSlotLayout.addView(endTimeSpinner);

                EditText inputLocation = new EditText(requireContext());
                inputLocation.setHint("Location");
                newSlotLayout.addView(inputLocation);

                TextView textIsMovable = new TextView(requireContext());
                textIsMovable.setText("Is the Slot Movable?");
                newSlotLayout.addView(textIsMovable);

                RadioGroup radioGroupIsMovable = new RadioGroup(requireContext());
                radioGroupIsMovable.setOrientation(LinearLayout.HORIZONTAL);

                RadioButton radioYes = new RadioButton(requireContext());
                radioYes.setText("Yes");
                radioGroupIsMovable.addView(radioYes);

                RadioButton radioNo = new RadioButton(requireContext());
                radioNo.setText("No");
                radioGroupIsMovable.addView(radioNo);

                newSlotLayout.addView(radioGroupIsMovable);

                LinearLayout alternativeSlotsContainer = new LinearLayout(requireContext());
                alternativeSlotsContainer.setOrientation(LinearLayout.VERTICAL);
                alternativeSlotsContainer.setTag("alternativeSlotsContainer");
                newSlotLayout.addView(alternativeSlotsContainer);

                Button btnAddAlternativeSlot = new Button(requireContext());
                btnAddAlternativeSlot.setText("Add Alternative Time Slot");
                btnAddAlternativeSlot.setVisibility(View.GONE);
                newSlotLayout.addView(btnAddAlternativeSlot);

                radioGroupIsMovable.setOnCheckedChangeListener((group, checkedId) -> {
                    if (checkedId == radioYes.getId()) {
                        btnAddAlternativeSlot.setVisibility(View.VISIBLE);
                    } else {
                        btnAddAlternativeSlot.setVisibility(View.GONE);
                        alternativeSlotsContainer.removeAllViews();
                    }
                });

                btnAddAlternativeSlot.setOnClickListener(view_alternate -> {
                    LinearLayout altSlotLayout = new LinearLayout(requireContext());
                    altSlotLayout.setOrientation(LinearLayout.VERTICAL);
                    altSlotLayout.setTag("alternativeSlot");

                    View divider = new View(requireContext());
                    divider.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                    ));
                    divider.setBackgroundColor(Color.GRAY);
                    altSlotLayout.addView(divider);

                    TextView altTitle = new TextView(requireContext());
                    altTitle.setText("Alternative Time Slot");
                    altSlotLayout.addView(altTitle);



                    Spinner altDaySpinner = new Spinner(requireContext());
                    LinearLayout.LayoutParams altLayoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    altDaySpinner.setLayoutParams(altLayoutParams);

                    ArrayAdapter<CharSequence> altAdapter = ArrayAdapter.createFromResource(
                            requireContext(),
                            R.array.days_of_week,
                            android.R.layout.simple_spinner_item
                    );
                    altAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    altDaySpinner.setAdapter(altAdapter);

                    altSlotLayout.addView(altDaySpinner);


                    Spinner altStartTimeSpinner = new Spinner(requireContext());
                    LinearLayout.LayoutParams altStartTimeLlayoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    altStartTimeSpinner.setLayoutParams(altStartTimeLlayoutParams);

                    ArrayAdapter<CharSequence> altStartTimeAdapter = ArrayAdapter.createFromResource(
                            requireContext(),
                            R.array.start_time_slots,
                            android.R.layout.simple_spinner_item
                    );
                    altStartTimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    altStartTimeSpinner.setAdapter(altStartTimeAdapter);
                    altStartTimeSpinner.setTag("altStartTimeSpinner");

                    altSlotLayout.addView(altStartTimeSpinner);

                    Spinner altEndTimeSpinner = new Spinner(requireContext());
                    LinearLayout.LayoutParams altEndTimeLlayoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    altEndTimeSpinner.setLayoutParams(altEndTimeLlayoutParams);

                    ArrayAdapter<CharSequence> altEndTimeAdapter = ArrayAdapter.createFromResource(
                            requireContext(),
                            R.array.end_time_slots,
                            android.R.layout.simple_spinner_item
                    );
                    altEndTimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    altEndTimeSpinner.setAdapter(altEndTimeAdapter);
                    altEndTimeSpinner.setTag("altEndTimeSpinner");

                    altSlotLayout.addView(altEndTimeSpinner);

                    EditText altInputLocation = new EditText(requireContext());
                    altInputLocation.setHint("Location");
                    altSlotLayout.addView(altInputLocation);

                    Button btnRemoveAltSlot = new Button(requireContext());
                    btnRemoveAltSlot.setText("Remove This Alternative Slot");
                    btnRemoveAltSlot.setOnClickListener(removeView -> {
                        alternativeSlotsContainer.removeView(altSlotLayout);
                    });
                    altSlotLayout.addView(btnRemoveAltSlot);

                    alternativeSlotsContainer.addView(altSlotLayout);
                });

                slotContainer.addView(newSlotLayout);
                linearLayoutSlots.addView(slotContainer);
            });

            confirmButton.setOnClickListener(confirmView -> {
                String code = codeInput.getText().toString();
                String name = nameInput.getText().toString();
                String lecturer = lecturerInput.getText().toString();
                String type = typeInput.getText().toString();
                type = typeSpinner.getSelectedItem().toString();

                List<TimeSlot> timeSlotList = new ArrayList<>();
                List<TimeSlot> alternativeSlotsList = new ArrayList<>();

                int childCount = linearLayoutSlots.getChildCount();

                for (int i = 0; i < childCount; i++) {
                    View childView = linearLayoutSlots.getChildAt(i);

                    if (childView instanceof LinearLayout && "slotContainer".equals(childView.getTag())) {
                        LinearLayout slotContainer = (LinearLayout) childView;

                        LinearLayout slotLayout = slotContainer.findViewWithTag("mainSlotLayout");
                        if (slotLayout != null) {
                            TimeSlot timeSlot = readSlotLayout(slotLayout);
                            timeSlotList.add(timeSlot);

                            LinearLayout alternativeSlotsContainer = slotLayout.findViewWithTag("alternativeSlotsContainer");
                            if (alternativeSlotsContainer != null) {
                                int altChildCount = alternativeSlotsContainer.getChildCount();
                                for (int j = 0; j < altChildCount; j++) {
                                    View altChildView = alternativeSlotsContainer.getChildAt(j);
                                    if (altChildView instanceof LinearLayout && "alternativeSlot".equals(altChildView.getTag())) {
                                        TimeSlot altTimeSlot = readSlotLayout((LinearLayout) altChildView);
                                        alternativeSlotsList.add(altTimeSlot);
                                    }
                                }
                            }
                        }
                    }
                }

                Module module = new Module(code, name, lecturer, true);
                module.setType(type);
                module.setTimeSlotList(timeSlotList);
                module.setAlternativeSlots(alternativeSlotsList);

                try {
                    addModule(module);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                dialog.dismiss();

                SaveModuleDetails(module);
            });
        });



        //loadTimetableData();

        moduleAdapter = new ModuleManagementAdapter(moduleSchedules, this);
        moduleListView.setAdapter(moduleAdapter);

        LoadModuleDetails();
        displayTimetable();
    }

    private void SaveModuleDetails(Module module) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection("modules")
                .document() // creates a new document reference with a random ID
                .set(module)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Timetable data saved", Toast.LENGTH_SHORT).show();
                    LoadModuleDetails();
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(requireContext(), "Failed to save timetable data", Toast.LENGTH_SHORT).show();
                });
    }

    private void LoadModuleDetails() {
        if (isAdded()) {
            moduleSchedules.clear();

            FirebaseFirestore database = FirebaseFirestore.getInstance();
            database.collection("modules").get().addOnSuccessListener(queryDocumentSnapshots -> {
                if (isAdded()) {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Module module = document.toObject(Module.class);
                            if (module != null) {
                                module.setDocumentId(document.getId());
                                List<TimeSlot> timeSlots = module.getTimeSlotList();
                                if (timeSlots != null) {
                                    for (TimeSlot timeSlot : timeSlots) {
                                        // Create a ModuleSchedule for each time slot
                                        boolean isMovable = true; // Set according to your data
                                        ModuleSchedule schedule = new ModuleSchedule(
                                                module,
                                                timeSlot,
                                                isMovable,
                                                module.isShow());

                                        // Add to the master list
                                        moduleSchedules.add(schedule);
                                    }
                                }
                            }
                        }




                        // Recreate adapter with fresh data
                        moduleAdapter = new ModuleManagementAdapter(moduleSchedules, TimetableFragment.this);
                        moduleListView.setAdapter(moduleAdapter);

                        // Refresh the timetable grid
                        displayTimetable();
                    } else {
                        // Handle empty data case
                        emptyView.setVisibility(View.VISIBLE);
                    }
                }
            }).addOnFailureListener(e -> {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load timetable data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e("TimetableFragment", "Error loading modules", e);
                }
            });
        }
    }



    private TimeSlot readSlotLayout(LinearLayout slotLayout) {
        String day = "";
        String startTime = "";
        String endTime = "";
        String location = "";
        boolean isMovable = false;

        for (int j = 0; j < slotLayout.getChildCount(); j++) {
            View inputView = slotLayout.getChildAt(j);

            if (inputView instanceof EditText) {
                EditText editText = (EditText) inputView;
                String hint = editText.getHint() != null ? editText.getHint().toString() : "";
                String text = editText.getText().toString();

                if (hint.contains("Day")) {
                    day = text;
                } else if (hint.contains("Start Time")) {
                    startTime = text;
                } else if (hint.contains("End Time")) {
                    endTime = text;
                } else if (hint.contains("Location")) {
                    location = text;
                }
            } else if (inputView instanceof RadioGroup) {
                RadioGroup radioGroup = (RadioGroup) inputView;
                int checkedId = radioGroup.getCheckedRadioButtonId();

                if (checkedId != -1) { // Check if a button is selected
                    RadioButton selectedRadioButton = radioGroup.findViewById(checkedId);
                    if (selectedRadioButton != null) {
                        isMovable = selectedRadioButton.getText().toString().equalsIgnoreCase("Yes");
                    }
                }
            } else if (inputView instanceof Spinner) {
                Spinner spinner = (Spinner) inputView;

                if (inputView.getTag() != null && (inputView.getTag().equals("startTimeSpinner") || inputView.getTag().equals("altStartTimeSpinner"))) {
                    startTime = spinner.getSelectedItem().toString();
                } else if (inputView.getTag() != null && (inputView.getTag().equals("endTimeSpinner") || inputView.getTag().equals("altEndTimeSpinner"))) {
                    endTime = spinner.getSelectedItem().toString();
                } else {
                    day = spinner.getSelectedItem().toString();
                }

                Log.d("TimetableFragment", "Day: " + day);
            }
        }

        Log.d("TimetableFragment", "Slot: Day=" + day + ", Start=" + startTime + ", End=" + endTime + ", Loc=" + location + ", Movable=" + isMovable);
        return new TimeSlot(day, startTime, endTime, location);
    }



    private void addModule(Module module) throws JSONException {
        Log.d("ModuleEntry", module.toString());
        JsonUtil jsonUtil = new JsonUtil();
        jsonUtil.appendModuleToFile(requireContext(), new JSONObject(module.toString()));
    }

    @Override
    public void onModuleVisibilityChanged() {
        timetableGrid.removeAllViews();
        displayTimetable();
    }

    private void displayTimetable() {
        timetableGrid.removeAllViews();

        if (moduleSchedules.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        emptyView.setVisibility(View.GONE);

        Map<String, Map<String, List<ModuleSchedule>>> timetableMap = new HashMap<>();

        for (String timeSlot : TIME_SLOTS) {
            timetableMap.put(timeSlot, new HashMap<>());
            for (String day : DAYS) {
                Map<String, List<ModuleSchedule>> dayMap = timetableMap.get(timeSlot);
                if (dayMap != null) {
                    dayMap.put(day, new ArrayList<>());
                }
            }
        }

        for (ModuleSchedule schedule : moduleSchedules) {
            TimeSlot slot = schedule.getTimeSlot();
            String day = slot.getDay();
            String startTime = slot.getStartTime();
            String endTime = slot.getEndTime();

            for (String timeSlot : TIME_SLOTS) {
                String[] times = timeSlot.split("-");
                String slotStart = times[0];

                if (isTimeInRange(slotStart, startTime, endTime)) {
                    Map<String, List<ModuleSchedule>> dayMap = timetableMap.get(timeSlot);
                    if (dayMap != null) {
                        List<ModuleSchedule> schedules = dayMap.get(day);
                        if (schedules != null) {
                            schedules.add(schedule);
                        }
                    }
                }
            }
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (String timeSlot : TIME_SLOTS) {
            TableRow row = new TableRow(getContext());

            TextView timeLabel = new TextView(getContext());
            timeLabel.setText(timeSlot);
            timeLabel.setPadding(8, 8, 8, 8);
            timeLabel.setWidth(250);
            row.addView(timeLabel);

            for (String day : DAYS) {
                List<ModuleSchedule> schedulesForSlot = new ArrayList<>();
                Map<String, List<ModuleSchedule>> dayMap = timetableMap.get(timeSlot);
                if (dayMap != null) {
                    List<ModuleSchedule> slots = dayMap.get(day);
                    if (slots != null) {
                        for (ModuleSchedule slot : slots) {
                            if (slot.isVisible()) {
                                schedulesForSlot.add(slot);
                            }
                        }
                    }
                }

                if (schedulesForSlot.isEmpty()) {
                    View emptyCell = new View(getContext());
                    TableRow.LayoutParams params = new TableRow.LayoutParams(300, 150);
                    params.setMargins(2, 2, 2, 2);
                    emptyCell.setLayoutParams(params);
                    emptyCell.setBackgroundColor(Color.LTGRAY);
                    row.addView(emptyCell);
                } else {
                    // TODO: Handle multiple modules in the same time slot properly
                    ModuleSchedule schedule = schedulesForSlot.get(0); // Just take the first one if multiple
                    Module module = schedule.getModule();

                    View moduleView = inflater.inflate(R.layout.item_timetable_module, null);

                    TextView codeText = moduleView.findViewById(R.id.module_code);
                    TextView nameText = moduleView.findViewById(R.id.module_name);
                    TextView locationText = moduleView.findViewById(R.id.module_location);

                    codeText.setText(module.getCode());
                    nameText.setText(module.getName());
                    locationText.setText(schedule.getTimeSlot().getLocation());

                    CardView cardView = (CardView) moduleView;
                    int colorIndex = Math.abs(module.getCode().hashCode()) % MODULE_COLORS.length;
                    cardView.setCardBackgroundColor(MODULE_COLORS[colorIndex]);

                    TableRow.LayoutParams params = new TableRow.LayoutParams(120, 150);
                    params.setMargins(2, 2, 2, 2);
                    moduleView.setLayoutParams(params);

                    moduleView.setOnClickListener(v -> handleModuleClick(schedule));

                    row.addView(moduleView);
                }
            }

            timetableGrid.addView(row);
        }
    }

    private boolean isTimeInRange(String timeToCheck, String startTime, String endTime) {
        return timeToCheck.compareTo(startTime) >= 0 && timeToCheck.compareTo(endTime) < 0;
    }

    private void handleModuleClick(ModuleSchedule schedule) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());

        View bottomSheetView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_module_code, null);

        TextView moduleCode = bottomSheetView.findViewById(R.id.module_code);
        TextView moduleName = bottomSheetView.findViewById(R.id.module_name);
        TextView moduleLecturer = bottomSheetView.findViewById(R.id.module_lecturer);
        TextView moduleLocation = bottomSheetView.findViewById(R.id.module_location);
        TextView moduleDay = bottomSheetView.findViewById(R.id.module_day);
        TextView moduleStartTime = bottomSheetView.findViewById(R.id.module_start_time);
        TextView moduleEndTime = bottomSheetView.findViewById(R.id.module_end_time);
        TextView moduleType = bottomSheetView.findViewById(R.id.module_type);

        Button deleteModuleButton = bottomSheetView.findViewById(R.id.delete_module_button);

        View hideShowButton = bottomSheetView.findViewById(R.id.hide_show_button);
        hideShowButton.setVisibility(View.GONE);

        Module module = schedule.getModule();
        TimeSlot timeSlot = schedule.getTimeSlot();
        moduleCode.setText(module.getCode());
        moduleName.setText(module.getName());
        moduleLecturer.setText(module.getLecturer());
        moduleLocation.setText(timeSlot.getLocation());
        moduleDay.setText(timeSlot.getDay());
        moduleStartTime.setText(timeSlot.getStartTime());
        moduleEndTime.setText(timeSlot.getEndTime());
        moduleType.setText(module.getType());

        deleteModuleButton.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Module")
                    .setMessage("Are you sure you want to delete " + module.getCode() + ": " + module.getName() + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        deleteModule(module);
                        bottomSheetDialog.dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void deleteModule(Module module) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        database.collection("modules")
                .whereEqualTo("code", module.getCode())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String documentId = null;

                        if (module.getDocumentId() != null && !module.getDocumentId().isEmpty()) {
                            documentId = module.getDocumentId();
                        } else {
                            DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                            documentId = document.getId();
                        }

                        database.collection("modules")
                                .document(documentId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(requireContext(),
                                            module.getCode() + " has been deleted",
                                            Toast.LENGTH_SHORT).show();

                                    LoadModuleDetails();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(requireContext(),
                                            "Could not delete the module: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    Log.e("TimetableFragment", "Error deleting of module", e);
                                });
                    } else {
                        Toast.makeText(requireContext(),
                                "Couldn't not found in database",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(),
                            "Error querying modules: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e("TimetableFragment", "Error querying the modules for deletion", e);
                });
    }
}