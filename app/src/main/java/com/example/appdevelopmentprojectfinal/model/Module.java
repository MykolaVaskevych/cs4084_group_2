package com.example.appdevelopmentprojectfinal.model;
public class Module {
    private String code;
    private String name;
    private String lecturer;

    public Module(String code, String name, String lecturer) {
        this.code = code;
        this.name = name;
        this.lecturer = lecturer;
    }

    // Getters
    public String getCode() {
        return code;
    }
    public String getName() {
        return name;
    }
    public String getLecturer() {
        return lecturer;
    }
}