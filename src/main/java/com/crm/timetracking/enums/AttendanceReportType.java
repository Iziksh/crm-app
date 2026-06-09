package com.crm.timetracking.enums;

public enum AttendanceReportType {

    //                    hebrewLabel   countsAsWorked  creditsStandardHours
    PRESENCE     ("נוכחות",   true,  false),
    VACATION     ("חופשה",    false, true),
    SICK         ("מחלה",     false, true),
    RESERVE_DUTY ("מילואים",  false, true),
    HOLIDAY      ("חג",       false, true),
    ABSENCE      ("היעדרות",  false, false);

    private final String  hebrewLabel;
    private final boolean countsAsWorked;
    private final boolean creditsStandardHours;

    AttendanceReportType(String hebrewLabel, boolean countsAsWorked, boolean creditsStandardHours) {
        this.hebrewLabel          = hebrewLabel;
        this.countsAsWorked       = countsAsWorked;
        this.creditsStandardHours = creditsStandardHours;
    }

    public String  getHebrewLabel()          { return hebrewLabel; }
    public boolean isCountsAsWorked()        { return countsAsWorked; }
    public boolean isCreditsStandardHours()  { return creditsStandardHours; }
}