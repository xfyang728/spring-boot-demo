package com.example.springbootdemo.service;

import com.example.springbootdemo.model.CarSale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataService {
    @Autowired
    private JdbcTemplate jdbc;

    public List query(String sql) {
//        String sql = "select 日期 from public.car_sale";
        return jdbc.queryForList(sql);
    }
}
