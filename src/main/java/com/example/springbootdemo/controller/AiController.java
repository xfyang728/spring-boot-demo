package com.example.springbootdemo.controller;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.example.springbootdemo.model.CarSale;
import com.example.springbootdemo.model.dataReq;
import com.example.springbootdemo.service.DataService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("ai")
public class AiController {
    @Resource
    DataService dataService;

    @PostMapping("query")
    public List query(@RequestBody dataReq req) {
//        HashMap<String, Object> paramMap = new HashMap<>();
//        paramMap.put("content", "表名: car_sale 字段定义字段名称  字段类型 描述 \\n 省份 varchar 订单数量 int 销售额 decimal 折扣 decimal 依据字段定义生成各天订单数量的查询sql");
//
//        String result= HttpUtil.post("http://127.0.0.1:5000/ask", JSONUtil.toJsonStr(paramMap));

        List list = dataService.query(req.getResult());
        return list;
    }
}
