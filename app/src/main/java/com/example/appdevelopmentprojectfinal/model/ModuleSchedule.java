package com.example.appdevelopmentprojectfinal.model;

public class ModuleSchedule {
    private Module module;
    private TimeSlot timeSlot;
    private boolean isMovable;

    public ModuleSchedule(Module module, TimeSlot timeSlot, boolean isMovable) {
        this.module = module;
        this.timeSlot = timeSlot;
        this.isMovable = isMovable;
    }

    // Getters
    public Module getModule() {
        return module;
    }
    public TimeSlot getTimeSlot() {
        return timeSlot;
    }
    public boolean isMovable() {
        return isMovable;
    }

    // Setter for when a module is moved
    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }
}