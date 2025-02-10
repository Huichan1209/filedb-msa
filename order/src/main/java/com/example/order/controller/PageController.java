package com.example.order.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController
{
    @GetMapping("/dashboard")
    public String redirectTestPage()
    {
        return "dashboard";
    }
}
