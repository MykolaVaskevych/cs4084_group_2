package com.example.appdevelopmentprojectfinal.timetable;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.appdevelopmentprojectfinal.utils.JsonUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class Module {
    private String code;
    private String name;
    private String lecturer;
    private boolean show;
    private String documentId;
    private String type;
    private String userId;

    private List<TimeSlot> timeSlotList;
    private List<TimeSlot> alternativeSlots;

    public Module() {
        // (required for firestore)
    }

    public Module(String code, String name, String lecturer, boolean show) {
        this.code = code;
        this.name = name;
        this.lecturer = lecturer;
        this.show = show;
        this.timeSlotList = new ArrayList<>();
        this.alternativeSlots = new ArrayList<>();
    }

    // Getters
    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isShow() {
        return show;
    }

    public String getType() {
        return type;
    }

    public List<TimeSlot> getTimeSlotList() {
        return timeSlotList;
    }

    public List<TimeSlot> getAlternativeSlots() {
        return alternativeSlots;
    }

    public String getLecturer() {
        return lecturer;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getUserId() {
        return userId;
    }


    public void setLecturer(String lecturer) {
        this.lecturer = lecturer;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public void setTimeSlotList(List<TimeSlot> timeSlotList) {
        this.timeSlotList = timeSlotList;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAlternativeSlots(List<TimeSlot> alternativeSlots) {
        this.alternativeSlots = alternativeSlots;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        StringBuilder slotsString = new StringBuilder("[");
        if (timeSlotList != null && !timeSlotList.isEmpty()) {
            for (int i = 0; i < timeSlotList.size(); i++) {
                slotsString.append(timeSlotList.get(i).toString());
                if (i < timeSlotList.size() - 1) {
                    slotsString.append(",");
                }
            }
        }
        slotsString.append("]");

        StringBuilder alternativeSlotsString = new StringBuilder("[");
        if (alternativeSlots != null && !alternativeSlots.isEmpty()) {
            for (int i = 0; i < alternativeSlots.size(); i++) {
                alternativeSlotsString.append(alternativeSlots.get(i).toString());
                if (i < alternativeSlots.size() - 1) {
                    alternativeSlotsString.append(",");
                }
            }
        }
        alternativeSlotsString.append("]");

        return "{" +
                "\"code\":\"" + code + "\"," +
                "\"name\":\"" + name + "\"," +
                "\"lecturer\":\"" + lecturer + "\"," +
                "\"type\":\"" + type + "\"," +
                "\"userId\":\"" + (userId != null ? userId : "") + "\"," +
                "\"show\":" + show + "," +
                "\"slots\":" + slotsString.toString() + "," +
                "\"alternativeSlots\":" + alternativeSlotsString.toString() +
                "}";
    }
}