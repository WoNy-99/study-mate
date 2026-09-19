package com.example.studymate;

public class ScheduleItem {
    private final String subject;
    private final String day;
    private final int startHour;
    private final int endHour;

    public ScheduleItem(String subject, String day, int startHour, int endHour) {
        this.subject = subject;
        this.day = day;
        this.startHour = startHour;
        this.endHour = endHour;
    }

    public String getSubject() { return subject; }
    public String getDay() { return day; }
    public int getStartHour() { return startHour; }
    public int getEndHour() { return endHour; }
}
