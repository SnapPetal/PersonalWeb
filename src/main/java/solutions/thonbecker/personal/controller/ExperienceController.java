package solutions.thonbecker.personal.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import solutions.thonbecker.personal.service.ExperienceService;

@RestController
@RequiredArgsConstructor
public class ExperienceController {
    private final ExperienceService experienceService;

    @GetMapping("/api/experience/count")
    @ResponseBody
    public String getYearsOfExperience() {
        long years = experienceService.calculateYearsOfExperience();
        return years + "+ years of experience";
    }
}
