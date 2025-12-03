package com.example.whatsapp;

public class Chatlist {
    public String id; // ID người dùng (Key)
    public long time; // Thời gian

    public Chatlist() {
    }

    public Chatlist(String id, long time) {
        this.id = id;
        this.time = time;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}