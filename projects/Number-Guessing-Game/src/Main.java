import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;


public class Main {

    static final String RST = "\033[0m";
    static final String GRN = "\033[92m";
    static final String CYN = "\033[96m";
    static final String YLW = "\033[93m";
    static final String RED = "\033[91m";
    static final String DIM = "\033[2m";
    static final String BLD = "\033[1m";
    static final String MAG = "\033[95m";

    enum Difficulty {
        EASY   (1, 50,  10, "Easy   — 1..50,   10 guesses"),
        MEDIUM (1, 100, 7,  "Medium — 1..100,   7 guesses"),
        HARD   (1, 200, 6,  "Hard   — 1..200,   6 guesses"),
        INSANE (1, 500, 8,  "Insane — 1..500,   8 guesses"),
        CUSTOM (0, 0,   0,  "Custom — you pick the range & limit");

        final int    defaultMin;
        final int    defaultMax;
        final int    maxGuesses;
        final String label;

        Difficulty(int min, int max, int guesses, String label) {
            this.defaultMin = min;
            this.defaultMax = max;
            this.maxGuesses = guesses;
            this.label      = label;
        }
    }

    enum HintStrength { NONE, BASIC, RICH, PERCENTAGE }

    static class GuessRecord {
        final int    guess;
        final int    target;
        final String hint;
        final long   elapsedMs;
        final int    guessNumber;

        GuessRecord(int guess, int target, String hint, long elapsedMs, int guessNumber) {
            this.guess       = guess;
            this.target      = target;
            this.hint        = hint;
            this.elapsedMs   = elapsedMs;
            this.guessNumber = guessNumber;
        }

        @Override
        public String toString() {
            return String.format("  Guess #%d: %d → %s  (%dms)", guessNumber, guess, hint, elapsedMs);
        }
    }

    static class RoundResult {
        final boolean  won;
        final int      target;
        final int      guessCount;
        final long     totalMs;
        final int      rangeMin;
        final int      rangeMax;
        final int      maxGuesses;
        final String   difficulty;
        final List<GuessRecord> guesses;

        RoundResult(boolean won, int target, int guessCount, long totalMs,
                    int rangeMin, int rangeMax, int maxGuesses,
                    String difficulty, List<GuessRecord> guesses) {
            this.won        = won;
            this.target     = target;
            this.guessCount = guessCount;
            this.totalMs    = totalMs;
            this.rangeMin   = rangeMin;
            this.rangeMax   = rangeMax;
            this.maxGuesses = maxGuesses;
            this.difficulty = difficulty;
            this.guesses    = Collections.unmodifiableList(new ArrayList<>(guesses));
        }

        double efficiency() {
            int optimalGuesses = (int) Math.ceil(Math.log(rangeMax - rangeMin + 1) / Math.log(2));
            return optimalGuesses == 0 ? 100.0 : Math.min(100.0, (double) optimalGuesses / guessCount * 100.0);
        }

        String rating() {
            if (!won) return "DNF";
            double eff = efficiency();
            if (eff >= 100) return "PERFECT";
            if (eff >=  85) return "GREAT";
            if (eff >=  70) return "GOOD";
            if (eff >=  55) return "OK";
            return "POOR";
        }
    }

    static class SessionStats {
        int              totalRounds     = 0;
        int              wins            = 0;
        int              losses          = 0;
        int              bestScore       = Integer.MAX_VALUE;
        long             bestTimeMs      = Long.MAX_VALUE;
        long             totalMs         = 0;
        int              totalGuesses    = 0;
        int              currentStreak   = 0;
        int              bestStreak      = 0;
        final List<RoundResult> history  = new ArrayList<>();
        final Map<String, Integer> diffWins = new HashMap<>();

        void record(RoundResult r) {
            totalRounds++;
            history.add(r);
            totalMs      += r.totalMs;
            totalGuesses += r.guessCount;

            if (r.won) {
                wins++;
                currentStreak++;
                bestStreak  = Math.max(bestStreak, currentStreak);
                bestScore   = Math.min(bestScore, r.guessCount);
                bestTimeMs  = Math.min(bestTimeMs, r.totalMs);
                diffWins.merge(r.difficulty, 1, Integer::sum);
            } else {
                losses++;
                currentStreak = 0;
            }
        }

        double winRate()    { return totalRounds == 0 ? 0 : (double) wins / totalRounds * 100; }
        double avgGuesses() { return totalRounds == 0 ? 0 : (double) totalGuesses / totalRounds; }
        double avgTimeMs()  { return totalRounds == 0 ? 0 : (double) totalMs / totalRounds; }

        void print() {
            if (totalRounds == 0) { System.out.println(DIM + "  No rounds played." + RST); return; }

            System.out.println();
            System.out.println(BLD + CYN + "  ╔══════════════════════ SESSION SUMMARY ══════════════════╗" + RST);
            System.out.printf( BLD + CYN + "  ║" + RST + "  Rounds: %-5d  Wins: %-5d  Losses: %-5d  Win rate: %5.1f%%" + BLD + CYN + "  ║%n" + RST,
                totalRounds, wins, losses, winRate());
            System.out.printf( BLD + CYN + "  ║" + RST + "  Best score (fewest guesses): %-4s  Best time: %-10s" + BLD + CYN + "  ║%n" + RST,
                bestScore == Integer.MAX_VALUE ? "—" : bestScore,
                bestTimeMs == Long.MAX_VALUE   ? "—" : bestTimeMs + "ms");
            System.out.printf( BLD + CYN + "  ║" + RST + "  Avg guesses/round: %-6.2f  Avg time/round: %-8.0fms" + BLD + CYN + "  ║%n" + RST,
                avgGuesses(), avgTimeMs());
            System.out.printf( BLD + CYN + "  ║" + RST + "  Best streak: %-4d  Current streak: %-4d             " + BLD + CYN + "  ║%n" + RST,
                bestStreak, currentStreak);
            System.out.println(BLD + CYN + "  ╚══════════════════════════════════════════════════════════╝" + RST);

            if (!history.isEmpty()) {
                System.out.println(DIM + "\n  Round history:" + RST);
                System.out.printf(DIM + "  %-5s %-10s %-8s %-8s %-10s %-8s%n" + RST,
                    "#", "Result", "Guesses", "Target", "Time(ms)", "Rating");
                System.out.println(DIM + "  " + "─".repeat(54) + RST);
                for (int i = 0; i < history.size(); i++) {
                    RoundResult r = history.get(i);
                    String col = r.won ? GRN : RED;
                    System.out.printf("  %-5d " + col + "%-10s" + RST + " %-8d %-8d %-10d %-8s%n",
                        i + 1, r.won ? "WIN" : "LOSS", r.guessCount, r.target, r.totalMs, r.rating());
                }
            }

            IntSummaryStatistics guessStat = history.stream()
                .filter(r -> r.won)
                .mapToInt(r -> r.guessCount)
                .summaryStatistics();

            if (wins > 0) {
                System.out.printf(DIM + "%n  Winning round stats — min: %d  max: %d  avg: %.1f%n" + RST,
                    guessStat.getMin(), guessStat.getMax(), guessStat.getAverage());
            }

            System.out.println();
        }
    }

    static int    rangeMin   = 1;
    static int    rangeMax   = 100;
    static int    maxGuesses = 7;
    static String diffLabel  = "Medium";

    static HintStrength hintMode    = HintStrength.RICH;
    static boolean      timerOn     = true;
    static boolean      showHistory = false;

    static final Random  rng     = new Random();
    static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        printBanner();
        SessionStats session = new SessionStats();

        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim().toLowerCase();
            switch (choice) {
                case "1", "p", "play" -> playRound(session);
                case "2", "d", "diff" -> configureDifficulty();
                case "3", "h", "hint" -> cycleHintMode();
                case "4", "t", "time" -> { timerOn = !timerOn; ok("Timer " + (timerOn ? "ON" : "OFF")); }
                case "5", "s", "stat" -> session.print();
                case "6", "r", "hist" -> { showHistory = !showHistory; ok("Round history display " + (showHistory ? "ON" : "OFF")); }
                case "7", "q", "quit" -> { running = false; }
                default -> warn("Unknown option '" + choice + "'. Type 1–7.");
            }
        }

        System.out.println(DIM + "\n  Final stats before exit:" + RST);
        session.print();
        System.out.println(GRN + BLD + "\n  Thanks for playing. Goodbye!\n" + RST);
    }

    static void playRound(SessionStats session) {
        int secret      = rng.nextInt(rangeMax - rangeMin + 1) + rangeMin;
        int guessCount  = 0;
        int guessesLeft = maxGuesses;
        long roundStart = System.currentTimeMillis();
        List<GuessRecord> guessLog = new ArrayList<>();
        List<Integer>     seen     = new ArrayList<>();

        System.out.println();
        System.out.println(BLD + "  ┌─────────────────────────────────────────────────────┐");
        System.out.printf (BLD + "  │  🎮 NEW ROUND  ·  Range: %d–%d  ·  Max guesses: %d%n",
            rangeMin, rangeMax, maxGuesses);
        System.out.printf (BLD + "  │  Difficulty: %-10s  Hints: %-10s  Timer: %s%n",
            diffLabel, hintMode, timerOn ? "ON" : "OFF");
        System.out.println(BLD + "  └─────────────────────────────────────────────────────┘" + RST);
        int optimalGuesses = (int) Math.ceil(Math.log(rangeMax - rangeMin + 1) / Math.log(2));
        info(String.format("  Optimal binary-search needs %d guesses for this range.", optimalGuesses));

        boolean won  = false;
        boolean quit = false;

        while (guessesLeft > 0 && !won && !quit) {
            System.out.printf("%n" + CYN + "  [%d/%d] Guess: " + RST, guessCount + 1, maxGuesses);
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("q") || input.equals("quit")) {
                quit = true;
                break;
            }
            if (input.equals("hint") || input.equals("?")) {
                printBinaryHint(secret, seen, rangeMin, rangeMax);
                continue;
            }

            int guess;
            try {
                guess = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                warn("  Enter a number between " + rangeMin + " and " + rangeMax + " (or 'q' to quit).");
                continue;
            }

            if (guess < rangeMin || guess > rangeMax) {
                warn(String.format("  Out of range! Guess must be %d–%d.", rangeMin, rangeMax));
                continue;
            }

            if (seen.contains(guess)) {
                warn("  You already guessed " + guess + "! Try something new.");
                continue;
            }

            long guessTime = System.currentTimeMillis() - roundStart;
            guessCount++;
            guessesLeft--;
            seen.add(guess);

            String hint = buildHint(guess, secret, seen, rangeMin, rangeMax);
            guessLog.add(new GuessRecord(guess, secret, hint, guessTime, guessCount));

            if (guess == secret) {
                won = true;
                long elapsed = System.currentTimeMillis() - roundStart;
                System.out.println();
                System.out.println(GRN + BLD + "  ✓ CORRECT! The number was " + secret + "." + RST);
                if (timerOn) info(String.format("  Time: %dms", elapsed));
                printGuessBar(guessCount, maxGuesses);
                RoundResult result = new RoundResult(true, secret, guessCount, elapsed,
                    rangeMin, rangeMax, maxGuesses, diffLabel, guessLog);
                System.out.printf(MAG + "  Rating: %-8s  Efficiency: %.1f%%%n" + RST,
                    result.rating(), result.efficiency());
                session.record(result);
                printLiveStats(session);
            } else {
                System.out.println("  " + hint);
                if (guessesLeft == 1) System.out.println(YLW + "  ⚠ Last guess!" + RST);
                if (guessesLeft == 0) {
                    long elapsed = System.currentTimeMillis() - roundStart;
                    System.out.println();
                    System.out.println(RED + BLD + "  ✗ GAME OVER — the number was " + secret + "." + RST);
                    session.record(new RoundResult(false, secret, guessCount, elapsed,
                        rangeMin, rangeMax, maxGuesses, diffLabel, guessLog));
                }
            }
        }

        if (quit) {
            long elapsed = System.currentTimeMillis() - roundStart;
            warn("  Round abandoned. The number was " + secret + ".");
            session.record(new RoundResult(false, secret, guessCount, elapsed,
                rangeMin, rangeMax, maxGuesses, diffLabel, guessLog));
        }

        if (showHistory && !guessLog.isEmpty()) {
            System.out.println(DIM + "\n  This round's guesses:");
            guessLog.forEach(g -> System.out.println(DIM + g + RST));
        }
    }

    static String buildHint(int guess, int secret, List<Integer> seen, int min, int max) {
        int  diff    = Math.abs(guess - secret);
        int  range   = max - min;
        String dir   = guess < secret ? "↑ Higher" : "↓ Lower";

        return switch (hintMode) {
            case NONE -> dir;
            case BASIC -> {
                String warmth = diff <= range / 10 ? "🔥 Very hot!" :
                                diff <= range / 4  ? "♨ Hot"       :
                                diff <= range / 2  ? "~ Warm"      : "❄ Cold";
                yield dir + "  |  " + warmth;
            }
            case RICH -> {
                String warmth = diff == 0          ? "✓ Correct!"  :
                                diff <= range / 10 ? "🔥 Scorching" :
                                diff <= range / 5  ? "♨ Hot"        :
                                diff <= range / 3  ? "~ Warm"       :
                                diff <= range / 2  ? "Cool"         : "❄ Ice cold";
                List<Integer> sortedSeen = new ArrayList<>(seen);
                Collections.sort(sortedSeen);
                int lo = min, hi = max;
                for (int s : sortedSeen) {
                    if (s < secret && s > lo) lo = s;
                    if (s > secret && s < hi) hi = s;
                }
                yield String.format("%s  |  %s  |  narrowed: [%d–%d]", dir, warmth, lo, hi);
            }
            case PERCENTAGE -> {
                int pct = (int)(100.0 - (double) diff / range * 100.0);
                String warmth = pct >= 95 ? "🔥 Scorching" :
                                pct >= 80 ? "♨ Hot"        :
                                pct >= 60 ? "~ Warm"       :
                                pct >= 40 ? "Cool"         : "❄ Cold";
                yield String.format("%s  |  %d%% close  |  %s", dir, pct, warmth);
            }
        };
    }

    static void printBinaryHint(int secret, List<Integer> seen, int min, int max) {
        List<Integer> sorted = new ArrayList<>(seen);
        Collections.sort(sorted);
        int lo = min, hi = max;
        for (int s : sorted) {
            if (s < secret) lo = Math.max(lo, s + 1);
            if (s > secret) hi = Math.min(hi, s - 1);
        }
        int mid = (lo + hi) / 2;
        info(String.format("  💡 Binary hint: remaining range [%d–%d], try around %d", lo, hi, mid));
    }

    static void printGuessBar(int used, int max) {
        int filled = used;
        int empty  = max - used;
        String bar = GRN + "█".repeat(filled) + RST + DIM + "░".repeat(empty) + RST;
        System.out.printf("  Guesses used: [%s]  %d / %d%n", bar, used, max);
    }

    static void printLiveStats(SessionStats s) {
        System.out.printf(DIM + "  Session: %dW / %dL  |  streak: %d  |  best: %s  |  win rate: %.0f%%%n" + RST,
            s.wins, s.losses, s.currentStreak,
            s.bestScore == Integer.MAX_VALUE ? "—" : String.valueOf(s.bestScore),
            s.winRate());
    }

    static void configureDifficulty() {
        System.out.println();
        System.out.println(BLD + "  Select difficulty:" + RST);
        Difficulty[] vals = Difficulty.values();
        for (int i = 0; i < vals.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, vals[i].label);
        }
        System.out.print(CYN + "  Choice: " + RST);
        String in = scanner.nextLine().trim();
        int choice;
        try { choice = Integer.parseInt(in) - 1; }
        catch (NumberFormatException e) { warn("Invalid choice."); return; }

        if (choice < 0 || choice >= vals.length) { warn("Out of range."); return; }
        Difficulty d = vals[choice];

        if (d == Difficulty.CUSTOM) {
            System.out.print("  Min value: ");
            int customMin = readInt(1);
            System.out.print("  Max value: ");
            int customMax = readInt(customMin + 1);
            System.out.print("  Max guesses: ");
            int customGuesses = readInt(1);
            rangeMin   = customMin;
            rangeMax   = customMax;
            maxGuesses = customGuesses;
            diffLabel  = "Custom";
        } else {
            rangeMin   = d.defaultMin;
            rangeMax   = d.defaultMax;
            maxGuesses = d.maxGuesses;
            diffLabel  = d.name().substring(0, 1) + d.name().substring(1).toLowerCase();
        }
        ok(String.format("Difficulty set: range %d–%d, max %d guesses.", rangeMin, rangeMax, maxGuesses));
    }

    static void cycleHintMode() {
        HintStrength[] modes = HintStrength.values();
        hintMode = modes[(hintMode.ordinal() + 1) % modes.length];
        ok("Hint mode → " + hintMode);
        switch (hintMode) {
            case NONE       -> info("  No hints beyond higher/lower.");
            case BASIC      -> info("  Adds temperature hint (hot/warm/cold).");
            case RICH       -> info("  Adds temperature + narrowed range after each guess.");
            case PERCENTAGE -> info("  Shows how close you are as a percentage.");
        }
    }

    static int readInt(int minimum) {
        while (true) {
            try {
                int v = Integer.parseInt(scanner.nextLine().trim());
                if (v >= minimum) return v;
                System.out.print("  Must be ≥ " + minimum + ": ");
            } catch (NumberFormatException e) {
                System.out.print("  Enter a number: ");
            }
        }
    }

    static void printBanner() {
        System.out.println();
        System.out.println(GRN + BLD + "  ████████████████████████████████████████████" + RST);
        System.out.println(GRN + BLD + "  ██  NUMBER GUESSING GAME  ·  Java 120  ██" + RST);
        System.out.println(GRN + BLD + "  ████████████████████████████████████████████" + RST);
        System.out.println(DIM + "  Higher/lower hints · streaks · efficiency rating" + RST);
        System.out.println();
    }

    static void printMenu() {
        System.out.println();
        System.out.println(DIM + "  ┌──────── MENU ──────────────────────────────────────┐" + RST);
        System.out.printf (DIM + "  │" + RST + "  1. Play         current: %s (%d–%d, %d guesses)" + DIM + "%n│" + RST,
            diffLabel, rangeMin, rangeMax, maxGuesses);
        System.out.printf (DIM + "  │" + RST + "  2. Difficulty   3. Hints [%s]   4. Timer [%s]%n",
            hintMode, timerOn ? "ON" : "OFF");
        System.out.println(DIM + "  │" + RST + "  5. Stats        6. Toggle history   7. Quit");
        System.out.println(DIM + "  └────────────────────────────────────────────────────┘" + RST);
        System.out.print(CYN + "  > " + RST);
    }

    static void ok(String msg)   { System.out.println(GRN + "  ✓ " + msg + RST); }
    static void info(String msg) { System.out.println(DIM + msg + RST); }
    static void warn(String msg) { System.out.println(YLW + "  ⚠ " + msg + RST); }
}