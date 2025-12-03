package com.example.whatsapp;

public class Contacts {
    private String name;
    private String status;
    private String image;
    private String uid; // 1. THÊM BIẾN NÀY

    public Contacts() { }

    public Contacts(String name, String status, String image, String uid) {
        this.name = name;
        this.status = status;
        this.image = image;
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    // 2. THÊM GETTER VÀ SETTER CHO UID (Quan trọng)
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}