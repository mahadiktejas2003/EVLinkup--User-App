package com.example.evchargingfinal;

import com.google.firebase.firestore.GeoPoint;

public class Owner {
    String owner_id, owner_email, owner_name, ev_station_name;
    double avg_rating;
    private double amount_earned;
    private GeoPoint owner_location;
    int charging_points, price, charging_point_com_type_1, charging_point_com_type_2, charging_point_com_type_3, reviews;


    double lat,lang;

    public Owner() {
    }

    public Owner(String owner_id, String owner_email, String owner_name, String ev_station_name, double avg_rating, GeoPoint owner_location, int charging_points, int price, int charging_point_com_type_1, int charging_point_com_type_2, int charging_point_com_type_3, int reviews, double amount_earned) {
        this.owner_id = owner_id;
        this.owner_email = owner_email;
        this.owner_name = owner_name;
        this.ev_station_name = ev_station_name;
        this.avg_rating = avg_rating;
        this.owner_location = owner_location;
        this.charging_points = charging_points;
        this.price = price;
        this.charging_point_com_type_1 = charging_point_com_type_1;
        this.charging_point_com_type_2 = charging_point_com_type_2;
        this.charging_point_com_type_3 = charging_point_com_type_3;
        this.reviews = reviews;
        this.amount_earned = amount_earned;

    }


    public Owner(String owner_id, String owner_email, String owner_name, String ev_station_name, double avg_rating, GeoPoint owner_location, int charging_points, int price, int charging_point_com_type_1, int charging_point_com_type_2, int charging_point_com_type_3, int reviews,double lat,double lang) {
        this.owner_id = owner_id;
        this.owner_email = owner_email;
        this.owner_name = owner_name;
        this.ev_station_name = ev_station_name;
        this.avg_rating = avg_rating;
        this.owner_location = owner_location;
        this.charging_points = charging_points;
        this.price = price;
        this.charging_point_com_type_1 = charging_point_com_type_1;
        this.charging_point_com_type_2 = charging_point_com_type_2;
        this.charging_point_com_type_3 = charging_point_com_type_3;
        this.reviews = reviews;
        this.lat = lat;
        this.lang = lang;
    }

    public String getOwner_id() {
        return owner_id;
    }

    public void setOwner_id(String owner_id) {
        this.owner_id = owner_id;
    }

    public String getOwner_email() {
        return owner_email;
    }

    public void setOwner_email(String owner_email) {
        this.owner_email = owner_email;
    }

    public String getOwner_name() {
        return owner_name;
    }

    public void setOwner_name(String owner_name) {
        this.owner_name = owner_name;
    }

    public String getEv_station_name() {
        return ev_station_name;
    }

    public void setEv_station_name(String ev_station_name) {
        this.ev_station_name = ev_station_name;
    }

    public double getAvg_rating() {
        return avg_rating;
    }

    public void setAvg_rating(double avg_rating) {
        this.avg_rating = avg_rating;
    }

    public GeoPoint getOwner_location() {
        return owner_location;
    }

    public void setOwner_location(GeoPoint owner_location) {
        this.owner_location = owner_location;
    }

    public int getCharging_points() {
        return charging_points;
    }

    public void setCharging_points(int charging_points) {
        this.charging_points = charging_points;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getCharging_point_com_type_1() {
        return charging_point_com_type_1;
    }

    public void setCharging_point_com_type_1(int charging_point_com_type_1) {
        this.charging_point_com_type_1 = charging_point_com_type_1;
    }

    public int getCharging_point_com_type_2() {
        return charging_point_com_type_2;
    }

    public void setCharging_point_com_type_2(int charging_point_com_type_2) {
        this.charging_point_com_type_2 = charging_point_com_type_2;
    }

    public int getCharging_point_com_type_3() {
        return charging_point_com_type_3;
    }

    public void setCharging_point_com_type_3(int charging_point_com_type_3) {
        this.charging_point_com_type_3 = charging_point_com_type_3;
    }

    public int getReviews() {
        return reviews;
    } // Keep this

    public double getLat() {
        return lat;
    }

    public double getAmount_earned() {  // Add this getter
        return amount_earned;
    }
    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLang() {
        return lang;
    }
    public void setAmount_earned(double amount_earned) {
        this.amount_earned = amount_earned;
    }
    public void setLang(double lang) {
        this.lang = lang;
    }

    public void setReviews(int reviews) {
        this.reviews = reviews;
    }
}