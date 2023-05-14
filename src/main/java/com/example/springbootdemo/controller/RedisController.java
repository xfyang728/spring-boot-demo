package com.example.springbootdemo.controller;

import com.example.springbootdemo.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("redis")
public class RedisController {

    private final RedisTemplate redisTemplate;

    @Autowired
    private RedisUtil redisUtil;

    public RedisController(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("save")
    public String save(String key, String value){
        redisUtil.set(key, value);
        return "success";
    }

}
