package com.example.chatternet.model;

import com.google.firebase.Timestamp;

public class ChatImageModel {

    private String senderId;
    private Timestamp timestamp;
    private String imageUrl;

    public ChatImageModel() {
    }

    public ChatImageModel(String senderId, Timestamp timestamp, String imageUrl) {
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.imageUrl = imageUrl;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
