package biz.thonbecker.personal.foosball.platform.tournament.algorithm;

import biz.thonbecker.personal.foosball.platform.persistence.Tournament;
import biz.thonbecker.personal.foosball.platform.persistence.TournamentMatch;
import biz.thonbecker.personal.foosball.platform.persistence.TournamentRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Single Elimination Tournament Algorithm
 * Creates a traditional elimination bracket where teams are eliminated after one loss
 */
@Component
public class SingleEliminationAlgorithm implements TournamentAlgorithm {

    @Override
    public List<TournamentMatch> generateBracket(Tournament tournament, List<TournamentRegistration> registrations) {
        if (registrations.size() < getMinimumParticipants()) {
            throw new IllegalArgumentException("Not enough participants for single elimination tournament");
        }

        final var matches = new ArrayList<TournamentMatch>();
        final var shuffledRegistrations = new ArrayList<>(registrations);

        // Shuffle if no seeding
        if (registrations.stream().noneMatch(r -> r.getSeed() != null)) {
            Collections.shuffle(shuffledRegistrations);
        } else {
            // Sort by seed (nulls last)
            shuffledRegistrations.sort((r1, r2) -> {
                if (r1.getSeed() == null && r2.getSeed() == null) return 0;
                if (r1.getSeed() == null) return 1;
                if (r2.getSeed() == null) return -1;
                return Integer.compare(r1.getSeed(), r2.getSeed());
            });
        }

        // Calculate number of rounds needed
        final var participantCount = shuffledRegistrations.size();
        final var roundCount = (int) Math.ceil(Math.log(participantCount) / Math.log(2));

        // Create bracket structure
        matches.addAll(createBracketStructure(tournament, participantCount, roundCount));

        // Assign teams to first round
        assignTeamsToFirstRound(matches, shuffledRegistrations, participantCount);

        return matches;
    }

    private List<TournamentMatch> createBracketStructure(Tournament tournament, int participantCount, int roundCount) {
        final var matches = new ArrayList<TournamentMatch>();

        for (var round = 1; round <= roundCount; round++) {
            final var matchesInRound = (int) Math.pow(2, roundCount - round);

            for (var matchNum = 1; matchNum <= matchesInRound; matchNum++) {
                final var match = new TournamentMatch(tournament, round, matchNum);
                match.setBracketType(TournamentMatch.BracketType.MAIN);
                matches.add(match);
            }
        }

        // Set up advancement paths
        setupAdvancementPaths(matches, roundCount);

        return matches;
    }

    private void setupAdvancementPaths(List<TournamentMatch> matches, int roundCount) {
        for (var match : matches) {
            final var currentRound = match.getRoundNumber();
            final var currentMatch = match.getMatchNumber();

            // Skip final match
            if (currentRound == roundCount) continue;

            // Find next match for winner
            final var nextRound = currentRound + 1;
            final var nextMatch = (currentMatch + 1) / 2; // Integer division for pairing

            final var nextTournamentMatch = matches.stream()
                    .filter(m -> m.getRoundNumber().equals(nextRound)
                            && m.getMatchNumber().equals(nextMatch))
                    .findFirst()
                    .orElse(null);

            if (nextTournamentMatch != null) {
                match.setNextMatch(nextTournamentMatch);
            }
        }
    }

    private void assignTeamsToFirstRound(
            List<TournamentMatch> matches, List<TournamentRegistration> registrations, int participantCount) {

        // Get first round matches
        final var firstRoundMatches = matches.stream()
                .filter(m -> m.getRoundNumber() == 1)
                .sorted((m1, m2) -> Integer.compare(m1.getMatchNumber(), m2.getMatchNumber()))
                .toList();

        // Assign teams with byes going to lower seeds
        var registrationIndex = 0;
        for (TournamentMatch match : firstRoundMatches) {
            if (registrationIndex < registrations.size()) {
                match.setTeam1(registrations.get(registrationIndex++));
            }

            if (registrationIndex < registrations.size()) {
                match.setTeam2(registrations.get(registrationIndex++));
            }

            match.updateStatus();

            // Handle byes - automatically advance team to next round
            if (match.hasBye()) {
                TournamentRegistration byeWinner = match.getByeWinner();
                if (byeWinner != null && match.getNextMatch() != null) {
                    advanceBye(match, byeWinner);
                }
            }
        }
    }

    private void advanceBye(TournamentMatch byeMatch, TournamentRegistration byeWinner) {
        byeMatch.setWinner(byeWinner);
        byeMatch.setStatus(TournamentMatch.MatchStatus.WALKOVER);

        TournamentMatch nextMatch = byeMatch.getNextMatch();
        if (nextMatch != null) {
            if (nextMatch.getTeam1() == null) {
                nextMatch.setTeam1(byeWinner);
            } else if (nextMatch.getTeam2() == null) {
                nextMatch.setTeam2(byeWinner);
            }
            nextMatch.updateStatus();
        }
    }

    @Override
    public List<TournamentMatch> advanceWinner(TournamentMatch completedMatch) {
        final var updatedMatches = new ArrayList<TournamentMatch>();

        if (completedMatch.getWinner() == null || !completedMatch.isCompleted()) {
            return updatedMatches;
        }

        final var nextMatch = completedMatch.getNextMatch();
        if (nextMatch != null) {
            final var winner = completedMatch.getWinner();

            // Determine which slot in next match to fill
            if (nextMatch.getTeam1() == null) {
                nextMatch.setTeam1(winner);
            } else if (nextMatch.getTeam2() == null) {
                nextMatch.setTeam2(winner);
            }

            nextMatch.updateStatus();
            updatedMatches.add(nextMatch);
        }

        return updatedMatches;
    }

    @Override
    public boolean isTournamentComplete(Tournament tournament) {
        final var matches = tournament.getMatches();

        // Find the final match (highest round number)
        final var maxRound =
                matches.stream().mapToInt(TournamentMatch::getRoundNumber).max().orElse(0);

        return matches.stream().filter(m -> m.getRoundNumber() == maxRound).allMatch(TournamentMatch::isCompleted);
    }

    @Override
    public int getMinimumParticipants() {
        return 2;
    }

    @Override
    public boolean isValidParticipantCount(int participantCount) {
        return participantCount >= getMinimumParticipants();
    }

    @Override
    public Tournament.TournamentType getTournamentType() {
        return Tournament.TournamentType.SINGLE_ELIMINATION;
    }
}
