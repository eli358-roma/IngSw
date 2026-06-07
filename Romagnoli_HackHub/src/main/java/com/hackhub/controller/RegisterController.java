package com.hackhub.controller;

import com.hackhub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegisterController {

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String confirmPassword,
                             @RequestParam(defaultValue = "USER") String role) {

        if (!password.equals(confirmPassword)) {
            return "redirect:/register?error=Le+password+non+corrispondono";
        }

        try {
            userService.createUser(email, username, password, role);
            return "redirect:/login?registered=true";
        } catch (Exception e) {
            return "redirect:/register?error=" + e.getMessage();
        }
    }
}