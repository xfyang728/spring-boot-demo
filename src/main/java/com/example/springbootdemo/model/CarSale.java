package com.example.springbootdemo.model;

import lombok.Data;

@Data
public class CarSale {
    private String data;
    private String id;
    private String type;
    private int count;
    private double value;
}
