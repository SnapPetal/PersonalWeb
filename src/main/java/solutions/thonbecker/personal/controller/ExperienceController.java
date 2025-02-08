package solutions.thonbecker.personal.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import solutions.thonbecker.personal.service.ExperienceService;

@RestController
@RequiredArgsConstructor
public class ExperienceController {
    private final ExperienceService experienceService;

    @GetMapping("/api/experience/count")
    public String getExperienceCount() {
        long years = experienceService.calculateYearsOfExperience();
        return String.format("<span class=\"exp-number\" data-count=\"%d\">0</span>+ years of experience", years);
    }
}
