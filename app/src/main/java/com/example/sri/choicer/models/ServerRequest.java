package com.example.sri.choicer.models;

/**
 * Created by Sri on 10/27/2017.
 * Request Class for Retrofit 2 HTTP calls
 */

public class ServerRequest {

    private String operation;
    private User user;
    private Choice choice;

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setChoice(Choice choice) {
        this.choice = choice;
    }
}