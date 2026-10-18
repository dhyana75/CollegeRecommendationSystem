package com.college.model;

public class Cutoff {
    private int cutoffId;
    private int courseId;
    private String category;
    private double cutoffPercentile;
    private String examName;
    private String choiceCode;
    private int year;
    private int capRound;
    private int meritRank;

    public Cutoff(int cutoffId, int courseId, String category, double cutoffPercentile, String examName) {
        this(cutoffId, courseId, category, cutoffPercentile, examName, "", 0, 1, -1);
    }

    public Cutoff(int cutoffId, int courseId, String category, double cutoffPercentile,
                  String examName, String choiceCode, int year, int capRound, int meritRank) {
        this.cutoffId = cutoffId;
        this.courseId = courseId;
        this.category = category;
        this.cutoffPercentile = cutoffPercentile;
        this.examName = examName;
        this.choiceCode = choiceCode == null ? "" : choiceCode;
        this.year = year;
        this.capRound = capRound;
        this.meritRank = meritRank;
    }

    public int getCutoffId() { return cutoffId; }
    public void setCutoffId(int cutoffId) { this.cutoffId = cutoffId; }
    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getCutoffPercentile() { return cutoffPercentile; }
    public void setCutoffPercentile(double cutoffPercentile) { this.cutoffPercentile = cutoffPercentile; }
    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }
    public String getChoiceCode() { return choiceCode; }
    public void setChoiceCode(String choiceCode) { this.choiceCode = choiceCode == null ? "" : choiceCode; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getCapRound() { return capRound; }
    public void setCapRound(int capRound) { this.capRound = capRound; }
    public int getMeritRank() { return meritRank; }
    public void setMeritRank(int meritRank) { this.meritRank = meritRank; }
}
