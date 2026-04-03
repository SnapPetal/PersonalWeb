package biz.thonbecker.personal.shared.platform.configuration;

import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
class BuildInfoAdvice {

    private final String appVersion;

    BuildInfoAdvice(BuildProperties buildProperties, GitProperties gitProperties) {
        this.appVersion = buildProperties.getVersion() + " (" + gitProperties.getShortCommitId() + ")";
    }

    @ModelAttribute("appVersion")
    String appVersion() {
        return appVersion;
    }
}
