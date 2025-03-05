package com.example.appdevelopmentprojectfinal.model;

public class TimeSlot {
    private String day;
    private String startTime;
    private String endTime;
    private String location;

    public TimeSlot(String day, String startTime, String endTime, String location) {
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
    }

    // Getters
    public String getDay() {
        return day;
    }
    public String getStartTime() {
        return startTime;
    }
    public String getEndTime() {
        return endTime;
    }
    public String getLocation() {
        return location;
    }
}