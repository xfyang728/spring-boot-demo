package com.example.springbootdemo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *  Controller-->service接口-->serviceImpl-->dao接口-->daoImpl-->mapper-->db
 */
@RestController
@RequestMapping("demo")
public class democontroller {
    @PostMapping("test")
    public String HHH() {
        return "super.toString()";
    }
}
