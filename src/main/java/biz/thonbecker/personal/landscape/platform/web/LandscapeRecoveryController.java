package biz.thonbecker.personal.landscape.platform.web;

import biz.thonbecker.personal.landscape.platform.LandscapeRecoveryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class LandscapeRecoveryController {
    private final LandscapeRecoveryService recoveryService;
    private final LandscapeOwnerCookie ownerCookie;

    @PostMapping("/landscape/recovery")
    public String request(
            @RequestParam @Email final String email,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        recoveryService.request(
                email, ownerCookie.resolve(request, response), request.getScheme() + "://" + request.getServerName());
        return "redirect:/landscape?recoveryRequested";
    }

    @GetMapping("/landscape/recovery/confirm")
    public String confirm(
            @RequestParam final String token, final HttpServletRequest request, final HttpServletResponse response) {
        return recoveryService
                .confirm(token)
                .map(ownerId -> {
                    ownerCookie.restore(ownerId, request, response);
                    return "redirect:/landscape?recovered";
                })
                .orElse("redirect:/landscape?recoveryInvalid");
    }
}
