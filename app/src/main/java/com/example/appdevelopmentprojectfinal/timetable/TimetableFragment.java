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
            Color.parseColor("#E1BEE7"),  // Light Purple
            Color.parseColor("#F8BBD0"), // Light Pink
            Color.parseColor("#D7CCC8"), // Light Brown
            Color.parseColor("#CFD8DC"), // Light Gray
            Color.parseColor("#B2EBF2"), // Light Cyan
            Color.parseColor("#B3E5FC")  // Lighter Blue
    };

    public TimetableFragment() {
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

            confirmButton.setVisibility(View.GONE);

            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setView(dialogView)
                    .create();

            dialog.show();

            Button btnAddAnotherSlot = dialogView.findViewById(R.id.btn_add_slot);
            btnAddAnotherSlot.setOnClickListener(innerView -> {
                if (linearLayoutSlots.findViewWithTag("slotContainer") != null) {
                    Toast.makeText(requireContext(), "Only one slot can be added at a time", Toast.LENGTH_SHORT).show();
                    return;
                }
                confirmButton.setVisibility(View.VISIBLE);
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
                String code = codeInput.getText().toString().trim();
                String name = nameInput.getText().toString().trim();
                String lecturer = lecturerInput.getText().toString().trim();
                String type = typeSpinner.getSelectedItem().toString();

                if (code.isEmpty()) {
                    codeInput.setError("Module code is required");
                    return;
                }

                if (name.isEmpty()) {
                    nameInput.setError("Module name is required");
                    return;
                }

                if (lecturer.isEmpty()) {
                    lecturerInput.setError("Lecturer name is required");
                    return;
                }

                // Check if at least one time slot is added
                if (linearLayoutSlots.getChildCount() == 0) {
                    Toast.makeText(requireContext(), "Add at least one time slot", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validate code format (e.g., CS1234)
                if (!code.matches("^[A-Za-z]{2}[0-9]{4}$")) {
                    codeInput.setError("Invalid code format. Use 2 letters and 4 numbers. E.g., use CS1234 or ET4011.");
                    return;
                }

                if (!name.matches("^[A-Za-z\\ ]{1,50}$")) {
                    nameInput.setError("Invalid name format. use only (A-Z,a-z) and make sure it's less than 50 letters.");
                    return;
                }

                if (!lecturer.matches("^[A-Za-z\\-. ]{1,30}$")) {
                    lecturerInput.setError("Invalid name format. use only (A-Z,a-z,-,.) and make sure it's less than 30 letters.");
                    return;
                }

                if (!validateTimeSlots()) {
                    return;
                }


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
                .document()
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
                                        boolean isMovable = true;
                                        ModuleSchedule schedule = new ModuleSchedule(
                                                module,
                                                timeSlot,
                                                isMovable,
                                                module.isShow());
//1
                                        moduleSchedules.add(schedule);
                                    }
                                }
                            }
                        }




                        moduleAdapter = new ModuleManagementAdapter(moduleSchedules, TimetableFragment.this);
                        moduleListView.setAdapter(moduleAdapter);

                        displayTimetable();
                    } else {
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

                if (checkedId != -1) {
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
                    ModuleSchedule schedule = schedulesForSlot.get(0);
                    Module module = schedule.getModule();

                    View moduleView = inflater.inflate(R.layout.item_timetable_module, null);

                    TextView codeText = moduleView.findViewById(R.id.module_code);
                    TextView nameText = moduleView.findViewById(R.id.module_name);
                    TextView locationText = moduleView.findViewById(R.id.module_location);

                    if (module.getAlternativeSlots() != null && !module.getAlternativeSlots().isEmpty()) {
                        codeText.setText(module.getCode() + " *");
                        codeText.setTextColor(Color.BLUE);
                    } else {
                        codeText.setText(module.getCode());
                    }

                    codeText.setText(module.getCode());
                    nameText.setText(module.getName());
                    locationText.setText(schedule.getTimeSlot().getLocation());

                    CardView cardView = (CardView) moduleView;
                    int colorIndex = (Integer.parseInt(module.getCode().replaceAll("[^0-9]", "")) % 100) % MODULE_COLORS.length;
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

        Button viewAlternativesButton = bottomSheetView.findViewById(R.id.btn_view_alternatives);
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

        if (module.getAlternativeSlots() != null && !module.getAlternativeSlots().isEmpty()) {
            viewAlternativesButton.setVisibility(View.VISIBLE);
            viewAlternativesButton.setOnClickListener(v -> {
                showAlternativeSlots(module, schedule.getTimeSlot(), bottomSheetDialog);
            });
        }

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

    private void showAlternativeSlots(Module module, TimeSlot currentSlot, BottomSheetDialog parentDialog) {
        parentDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Alternative Time Slots");

        List<TimeSlot> alternatives = module.getAlternativeSlots();
        CharSequence[] items = new CharSequence[alternatives.size()];
        for (int i = 0; i < alternatives.size(); i++) {
            TimeSlot alt = alternatives.get(i);
            items[i] = alt.getDay() + " " + alt.getStartTime() + "-" + alt.getEndTime() + " (" + alt.getLocation() + ")";
        }

        builder.setItems(items, (dialog, which) -> {
            TimeSlot selectedAlt = alternatives.get(which);
            new AlertDialog.Builder(requireContext())
                    .setTitle("Switch Time Slot")
                    .setMessage("Switch from " + currentSlot.getDay() + " " + currentSlot.getStartTime() +
                            " to " + selectedAlt.getDay() + " " + selectedAlt.getStartTime() + "?")
                    .setPositiveButton("Switch", (confirmDialog, confirmWhich) -> {
                        switchTimeSlot(module, currentSlot, selectedAlt);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void switchTimeSlot(Module module, TimeSlot currentSlot, TimeSlot newSlot) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection("modules")
                .document(module.getDocumentId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Module updatedModule = documentSnapshot.toObject(Module.class);
                    if (updatedModule != null) {
                        List<TimeSlot> timeSlots = updatedModule.getTimeSlotList();
                        boolean slotFound = false;

                        for (int i = 0; i < timeSlots.size(); i++) {
                            TimeSlot slot = timeSlots.get(i);
                            if (slot.getDay().equals(currentSlot.getDay()) &&
                                    slot.getStartTime().equals(currentSlot.getStartTime()) &&
                                    slot.getEndTime().equals(currentSlot.getEndTime())) {
                                timeSlots.set(i, newSlot);
                                slotFound = true;
                                break;
                            }
                        }

                        if (slotFound) {
                            updatedModule.setTimeSlotList(timeSlots);

                            List<TimeSlot> alternativeSlots = updatedModule.getAlternativeSlots();
                            if (alternativeSlots == null) {
                                alternativeSlots = new ArrayList<>();
                            }

                            boolean oldSlotExists = false;
                            for (TimeSlot alt : alternativeSlots) {
                                if (alt.getDay().equals(currentSlot.getDay()) &&
                                        alt.getStartTime().equals(currentSlot.getStartTime()) &&
                                        alt.getEndTime().equals(currentSlot.getEndTime())) {
                                    oldSlotExists = true;
                                    break;
                                }
                            }

                            if (!oldSlotExists) {
                                alternativeSlots.add(currentSlot);
                            }

                            for (int i = 0; i < alternativeSlots.size(); i++) {
                                TimeSlot alt = alternativeSlots.get(i);
                                if (alt.getDay().equals(newSlot.getDay()) &&
                                        alt.getStartTime().equals(newSlot.getStartTime()) &&
                                        alt.getEndTime().equals(newSlot.getEndTime())) {
                                    alternativeSlots.remove(i);
                                    break;
                                }
                            }

                            updatedModule.setAlternativeSlots(alternativeSlots);

                            database.collection("modules")
                                    .document(module.getDocumentId())
                                    .set(updatedModule)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(requireContext(), "Time slot has been switched successfully", Toast.LENGTH_SHORT).show();
                                        LoadModuleDetails();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(requireContext(), "Could not switch time slot: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error loading module: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
    private boolean validateTimeSlots() {
        boolean isValid = true;

        for (int i = 0; i < linearLayoutSlots.getChildCount(); i++) {
            View childView = linearLayoutSlots.getChildAt(i);

            if (childView instanceof LinearLayout && "slotContainer".equals(childView.getTag())) {
                LinearLayout slotContainer = (LinearLayout) childView;
                LinearLayout slotLayout = slotContainer.findViewWithTag("mainSlotLayout");

                if (slotLayout != null) {
                    EditText locationField = null;
                    for (int j = 0; j < slotLayout.getChildCount(); j++) {
                        View view = slotLayout.getChildAt(j);
                        if (view instanceof EditText && "Location".equals(((EditText) view).getHint())) {
                            locationField = (EditText) view;
                            break;
                      }
                    }
                    if (locationField != null && locationField.getText().toString().trim().isEmpty()) {
                        locationField.setError("Location is required");
                        isValid = false;
                    }

                    Spinner startTimeSpinner = slotLayout.findViewWithTag("startTimeSpinner");
                    Spinner endTimeSpinner = slotLayout.findViewWithTag("endTimeSpinner");

                    if (startTimeSpinner != null && endTimeSpinner != null) {
                        String startTime = startTimeSpinner.getSelectedItem().toString();
                        String endTime = endTimeSpinner.getSelectedItem().toString();

                        if (startTime.compareTo(endTime) >= 0) {
                            Toast.makeText(requireContext(),
                                    "End time must be after start time",
                                    Toast.LENGTH_SHORT).show();
                            isValid = false;
                        }
                    }
                }
            }
        }

        return isValid;
    }
}