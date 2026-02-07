package biz.thonbecker.personal.foosball.platform.tournament.algorithm;

import biz.thonbecker.personal.foosball.platform.persistence.Tournament;
import biz.thonbecker.personal.foosball.platform.persistence.TournamentMatch;
import biz.thonbecker.personal.foosball.platform.persistence.TournamentRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Double Elimination Tournament Algorithm
 * Creates a bracket with both winner's and loser's brackets.
 * Teams are eliminated after TWO losses.
 */
@Component
public class DoubleEliminationAlgorithm implements TournamentAlgorithm {

    @Override
    public List<TournamentMatch> generateBracket(Tournament tournament, List<TournamentRegistration> registrations) {
        if (registrations.size() < getMinimumParticipants()) {
            throw new IllegalArgumentException("Not enough participants for double elimination tournament");
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

        final var participantCount = shuffledRegistrations.size();
        final var winnersBracketRounds = (int) Math.ceil(Math.log(participantCount) / Math.log(2));

        // Create winner's bracket (like single elimination)
        final var winnersBracketMatches = createWinnersBracket(tournament, participantCount, winnersBracketRounds);
        matches.addAll(winnersBracketMatches);

        // Create loser's bracket
        final var losersBracketMatches = createLosersBracket(tournament, participantCount, winnersBracketRounds);
        matches.addAll(losersBracketMatches);

        // Create grand finals
        final var grandFinalsMatches = createGrandFinals(tournament, winnersBracketRounds);
        matches.addAll(grandFinalsMatches);

        // Link brackets together
        linkBrackets(winnersBracketMatches, losersBracketMatches, grandFinalsMatches);

        // Assign teams to first round of winner's bracket
        assignTeamsToFirstRound(winnersBracketMatches, shuffledRegistrations, participantCount);

        return matches;
    }

    private List<TournamentMatch> createWinnersBracket(Tournament tournament, int participantCount, int roundCount) {
        final var matches = new ArrayList<TournamentMatch>();

        for (var round = 1; round <= roundCount; round++) {
            final var matchesInRound = (int) Math.pow(2, roundCount - round);

            for (var matchNum = 1; matchNum <= matchesInRound; matchNum++) {
                final var match = new TournamentMatch(tournament, round, matchNum);
                match.setBracketType(TournamentMatch.BracketType.MAIN);
                matches.add(match);
            }
        }

        // Set up advancement paths for winner's bracket
        setupWinnersBracketPaths(matches, roundCount);

        return matches;
    }

    private List<TournamentMatch> createLosersBracket(
            Tournament tournament, int participantCount, int winnersBracketRounds) {
        final var matches = new ArrayList<TournamentMatch>();

        // Loser's bracket has (2 * winnersBracketRounds - 1) rounds
        // Each round alternates between receiving new losers and playing among existing losers
        final var losersBracketRounds = 2 * winnersBracketRounds - 1;

        for (var round = 1; round <= losersBracketRounds; round++) {
            // Calculate matches in this round
            final var matchesInRound = calculateLosersBracketMatches(round, winnersBracketRounds, participantCount);

            for (var matchNum = 1; matchNum <= matchesInRound; matchNum++) {
                final var match = new TournamentMatch(tournament, round, matchNum);
                match.setBracketType(TournamentMatch.BracketType.LOSERS);
                matches.add(match);
            }
        }

        // Set up advancement paths for loser's bracket
        setupLosersBracketPaths(matches, losersBracketRounds);

        return matches;
    }

    private int calculateLosersBracketMatches(int round, int winnersBracketRounds, int participantCount) {
        // Round 1: Half of initial participants (losers from WB round 1)
        if (round == 1) {
            final var winnersR1Matches = (int) Math.pow(2, winnersBracketRounds - 1);
            return winnersR1Matches / 2;
        }

        // Odd rounds: Same as previous round (playing among existing losers)
        // Even rounds: Receive new losers from winner's bracket
        final var winnersRoundEquivalent = (round + 1) / 2;
        final var matchesFromWinners = (int) Math.pow(2, winnersBracketRounds - winnersRoundEquivalent - 1);

        if (round % 2 == 0) {
            // Even round: new losers join
            return Math.max(1, matchesFromWinners);
        } else {
            // Odd round: play among existing
            return Math.max(1, matchesFromWinners);
        }
    }

    private List<TournamentMatch> createGrandFinals(Tournament tournament, int winnersBracketRounds) {
        final var matches = new ArrayList<TournamentMatch>();

        // Grand Finals (single match)
        final var grandFinals = new TournamentMatch(tournament, winnersBracketRounds + 1, 1);
        grandFinals.setBracketType(TournamentMatch.BracketType.MAIN);
        matches.add(grandFinals);

        return matches;
    }

    private void setupWinnersBracketPaths(List<TournamentMatch> winnersBracket, int roundCount) {
        for (var match : winnersBracket) {
            final var currentRound = match.getRoundNumber();
            final var currentMatch = match.getMatchNumber();

            // Skip final match of winner's bracket
            if (currentRound == roundCount) continue;

            // Find next match for winner
            final var nextRound = currentRound + 1;
            final var nextMatch = (currentMatch + 1) / 2;

            final var nextTournamentMatch = winnersBracket.stream()
                    .filter(m -> m.getRoundNumber().equals(nextRound)
                            && m.getMatchNumber().equals(nextMatch))
                    .findFirst()
                    .orElse(null);

            if (nextTournamentMatch != null) {
                match.setNextMatch(nextTournamentMatch);
            }
        }
    }

    private void setupLosersBracketPaths(List<TournamentMatch> losersBracket, int roundCount) {
        for (var match : losersBracket) {
            final var currentRound = match.getRoundNumber();
            final var currentMatch = match.getMatchNumber();

            // Skip final match of loser's bracket
            if (currentRound == roundCount) continue;

            // Find next match in loser's bracket
            final var nextRound = currentRound + 1;
            final var nextMatch = calculateNextLosersBracketMatch(currentRound, currentMatch);

            final var nextTournamentMatch = losersBracket.stream()
                    .filter(m -> m.getRoundNumber().equals(nextRound)
                            && m.getMatchNumber().equals(nextMatch))
                    .findFirst()
                    .orElse(null);

            if (nextTournamentMatch != null) {
                match.setNextMatch(nextTournamentMatch);
            }
        }
    }

    private int calculateNextLosersBracketMatch(int currentRound, int currentMatch) {
        // Odd rounds feed into same position in next round
        // Even rounds pair up: matches 1,2 -> 1; matches 3,4 -> 2, etc.
        if (currentRound % 2 == 0) {
            return (currentMatch + 1) / 2;
        } else {
            return currentMatch;
        }
    }

    private void linkBrackets(
            List<TournamentMatch> winnersBracket,
            List<TournamentMatch> losersBracket,
            List<TournamentMatch> grandFinals) {

        // Link winner's bracket final to grand finals
        final var winnersFinal = winnersBracket.stream()
                .filter(m -> m.getNextMatch() == null)
                .findFirst()
                .orElse(null);

        if (winnersFinal != null && !grandFinals.isEmpty()) {
            winnersFinal.setNextMatch(grandFinals.get(0));
        }

        // Link loser's bracket final to grand finals
        final var losersFinal = losersBracket.stream()
                .filter(m -> m.getNextMatch() == null)
                .findFirst()
                .orElse(null);

        if (losersFinal != null && !grandFinals.isEmpty()) {
            losersFinal.setNextMatch(grandFinals.get(0));
        }

        // Link losers from winner's bracket to loser's bracket
        linkWinnersBracketLosersToLosersBracket(winnersBracket, losersBracket);
    }

    private void linkWinnersBracketLosersToLosersBracket(
            List<TournamentMatch> winnersBracket, List<TournamentMatch> losersBracket) {

        for (var wbMatch : winnersBracket) {
            final var wbRound = wbMatch.getRoundNumber();
            final var wbMatchNum = wbMatch.getMatchNumber();

            // Calculate which loser's bracket match receives this loser
            final var lbRound = calculateLosersBracketRoundForLoser(wbRound);
            final var lbMatchNum = calculateLosersBracketMatchForLoser(wbRound, wbMatchNum);

            final var loserDestination = losersBracket.stream()
                    .filter(m -> m.getRoundNumber().equals(lbRound)
                            && m.getMatchNumber().equals(lbMatchNum))
                    .findFirst()
                    .orElse(null);

            if (loserDestination != null) {
                wbMatch.setConsolationMatch(loserDestination);
            }
        }
    }

    private int calculateLosersBracketRoundForLoser(int winnersBracketRound) {
        // Losers from WB round N go to LB round 2N-1 (odd rounds)
        if (winnersBracketRound == 1) {
            return 1;
        }
        return 2 * winnersBracketRound - 2;
    }

    private int calculateLosersBracketMatchForLoser(int winnersBracketRound, int winnersBracketMatch) {
        // In early rounds, multiple WB matches may feed into same LB match
        if (winnersBracketRound == 1) {
            return (winnersBracketMatch + 1) / 2;
        }
        return winnersBracketMatch;
    }

    private void assignTeamsToFirstRound(
            List<TournamentMatch> winnersBracket, List<TournamentRegistration> registrations, int participantCount) {

        final var firstRoundMatches = winnersBracket.stream()
                .filter(m -> m.getRoundNumber() == 1)
                .sorted((m1, m2) -> Integer.compare(m1.getMatchNumber(), m2.getMatchNumber()))
                .toList();

        var registrationIndex = 0;
        for (TournamentMatch match : firstRoundMatches) {
            if (registrationIndex < registrations.size()) {
                match.setTeam1(registrations.get(registrationIndex++));
            }

            if (registrationIndex < registrations.size()) {
                match.setTeam2(registrations.get(registrationIndex++));
            }

            match.updateStatus();

            // Handle byes
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

        final var winner = completedMatch.getWinner();
        final var loser = getLoser(completedMatch);

        // Advance winner to next match
        final var nextMatch = completedMatch.getNextMatch();
        if (nextMatch != null) {
            if (nextMatch.getTeam1() == null) {
                nextMatch.setTeam1(winner);
            } else if (nextMatch.getTeam2() == null) {
                nextMatch.setTeam2(winner);
            }
            nextMatch.updateStatus();
            updatedMatches.add(nextMatch);
        }

        // Drop loser to loser's bracket (if this is winner's bracket)
        if (completedMatch.getBracketType() == TournamentMatch.BracketType.MAIN && loser != null) {
            final var loserMatch = completedMatch.getConsolationMatch();
            if (loserMatch != null) {
                if (loserMatch.getTeam1() == null) {
                    loserMatch.setTeam1(loser);
                } else if (loserMatch.getTeam2() == null) {
                    loserMatch.setTeam2(loser);
                }
                loserMatch.updateStatus();
                updatedMatches.add(loserMatch);
            }
        }

        return updatedMatches;
    }

    private @Nullable TournamentRegistration getLoser(TournamentMatch match) {
        final var winner = match.getWinner();
        final var team1 = match.getTeam1();
        final var team2 = match.getTeam2();

        if (winner == null || team1 == null || team2 == null) {
            return null;
        }

        return winner.equals(team1) ? team2 : team1;
    }

    @Override
    public boolean isTournamentComplete(Tournament tournament) {
        final var matches = tournament.getMatches();

        // Find the grand finals (highest round in MAIN bracket)
        final var grandFinals = matches.stream()
                .filter(m -> m.getBracketType() == TournamentMatch.BracketType.MAIN)
                .max((m1, m2) -> Integer.compare(m1.getRoundNumber(), m2.getRoundNumber()))
                .orElse(null);

        return grandFinals != null && grandFinals.isCompleted();
    }

    @Override
    public int getMinimumParticipants() {
        return 3;
    }

    @Override
    public boolean isValidParticipantCount(int participantCount) {
        return participantCount >= getMinimumParticipants();
    }

    @Override
    public Tournament.TournamentType getTournamentType() {
        return Tournament.TournamentType.DOUBLE_ELIMINATION;
    }
}
