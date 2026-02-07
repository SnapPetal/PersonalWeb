package biz.thonbecker.personal.content.platform.web;

import biz.thonbecker.personal.content.platform.service.ExperienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
