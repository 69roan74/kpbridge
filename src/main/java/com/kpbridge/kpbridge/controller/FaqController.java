package com.kpbridge.kpbridge.controller;

import com.kpbridge.kpbridge.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class FaqController {

    private final FaqRepository faqRepository;

    @GetMapping("/faq")
    public String faq(Model model) {
        // DB에서 모든 FAQ를 가져와서 화면에 던져줌
        model.addAttribute("faqs", faqRepository.findAll());
        return "faq";
    }
}