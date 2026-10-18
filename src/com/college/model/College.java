
package com.college.model;

public class College {
    private int collegeId;
    private String collegeName;
    private String city;
    private String university;
    private String address;

    public College(int collegeId, String collegeName, String city,
                   String university, String address) {
        this.collegeId = collegeId;
        this.collegeName = collegeName;
        this.city = city;
        this.university = university;
        this.address = address;
    }

    public int getCollegeId() {
        return collegeId;
    }

    public void setCollegeId(int collegeId) {
        this.collegeId = collegeId;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return collegeName + " - " + city;
    }
}
