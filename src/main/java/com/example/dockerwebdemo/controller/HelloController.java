package com.example.dockerwebdemo.controller;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("")
public class HelloController {

    @Resource
    RedisKeyValueTemplate redisKeyValueTemplate;

    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @GetMapping("")
    public String index() {
        return "this is index page!";
    }

    @GetMapping("/hello")
    public String hello() {
        int count;
        Object countObj = redisTemplate.opsForValue().get("count");
        if (countObj == null) {
            count = 1;
        } else {
            count = (int) countObj;
            count++;
        }
        redisTemplate.opsForValue().set("count", count);
        return "hello,docker:" + count;
    }
}
