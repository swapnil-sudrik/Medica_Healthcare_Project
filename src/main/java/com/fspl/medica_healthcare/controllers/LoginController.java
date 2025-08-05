package com.fspl.medica_healthcare.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {
    @GetMapping("/login")
    public String loginPage() {
        System.out.println("Call----------");
        return "login";
    }

//    @PostMapping("/loginUser")
//    public String login(
//            @RequestParam("username") String username,
//            @RequestParam("password") String password,
//            HttpSession session,
//            Model model) {
//        try {
//
//            System.out.println("Token received: ");
//            return "redirect:/patientsWeb";
//        } catch (RuntimeException e) {
//            model.addAttribute("error", "Invalid username or password");
//            return "login";
//        }
//    }

    @GetMapping("/patientsWeb")
    public String showPatientListPage(Model model) {

        return "layout";
    }
}
