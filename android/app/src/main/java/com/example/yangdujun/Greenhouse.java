package com.example.yangdujun;

public class Greenhouse {
    public String id;
    public String name;
    public String distance;
    
    public Greenhouse(String id, String name, String distance) {
        this.id = id;
        this.name = name;
        this.distance = distance;
    }
    
    public Greenhouse(String name, String distance) {
        this.name = name;
        this.distance = distance;
    }
}


