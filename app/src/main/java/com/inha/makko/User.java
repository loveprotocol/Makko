package com.inha.makko;

import java.io.Serializable;

public class User implements Serializable {
    String name;
    String email;
    String uid;
    Double latitude;
    Double longtide;
    Integer accuracyInMeters;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongtide() {
        return longtide;
    }

    public void setLongtide(Double longtide) {
        this.longtide = longtide;
    }

    public Integer getAccuracyInMeters() {
        return accuracyInMeters;
    }

    public void setAccuracyInMeters(Integer accuracyInMeters) {
        this.accuracyInMeters = accuracyInMeters;
    }
}
