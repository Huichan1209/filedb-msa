package com.example.product.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class PageController
{
    @GetMapping("/dashboard")
    public String redirectTestPage()
    {
        return "dashboard";
    }
}
