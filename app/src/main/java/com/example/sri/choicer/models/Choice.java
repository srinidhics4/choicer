package com.example.sri.choicer.models;

/**
 * Created by Sri on 10/29/2017.
 * Choice Java Bean Class
 */

public class Choice {
    private int id;
    private int ch;
    private String email;
    private String title;
    private String img_name_1;
    private String img_name_2;
    private int vote1;
    private int vote2;
    private int end_status;


    public int getId(){
        return id;
    }

    public int getChoice(){
        return ch;
    }

    public String getEmail() {
        return email;
    }

    public String getTitle(){ return title; }

    public String getImgLink1() {
        return img_name_1;
    }

    public String getImgLink2() {
        return img_name_2;
    }

    public int getVoteCount1(){ return vote1;}

    public int getVoteCount2(){ return vote2;}

    public int getStatus() { return end_status; }

    public void setId(int id){
        this.id = id;
    }

    public void setCh(int ch){
        this.ch = ch;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setTitle(String title){ this.title = title; }

    public void setImgLink1(String img_name_1) {
        this.img_name_1 = img_name_1;
    }

    public void setImgLink2(String img_name_2) {
        this.img_name_2 = img_name_2;
    }

}
