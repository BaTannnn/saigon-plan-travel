package com.saigonplantravel.backend.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminHomeController {

    @GetMapping({"/admin", "/admin/"})
    public String adminHome() {
        return "redirect:/admin/places";
    }
}