package com.inha.makko;

import java.io.Serializable;
import java.util.ArrayList;

public class User implements Serializable {
    public String name;
    public String email;
    public String uid;
    public Double latitude;
    public Double longitude;
    public Integer accuracyInMeters;
    public ArrayList<String> friendArray;
}
