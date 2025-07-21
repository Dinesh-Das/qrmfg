package com.cqs.qrmfg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardingController {
    @RequestMapping(value = {"/qrmfg", "/qrmfg/{path:[^\\.]*}"})
    public String forward() {
        return "forward:/index.html";
    }
} 