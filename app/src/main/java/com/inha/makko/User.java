package com.inha.makko;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.ArrayList;

@IgnoreExtraProperties
public class User implements Serializable {
    public String name;
    public String email;
    public String uid;
    public Double latitude;
    public Double longitude;
    public Integer accuracyInMeters;
    public String address;
    public String roadAddress;
    public Long lastUpdateAt;
    public ArrayList<String> friendArray;
}
