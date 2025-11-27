package biz.thonbecker.personal.foosball.infrastructure.web.model;

public record GameRequest(
        String whiteTeamPlayer1,
        String whiteTeamPlayer2,
        String blackTeamPlayer1,
        String blackTeamPlayer2,
        int whiteTeamScore,
        int blackTeamScore) {}
