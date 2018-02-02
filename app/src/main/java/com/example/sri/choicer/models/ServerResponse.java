package com.example.sri.choicer.models;

/**
 * Created by Sri on 10/27/2017.
 * Response class for Retrofit 2 HTTP calls
 */

public class ServerResponse {

    private String result;
    private String message;
    private User user;
    private Choice choice;

    public String getResult() {
        return result;
    }

    public String getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }

    public Choice getChoice() {
        return choice;
    }

}