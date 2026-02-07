package biz.thonbecker.personal.content.platform.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class ExperienceService {
    private final LocalDate startDate = LocalDate.of(2005, 1, 1);

    public long calculateYearsOfExperience() {
        return ChronoUnit.YEARS.between(startDate, LocalDate.now());
    }
}
