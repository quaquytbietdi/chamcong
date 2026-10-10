package com.example.appchamcong.model;

public class Shift {

    private final String id;
    private final String name;
    private final int startHour;
    private final int startMinute;
    private final int endHour;
    private final int endMinute;

    public Shift(
            String id,
            String name,
            int startHour,
            int startMinute,
            int endHour,
            int endMinute
    ) {
        this.id = id;
        this.name = name;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.endHour = endHour;
        this.endMinute = endMinute;
    }

    // Constructor cũ để code hiện tại không bị vỡ
    public Shift(
            String name,
            int startHour,
            int startMinute,
            int endHour,
            int endMinute
    ) {
        this(
                name.toLowerCase().replace(" ", "_"),
                name,
                startHour,
                startMinute,
                endHour,
                endMinute
        );
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getStartHour() {
        return startHour;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public int getEndHour() {
        return endHour;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public String getStartTime() {
        return String.format(
                "%02d:%02d",
                startHour,
                startMinute
        );
    }

    public String getEndTime() {
        return String.format(
                "%02d:%02d",
                endHour,
                endMinute
        );
    }

    public String getTimeLabel() {
        return getStartTime() + " - " + getEndTime();
    }

    public String getDisplayLabel() {
        return name + " (" + getTimeLabel() + ")";
    }
}