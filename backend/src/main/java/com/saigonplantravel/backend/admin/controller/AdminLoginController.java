package com.saigonplantravel.backend.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminLoginController {
    @GetMapping("/admin/login")
    String loginPage() {
        return "admin/login";
    }
}
