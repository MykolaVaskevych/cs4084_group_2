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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;

public class TimetableFragment extends Fragment implements ModuleManagementAdapter.OnModuleVisibilityChangedListener {

    private final List<ModuleSchedule> moduleSchedules = new ArrayList<>();
    private TableLayout timetableGrid;
    private TextView emptyView;
    private RecyclerView moduleListView;
    private ModuleManagementAdapter moduleAdapter;

    private Button addModuleButton;
    LinearLayout linearLayoutSlots;

    private String currentUserId;
    private boolean isUserLoggedIn = false;

    private static final String[] TIME_SLOTS = {
            "09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00",
            "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00"
    };

    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

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

        initializeCurrentUser();

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
            if (!isUserLoggedIn) {
                Toast.makeText(requireContext(), "Please log in to add modules to your timetable", Toast.LENGTH_SHORT).show();
                return;
            }

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
                slotContainer.setTag("slotContainer");

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
                    TextView locationLabel = new TextView(requireContext());
                    locationLabel.setTextColor(Color.parseColor("#FF0000"));
                    altSlotLayout.addView(locationLabel);

                    EditText altInputLocation = new EditText(requireContext());
                    altInputLocation.setHint("Location");

                    altInputLocation.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            if (s.toString().trim().isEmpty()) {
                                altInputLocation.setError("Location is required for alternative slot");
                            } else {
                                boolean isDuplicate = false;
                                LinearLayout mainSlotLayout = linearLayoutSlots.findViewWithTag("mainSlotLayout");
                                if (mainSlotLayout != null) {
                                    for (int i = 0; i < mainSlotLayout.getChildCount(); i++) {
                                        View view = mainSlotLayout.getChildAt(i);
                                        if (view instanceof EditText && "Location".equals(((EditText) view).getHint())) {
                                            String mainLocation = ((EditText) view).getText().toString().trim();
                                            if (!mainLocation.isEmpty() && mainLocation.equals(s.toString().trim())) {
//                                                altInputLocation.setError("This location is already used in another place slot");
                                                isDuplicate = true;
                                                break;
                                            }
                                        }
                                    }
                                }

                                if (!isDuplicate) {
                                    LinearLayout alternativeSlotsContainer = (LinearLayout) altSlotLayout.getParent();
                                    if (alternativeSlotsContainer != null) {
                                        for (int i = 0; i < alternativeSlotsContainer.getChildCount(); i++) {
                                            View childView = alternativeSlotsContainer.getChildAt(i);
                                            if (childView == altSlotLayout) continue;

                                            if (childView instanceof LinearLayout && "alternativeSlot".equals(childView.getTag())) {
                                                LinearLayout otherAltSlot = (LinearLayout) childView;

                                                for (int j = 0; j < otherAltSlot.getChildCount(); j++) {
                                                    View otherView = otherAltSlot.getChildAt(j);
                                                    if (otherView instanceof EditText && "Location".equals(((EditText) otherView).getHint())) {
                                                        String otherLocation = ((EditText) otherView).getText().toString().trim();
                                                        if (!otherLocation.isEmpty() && otherLocation.equals(s.toString().trim())) {
                                                            altInputLocation.setError("This location is already used in another alternative slot");
                                                            isDuplicate = true;
                                                            break;
                                                        }
                                                    }
                                                }

                                                if (isDuplicate) break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });

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

                if (linearLayoutSlots.getChildCount() == 0) {
                    Toast.makeText(requireContext(), "Add at least one time slot", Toast.LENGTH_SHORT).show();
                    return;
                }

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
                boolean hasConflicts = false;
                StringBuilder conflictMessages = new StringBuilder();

                int childCount = linearLayoutSlots.getChildCount();

                for (int i = 0; i < childCount; i++) {
                    View childView = linearLayoutSlots.getChildAt(i);

                    if (childView instanceof LinearLayout && "slotContainer".equals(childView.getTag())) {
                        LinearLayout slotContainer = (LinearLayout) childView;

                        LinearLayout slotLayout = slotContainer.findViewWithTag("mainSlotLayout");
                        if (slotLayout != null) {
                            TimeSlot timeSlot = readSlotLayout(slotLayout);

                            String conflicts = checkTimeConflicts(timeSlot.getDay(), timeSlot.getStartTime(), timeSlot.getEndTime());
                            if (conflicts != null) {
                                hasConflicts = true;
                                conflictMessages.append("Main (original) slot (")
                                        .append(timeSlot.getDay())
                                        .append(" ")
                                        .append(timeSlot.getStartTime())
                                        .append("-")
                                        .append(timeSlot.getEndTime())
                                        .append(") conflicts with:\n")
                                        .append(conflicts)
                                        .append("\n");
                            }

                            timeSlotList.add(timeSlot);

                            LinearLayout alternativeSlotsContainer = slotLayout.findViewWithTag("alternativeSlotsContainer");
                            if (alternativeSlotsContainer != null) {
                                int altChildCount = alternativeSlotsContainer.getChildCount();
                                for (int j = 0; j < altChildCount; j++) {
                                    View altChildView = alternativeSlotsContainer.getChildAt(j);
                                    if (altChildView instanceof LinearLayout && "alternativeSlot".equals(altChildView.getTag())) {
                                        TimeSlot altTimeSlot = readSlotLayout((LinearLayout) altChildView);

                                        conflicts = checkTimeConflicts(altTimeSlot.getDay(), altTimeSlot.getStartTime(), altTimeSlot.getEndTime());
                                        if (conflicts != null) {
                                            hasConflicts = true;
                                            conflictMessages.append("Alternative slot (")
                                                    .append(altTimeSlot.getDay())
                                                    .append(" ")
                                                    .append(altTimeSlot.getStartTime())
                                                    .append("-")
                                                    .append(altTimeSlot.getEndTime())
                                                    .append(") conflicts with:\n")
                                                    .append(conflicts)
                                                    .append("\n");
                                        }

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
                module.setUserId(currentUserId);

                if (hasConflicts) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Time Slot Conflict")
                            .setMessage("This module overlaps with existing modules:\n\n" +
                                    conflictMessages.toString() +
                                    "\nAre you sure you want to add it?")
                            .setPositiveButton("Add Anyway", (dialogInterface, which) -> {
                                try {
                                    addModule(module);
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                                dialog.dismiss();
                                SaveModuleDetails(module);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    try {
                        addModule(module);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    dialog.dismiss();
                    SaveModuleDetails(module);
                }
            });
        });

        moduleAdapter = new ModuleManagementAdapter(moduleSchedules, this);
        moduleListView.setAdapter(moduleAdapter);

        LoadModuleDetails();
        displayTimetable();
    }

    private void initializeCurrentUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getEmail();
            isUserLoggedIn = true;
            Log.d("TimetableFragment", "User logged in: " + currentUserId);
        } else {
            currentUserId = "anonymous";
            isUserLoggedIn = false;
            Log.d("TimetableFragment", "No user logged in, using default ID");
            Toast.makeText(requireContext(), "Log in to see your personalized timetable", Toast.LENGTH_SHORT).show();
        }
    }

    private void SaveModuleDetails(Module module) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        Map<String, Object> moduleData = new HashMap<>();
        moduleData.put("code", module.getCode());
        moduleData.put("name", module.getName());
        moduleData.put("lecturer", module.getLecturer());
        moduleData.put("type", module.getType());
        moduleData.put("show", module.isShow());
        moduleData.put("timeSlotList", module.getTimeSlotList());
        moduleData.put("alternativeSlots", module.getAlternativeSlots());
        moduleData.put("userId", currentUserId);

        database.collection("modules")
                .document()
                .set(moduleData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Timetable data saved", Toast.LENGTH_SHORT).show();
                    updateUserModulesList(module.getCode());
                    LoadModuleDetails();
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(requireContext(), "Failed to save timetable data", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUserModulesList(String moduleCode) {
        if (!isUserLoggedIn) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> modules = (List<String>) documentSnapshot.get("modules");
                        if (modules == null) {
                            modules = new ArrayList<>();
                        }

                        if (!modules.contains(moduleCode)) {
                            modules.add(moduleCode);

                            db.collection("users").document(currentUserId)
                                    .update("modules", modules)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("TimetableFragment", "User modules list updated");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("TimetableFragment", "Error updating user modules list", e);
                                    });
                        }
                    }
                });
    }

    private void LoadModuleDetails() {
        if (isAdded()) {
            moduleSchedules.clear();

            FirebaseFirestore database = FirebaseFirestore.getInstance();
            database.collection("modules")
                    .whereEqualTo("userId", currentUserId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
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
                .whereEqualTo("userId", currentUserId)
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

                                    removeFromUserModulesList(module.getCode());
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
                                "Module not found in database",
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


    private void removeFromUserModulesList(String moduleCode) {
        if (!isUserLoggedIn) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> modules = (List<String>) documentSnapshot.get("modules");
                        if (modules != null && modules.contains(moduleCode)) {
                            modules.remove(moduleCode);

                            db.collection("users").document(currentUserId)
                                    .update("modules", modules)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("TimetableFragment", "Module removed from user modules list");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("TimetableFragment", "Error updating user modules list", e);
                                    });
                        }
                    }
                });
    }

    private boolean validateTimeSlots() {
        boolean isValid = true;

        List<SlotInfo> allSlots = new ArrayList<>();

        for (int i = 0; i < linearLayoutSlots.getChildCount(); i++) {
            View childView = linearLayoutSlots.getChildAt(i);

            if (childView instanceof LinearLayout && "slotContainer".equals(childView.getTag())) {
                LinearLayout slotContainer = (LinearLayout) childView;
                LinearLayout slotLayout = slotContainer.findViewWithTag("mainSlotLayout");

                if (slotLayout != null) {
                    EditText locationField = null;
                    String day = "";
                    String startTime = "";
                    String endTime = "";

                    for (int j = 0; j < slotLayout.getChildCount(); j++) {
                        View view = slotLayout.getChildAt(j);
                        if (view instanceof EditText && "Location".equals(((EditText) view).getHint())) {
                            locationField = (EditText) view;
                        } else if (view instanceof Spinner) {
                            if (view.getTag() == null) {
                                day = ((Spinner) view).getSelectedItem().toString();
                            } else if ("startTimeSpinner".equals(view.getTag())) {
                                startTime = ((Spinner) view).getSelectedItem().toString();
                            } else if ("endTimeSpinner".equals(view.getTag())) {
                                endTime = ((Spinner) view).getSelectedItem().toString();
                            }
                        }
                    }

                    if (locationField != null) {
                        String location = locationField.getText().toString().trim();
                        if (location.isEmpty()) {
                            locationField.setError("Location is required");
                            isValid = false;
                        } else {
                            allSlots.add(new SlotInfo(day, startTime, endTime, location, locationField));
                        }
                    }

                    if (!day.isEmpty() && !startTime.isEmpty() && !endTime.isEmpty()) {
                        if (startTime.compareTo(endTime) >= 0) {
                            Toast.makeText(requireContext(),
                                    "End time must be after start time",
                                    Toast.LENGTH_SHORT).show();
                            isValid = false;
                        }
                    }

                    LinearLayout alternativeSlotsContainer = slotLayout.findViewWithTag("alternativeSlotsContainer");
                    if (alternativeSlotsContainer != null && alternativeSlotsContainer.getChildCount() > 0) {
                        for (int j = 0; j < alternativeSlotsContainer.getChildCount(); j++) {
                            View altView = alternativeSlotsContainer.getChildAt(j);
                            if (altView instanceof LinearLayout && "alternativeSlot".equals(altView.getTag())) {
                                LinearLayout altSlotLayout = (LinearLayout) altView;

                                EditText altLocationField = null;
                                String altDay = "";
                                String altStartTime = "";
                                String altEndTime = "";

                                for (int k = 0; k < altSlotLayout.getChildCount(); k++) {
                                    View altInputView = altSlotLayout.getChildAt(k);
                                    if (altInputView instanceof EditText && "Location".equals(((EditText) altInputView).getHint())) {
                                        altLocationField = (EditText) altInputView;
                                    } else if (altInputView instanceof Spinner) {
                                        if (altInputView.getTag() == null) {
                                            altDay = ((Spinner) altInputView).getSelectedItem().toString();
                                        } else if ("altStartTimeSpinner".equals(altInputView.getTag())) {
                                            altStartTime = ((Spinner) altInputView).getSelectedItem().toString();
                                        } else if ("altEndTimeSpinner".equals(altInputView.getTag())) {
                                            altEndTime = ((Spinner) altInputView).getSelectedItem().toString();
                                        }
                                    }
                                }

                                if (altLocationField != null) {
                                    String altLocation = altLocationField.getText().toString().trim();
                                    if (altLocation.isEmpty()) {
                                        altLocationField.setError("Location required for alternative slot");
                                        isValid = false;
                                    } else {
                                        allSlots.add(new SlotInfo(altDay, altStartTime, altEndTime, altLocation, altLocationField));
                                    }
                                } else {
                                    Toast.makeText(requireContext(),
                                            "Alternative slot is missing location field",
                                            Toast.LENGTH_SHORT).show();
                                    isValid = false;
                                }

                                if (!altDay.isEmpty() && !altStartTime.isEmpty() && !altEndTime.isEmpty()) {
                                    if (altStartTime.compareTo(altEndTime) >= 0) {
                                        Toast.makeText(requireContext(),
                                                "Alternative slot end time have to be after start time",
                                                Toast.LENGTH_SHORT).show();
                                        isValid = false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        for (int i = 0; i < allSlots.size(); i++) {
            SlotInfo slot1 = allSlots.get(i);

            for (int j = i + 1; j < allSlots.size(); j++) {
                SlotInfo slot2 = allSlots.get(j);

                if (slot1.day.equals(slot2.day) &&
                        timeOverlap(slot1.startTime, slot1.endTime, slot2.startTime, slot2.endTime)) {

                    if (slot1.location.equals(slot2.location)) {
                        slot1.locationField.setError("Cannot use same location for overlapping time slots");
                        slot2.locationField.setError("Cannot use same location for overlapping time slots");
                        isValid = false;
                    }
                }
            }
        }

        return isValid;
    }

    private static class SlotInfo {
        String day;
        String startTime;
        String endTime;
        String location;
        EditText locationField;

        SlotInfo(String day, String startTime, String endTime, String location, EditText locationField) {
            this.day = day;
            this.startTime = startTime;
            this.endTime = endTime;
            this.location = location;
            this.locationField = locationField;
        }
    }

    private boolean timeOverlap(String start1, String end1, String start2, String end2) {
        if (end1.compareTo(start2) <= 0 || end2.compareTo(start1) <= 0) {
            return false;
        }
        return true;
    }

    private String checkTimeConflicts(String newDay, String newStart, String newEnd) {
        StringBuilder conflicts = new StringBuilder();

        for (ModuleSchedule schedule : moduleSchedules) {
            if (!schedule.isVisible()) {
                continue;
            }

            TimeSlot slot = schedule.getTimeSlot();

            if (slot.getDay().equals(newDay) &&
                    timeOverlap(slot.getStartTime(), slot.getEndTime(), newStart, newEnd)) {

                Module module = schedule.getModule();
                conflicts.append(module.getCode())
                        .append(": ")
                        .append(module.getName())
                        .append(" (")
                        .append(slot.getStartTime())
                        .append("-")
                        .append(slot.getEndTime())
                        .append(" at ")
                        .append(slot.getLocation())
                        .append(")\n");
            }
        }

        return conflicts.length() > 0 ? conflicts.toString() : null;
    }
}