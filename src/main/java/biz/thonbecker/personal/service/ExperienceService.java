package biz.thonbecker.personal.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class ExperienceService {
    private final LocalDate startDate = LocalDate.of(2005, 1, 1);

    public long calculateYearsOfExperience() {
        return ChronoUnit.YEARS.between(startDate, LocalDate.now());
    }
}
