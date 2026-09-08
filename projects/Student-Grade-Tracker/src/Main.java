import java.util.*;
import java.util.stream.*;

public class Main {

    static final String RST = "\033[0m";
    static final String GRN = "\033[92m";
    static final String CYN = "\033[96m";
    static final String YLW = "\033[93m";
    static final String RED = "\033[91m";
    static final String DIM = "\033[2m";
    static final String BLD = "\033[1m";
    static final String MAG = "\033[95m";

    private final List<Student> roster = new ArrayList<>();

    public static void main(String[] args) {
        Main app = new Main();

        section("1. Building the Roster");
        app.buildRoster();

        section("2. Individual Student Reports");
        app.printStudentReports();

        section("3. Class Ranking — sorted by weighted GPA desc, name asc");
        app.printRanking();

        section("4. Class Statistics");
        app.printClassStats();

        section("5. Subject Leaderboard — top student per subject");
        app.printSubjectLeaderboard();

        section("6. Honor Roll & Probation List");
        app.printHonorAndProbation();

        section("7. Weighted GPA vs Simple Average — why weighting matters");
        app.printWeightingComparison();

        section("8. Edge Cases");
        app.runEdgeCases();
    }

    void buildRoster() {
        String[][] studentData = {
                { "S001", "Alice Chen", "3" },
                { "S002", "Bob Patel", "2" },
                { "S003", "Carol Smith", "4" },
                { "S004", "David Kim", "1" },
                { "S005", "Eva Rodriguez", "3" },
                { "S006", "Frank Johnson", "2" },
                { "S007", "Grace Lee", "4" },
                { "S008", "Hiro Tanaka", "1" },
        };

        double[][][] gradeData = {
                // Alice: strong STEM
                { { 4, 88 }, { 4, 92 }, { 3, 79 }, { 3, 85 }, { 4, 95 }, { 2, 70 }, { 3, 82 }, { 3, 88 }, { 3, 91 },
                        { 2, 76 } },
                // Bob: balanced
                { { 4, 72 }, { 4, 68 }, { 3, 75 }, { 3, 80 }, { 4, 70 }, { 2, 65 }, { 3, 71 }, { 3, 74 }, { 3, 69 },
                        { 2, 77 } },
                // Carol: arts/humanities lean
                { { 4, 55 }, { 4, 60 }, { 3, 58 }, { 3, 91 }, { 4, 62 }, { 2, 94 }, { 3, 61 }, { 3, 70 }, { 3, 65 },
                        { 2, 95 } },
                // David: freshman, mixed
                { { 4, 45 }, { 4, 82 }, { 3, 90 }, { 3, 78 }, { 4, 88 }, { 2, 55 }, { 3, 0 }, { 3, 72 }, { 3, 84 },
                        { 2, 68 } },
                // Eva: near-honors
                { { 4, 83 }, { 4, 87 }, { 3, 80 }, { 3, 88 }, { 4, 85 }, { 2, 79 }, { 3, 84 }, { 3, 86 }, { 3, 82 },
                        { 2, 81 } },
                // Frank: struggling
                { { 4, 38 }, { 4, 42 }, { 3, 35 }, { 3, 50 }, { 4, 40 }, { 2, 55 }, { 3, 45 }, { 3, 38 }, { 3, 41 },
                        { 2, 48 } },
                // Grace: top student
                { { 4, 96 }, { 4, 94 }, { 3, 91 }, { 3, 98 }, { 4, 99 }, { 2, 89 }, { 3, 93 }, { 3, 95 }, { 3, 97 },
                        { 2, 92 } },
                // Hiro: freshman, above average
                { { 4, 76 }, { 4, 80 }, { 3, 74 }, { 3, 77 }, { 4, 83 }, { 2, 69 }, { 3, 78 }, { 3, 75 }, { 3, 81 },
                        { 2, 72 } },
        };

        String[] subjects = {
                "Mathematics", "Physics", "Chemistry", "English", "Computer Sci",
                "History", "Biology", "Economics", "Statistics", "Literature"
        };

        for (int i = 0; i < studentData.length; i++) {
            String[] sd = studentData[i];
            Student s = new Student(sd[0], sd[1], Integer.parseInt(sd[2]));
            for (int j = 0; j < subjects.length; j++) {
                try {
                    s.addGrade(subjects[j], gradeData[i][j][0], gradeData[i][j][1]);
                } catch (InvalidGradeException e) {
                    warn("Grade rejected — " + e.getMessage());
                }
            }
            roster.add(s);
            ok("Added: " + s);
        }
    }

    void printStudentReports() {
        for (Student s : roster) {
            s.printReport();
        }
    }

    void printRanking() {
        List<Student> ranked = roster.stream()
                .sorted(Comparator
                        .comparingDouble(Student::weightedGPA).reversed()
                        .thenComparing(Student::getName))
                .collect(Collectors.toList());

        System.out.println();
        System.out.printf("  %-4s %-6s %-20s %-12s %-8s %-10s %-10s %-16s%n",
                "Rank", "ID", "Name", "Year", "GPA %", "GPA/4.0", "Letter", "Standing");
        System.out.println("  " + "─".repeat(90));

        for (int i = 0; i < ranked.size(); i++) {
            Student s = ranked.get(i);
            String medal = i == 0 ? " 🥇" : i == 1 ? " 🥈" : i == 2 ? " 🥉" : "";
            String color = s.academicStanding() == Student.Standing.HONORS ? GRN
                    : s.academicStanding() == Student.Standing.PROBATION ? RED
                            : RST;
            System.out.printf(color + "  #%-3d %-6s %-20s %-12s %7.2f%%  %6.2f   %-6s   %-12s%s%n" + RST,
                    i + 1, s.getId(), s.getName(), s.yearLabel(),
                    s.weightedGPA(), s.gpaOn4Scale(),
                    Student.letterGrade(s.weightedGPA()),
                    s.academicStanding(),
                    medal);
        }
        System.out.println();
    }

    void printClassStats() {
        DoubleSummaryStatistics stats = roster.stream()
                .mapToDouble(Student::weightedGPA)
                .summaryStatistics();

        double[] gpas = roster.stream().mapToDouble(Student::weightedGPA).sorted().toArray();
        double median = gpas.length % 2 == 0
                ? (gpas[gpas.length / 2 - 1] + gpas[gpas.length / 2]) / 2.0
                : gpas[gpas.length / 2];

        double variance = roster.stream()
                .mapToDouble(s -> Math.pow(s.weightedGPA() - stats.getAverage(), 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        System.out.println();
        info(String.format("  Class size     : %d students", roster.size()));
        info(String.format("  Highest GPA    : %.2f%%  (%s)",
                stats.getMax(),
                roster.stream().max(Comparator.comparingDouble(Student::weightedGPA))
                        .map(Student::getName).orElse("?")));
        info(String.format("  Lowest GPA     : %.2f%%  (%s)",
                stats.getMin(),
                roster.stream().min(Comparator.comparingDouble(Student::weightedGPA))
                        .map(Student::getName).orElse("?")));
        info(String.format("  Class average  : %.2f%%", stats.getAverage()));
        info(String.format("  Median GPA     : %.2f%%", median));
        info(String.format("  Std deviation  : %.2f", stdDev));

        System.out.println();
        Map<Student.Standing, Long> dist = roster.stream()
                .collect(Collectors.groupingBy(Student::academicStanding, Collectors.counting()));
        for (Student.Standing st : Student.Standing.values()) {
            long count = dist.getOrDefault(st, 0L);
            String bar = "█".repeat((int) (count * 4));
            System.out.printf("  %-14s │ %s %d%n", st, bar, count);
        }
        System.out.println();
    }

    void printSubjectLeaderboard() {
        Map<String, Double> creditTable = Student.getCreditTable();
        System.out.println();
        System.out.printf("  %-16s %-22s %-10s %-8s%n",
                "Subject", "Top Student", "Score", "Credits");
        System.out.println("  " + "─".repeat(60));

        for (String subj : creditTable.keySet()) {
            roster.stream()
                    .filter(s -> s.getSubjects().containsKey(subj)
                            && !s.getSubjects().get(subj).getScores().isEmpty())
                    .max(Comparator.comparingDouble(s -> s.getSubjects().get(subj).average()))
                    .ifPresent(top -> {
                        double avg = top.getSubjects().get(subj).average();
                        double cr = top.getSubjects().get(subj).getCreditHours();
                        System.out.printf("  %-16s %-22s %8.2f%%  %.1f%n",
                                subj, top.getName(), avg, cr);
                    });
        }
        System.out.println();
    }

    void printHonorAndProbation() {
        List<Student> honors = roster.stream()
                .filter(s -> s.academicStanding() == Student.Standing.HONORS)
                .sorted(Comparator.comparingDouble(Student::weightedGPA).reversed())
                .collect(Collectors.toList());

        List<Student> probation = roster.stream()
                .filter(s -> s.academicStanding() == Student.Standing.PROBATION)
                .collect(Collectors.toList());

        System.out.println();
        System.out.println(GRN + BLD + "  🏆 Honor Roll" + RST);
        if (honors.isEmpty()) {
            System.out.println(DIM + "  (none)" + RST);
        } else {
            honors.forEach(s -> System.out.printf(GRN + "  ✓  %-20s %.2f%%  (%s)%n" + RST,
                    s.getName(), s.weightedGPA(), Student.letterGrade(s.weightedGPA())));
        }

        System.out.println();
        System.out.println(RED + BLD + "  ⚠  Academic Probation" + RST);
        if (probation.isEmpty()) {
            System.out.println(DIM + "  (none)" + RST);
        } else {
            probation.forEach(s -> System.out.printf(RED + "  ✗  %-20s %.2f%%  (%s)%n" + RST,
                    s.getName(), s.weightedGPA(), Student.letterGrade(s.weightedGPA())));
        }

        List<Student> failing = roster.stream()
                .filter(Student::hasFailingSubjects)
                .collect(Collectors.toList());

        System.out.println();
        System.out.println(YLW + BLD + "  ⚑  Students with at least one failing subject" + RST);
        if (failing.isEmpty()) {
            System.out.println(DIM + "  (none)" + RST);
        } else {
            for (Student s : failing) {
                String failedSubjects = s.getSubjects().entrySet().stream()
                        .filter(e -> !e.getValue().getScores().isEmpty() && !e.getValue().hasPassing())
                        .map(Map.Entry::getKey)
                        .collect(Collectors.joining(", "));
                System.out.printf(YLW + "  ⚠  %-20s → failing: %s%n" + RST,
                        s.getName(), failedSubjects);
            }
        }
        System.out.println();
    }

    void printWeightingComparison() {
        System.out.println();
        System.out.printf("  %-20s %14s %14s %10s%n",
                "Student", "Simple Avg", "Weighted GPA", "Δ Diff");
        System.out.println("  " + "─".repeat(62));

        for (Student s : roster) {
            OptionalDouble simpleAvg = s.getSubjects().values().stream()
                    .filter(sub -> !sub.getScores().isEmpty())
                    .mapToDouble(sub -> sub.average())
                    .average();

            if (simpleAvg.isEmpty())
                continue;
            double simple = simpleAvg.getAsDouble();
            double weighted = s.weightedGPA();
            double diff = weighted - simple;
            String marker = Math.abs(diff) > 1.0 ? (diff > 0 ? " ↑" : " ↓") : "  ";

            System.out.printf("  %-20s %13.2f%% %13.2f%%  %+8.2f%s%n",
                    s.getName(), simple, weighted, diff, marker);
        }
        System.out.println();
        System.out.println(DIM + "  ↑ weighted > simple: high-credit subjects above their overall avg" + RST);
        System.out.println(DIM + "  ↓ weighted < simple: high-credit subjects dragging the average down" + RST);
        System.out.println();
    }

    void runEdgeCases() {
        System.out.println();

        System.out.println(DIM + "  Testing InvalidGradeException (score > 100):" + RST);
        Student test = new Student("T001", "Test Student", 1);
        try {
            test.addGrade("Mathematics", 4.0, 105.0);
        } catch (InvalidGradeException e) {
            warn("Caught InvalidGradeException — " + e.getMessage());
            warn(String.format("  subject='%s'  attempted=%.1f", e.getSubject(), e.getGrade()));
        }

        System.out.println(DIM + "%n  Testing negative grade:" + RST);
        try {
            test.addGrade("Physics", 4.0, -5.0);
        } catch (InvalidGradeException e) {
            warn("Caught InvalidGradeException — " + e.getMessage());
        }

        System.out.println(DIM + "  Testing unknown subject lookup:" + RST);
        try {
            test.addGrade("Astrology", 99.0);
        } catch (IllegalArgumentException e) {
            warn("Caught IllegalArgumentException — " + e.getMessage());
        }

        System.out.println(DIM + "  Testing negative credit hours:" + RST);
        try {
            test.addGrade("Yoga", -1.0, 80.0);
        } catch (IllegalArgumentException e) {
            warn("Caught IllegalArgumentException — " + e.getMessage());
        }

        System.out.println(DIM + "  Testing student with no grades:" + RST);
        Student empty = new Student("T002", "No Grades Yet", 1);
        info(String.format("  weightedGPA=%.2f  gpaOn4=%.2f  standing=%s",
                empty.weightedGPA(), empty.gpaOn4Scale(), empty.academicStanding()));

        System.out.println(DIM + "  Testing exact boundary grades (0, 40, 55, 100):" + RST);
        for (double g : new double[] { 0.0, 40.0, 55.0, 70.0, 100.0 }) {
            System.out.printf("  %.1f  → letter='%s'  gpa4=%.2f%n",
                    g, Student.letterGrade(g), Student.percentToGPA(g));
        }
        System.out.println();
    }

    static void section(String title) {
        System.out.println();
        System.out.println(BLD + CYN + "── " + title + " " + "─".repeat(Math.max(0, 64 - title.length())) + RST);
    }

    static void ok(String msg) {
        System.out.println(GRN + "  ✓ " + msg + RST);
    }

    static void info(String msg) {
        System.out.println(DIM + msg + RST);
    }

    static void warn(String msg) {
        System.out.println(YLW + "  ⚠ " + msg + RST);
    }
}