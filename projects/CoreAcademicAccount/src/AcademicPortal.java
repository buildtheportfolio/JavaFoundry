import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Feature-rich Java console application built on top of the existing Student model.
 * The UI stays deliberately minimal: the application behavior, analytics and exports live in Java.
 */
public class AcademicPortal {
    private static final DecimalFormat DF = new DecimalFormat("0.00");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<Student> roster;
    private final Scanner scanner = new Scanner(System.in);

    public AcademicPortal() {
        roster = seedRoster();
    }

    public static void main(String[] args) {
        AcademicPortal app = new AcademicPortal();
        app.run();
    }

    public void run() {
        banner();
        boolean running = true;
        while (running) {
            menu();
            String choice = scanner.nextLine().trim();
            System.out.println();
            switch (choice) {
                case "1" -> dashboard();
                case "2" -> listStudents();
                case "3" -> searchStudents();
                case "4" -> studentReport();
                case "5" -> subjectAnalytics();
                case "6" -> yearAnalytics();
                case "7" -> riskMonitor();
                case "8" -> gradeDistribution();
                case "9" -> recordNewScore();
                case "10" -> toggleEnrollment();
                case "11" -> exportCsv();
                case "12" -> auditRoster();
                case "0" -> running = false;
                default -> System.out.println("Unknown option. Choose 0–12.");
            }
            if (running) pause();
        }
        System.out.println("Session closed at " + LocalDateTime.now().format(TS) + ".");
    }

    private void banner() {
        System.out.println("=".repeat(76));
        System.out.println("CORE ACADEMIC PORTAL — JAVA MANAGEMENT SUITE");
        System.out.println("=".repeat(76));
        System.out.println("Java-first features: search, analytics, risk monitoring, score entry, enrollment, CSV export and audit.");
        System.out.println();
    }

    private void menu() {
        System.out.println("1  Dashboard");
        System.out.println("2  List / rank students");
        System.out.println("3  Search students");
        System.out.println("4  Detailed student report");
        System.out.println("5  Subject analytics");
        System.out.println("6  Year analytics");
        System.out.println("7  Academic risk monitor");
        System.out.println("8  Grade distribution");
        System.out.println("9  Record a new score");
        System.out.println("10 Toggle enrollment status");
        System.out.println("11 Export complete roster to CSV");
        System.out.println("12 Audit roster integrity");
        System.out.println("0  Exit");
        System.out.print("\nSelect: ");
    }

    private void dashboard() {
        List<Student> active = activeStudents();
        double average = active.stream().mapToDouble(Student::weightedGPA).average().orElse(0);
        long honors = active.stream().filter(s -> s.academicStanding() == Student.Standing.HONORS).count();
        long probation = active.stream().filter(s -> s.academicStanding() == Student.Standing.PROBATION).count();
        double credits = active.stream().mapToDouble(Student::earnedCreditHours).sum();

        Optional<Student> top = active.stream().max(Comparator.comparingDouble(Student::weightedGPA));
        Optional<Student> improving = active.stream().max(Comparator.comparingDouble(this::improvementScore));

        System.out.println("DASHBOARD");
        System.out.println("─────────");
        metric("Active students", String.valueOf(active.size()));
        metric("Average weighted GPA", DF.format(average) + "%");
        metric("Honor roll", String.valueOf(honors));
        metric("Probation", String.valueOf(probation));
        metric("Credits earned", DF.format(credits));
        top.ifPresent(s -> metric("Top student", s.getName() + " — " + DF.format(s.weightedGPA()) + "%"));
        improving.ifPresent(s -> metric("Largest recent improvement", s.getName() + " — " + signed(improvementScore(s)) + " pts"));

        System.out.println();
        System.out.println("Standing mix:");
        for (Student.Standing standing : Student.Standing.values()) {
            long count = active.stream().filter(s -> s.academicStanding() == standing).count();
            System.out.printf("%-13s %s %d%n", standing, bar(count, active.size(), 28), count);
        }
    }

    private void listStudents() {
        System.out.println("SORTING");
        System.out.println("1 GPA descending");
        System.out.println("2 Name ascending");
        System.out.println("3 Year ascending, GPA descending");
        System.out.println("4 Strongest subject average descending");
        System.out.print("Sort: ");
        String choice = scanner.nextLine().trim();

        Comparator<Student> comparator = switch (choice) {
            case "2" -> Comparator.comparing(Student::getName, String.CASE_INSENSITIVE_ORDER);
            case "3" -> Comparator.comparingInt(Student::getYear)
                    .thenComparing(Comparator.comparingDouble(Student::weightedGPA).reversed());
            case "4" -> Comparator.comparingDouble(Student::highestSubjectAverage).reversed();
            default -> Comparator.comparingDouble(Student::weightedGPA).reversed();
        };

        List<Student> sorted = activeStudents().stream().sorted(comparator).toList();
        System.out.printf("\n%-4s %-6s %-22s %-12s %-9s %-12s %-11s%n", "#", "ID", "Name", "Year", "GPA", "Letter", "Standing");
        System.out.println("-".repeat(82));
        for (int i = 0; i < sorted.size(); i++) {
            Student s = sorted.get(i);
            System.out.printf("%-4d %-6s %-22s %-12s %-9s %-12s %-11s%n",
                    i + 1, s.getId(), clip(s.getName(), 22), s.yearLabel(),
                    DF.format(s.weightedGPA()), Student.letterGrade(s.weightedGPA()), s.academicStanding());
        }
    }

    private void searchStudents() {
        System.out.print("Search text (ID, name, year, standing): ");
        String query = scanner.nextLine().trim().toLowerCase();
        if (query.isBlank()) {
            System.out.println("Search cancelled.");
            return;
        }

        List<Student> matches = roster.stream()
                .filter(s -> {
                    String haystack = (s.getId() + " " + s.getName() + " " + s.yearLabel() + " " + s.academicStanding()).toLowerCase();
                    return haystack.contains(query);
                })
                .sorted(Comparator.comparingDouble(Student::weightedGPA).reversed())
                .toList();

        System.out.println("Matches: " + matches.size());
        matches.forEach(s -> System.out.printf("%s | %s | %s | %.2f%% | %s%n",
                s.getId(), s.getName(), s.yearLabel(), s.weightedGPA(), s.academicStanding()));
    }

    private void studentReport() {
        System.out.print("Student ID: ");
        String id = scanner.nextLine().trim();
        Optional<Student> found = roster.stream().filter(s -> s.getId().equalsIgnoreCase(id)).findFirst();
        if (found.isEmpty()) {
            System.out.println("Student not found.");
            return;
        }

        Student s = found.get();
        System.out.println();
        s.printReport();
        System.out.println("Additional analytics");
        metric("Enrollment", s.isEnrolled() ? "Active" : "Inactive");
        metric("Credits attempted", DF.format(s.totalCreditHoursAttempted()));
        metric("Credits earned", DF.format(s.earnedCreditHours()));
        metric("Completion", DF.format(percentage(s.earnedCreditHours(), s.totalCreditHoursAttempted())) + "%");
        metric("Strongest subject", s.strongestSubject().map(Subject::getName).orElse("N/A"));
        metric("Weakest subject", s.weakestSubject().map(Subject::getName).orElse("N/A"));
        metric("Score consistency", DF.format(scoreStandardDeviation(s)));
        metric("Trend", signed(improvementScore(s)) + " pts vs first/latest assessment");

        List<String> warnings = riskReasons(s);
        if (!warnings.isEmpty()) {
            System.out.println("Risk flags: " + String.join("; ", warnings));
        }
    }

    private void subjectAnalytics() {
        System.out.println("SUBJECT ANALYTICS");
        System.out.printf("%-18s %-9s %-9s %-9s %-9s %-8s%n", "Subject", "Class Avg", "High", "Low", "Spread", "Pass %");
        System.out.println("-".repeat(70));

        for (String subject : Student.getCreditTable().keySet()) {
            List<Subject> subjects = activeStudents().stream()
                    .map(s -> s.getSubjects().get(subject))
                    .filter(java.util.Objects::nonNull)
                    .filter(s -> !s.getScores().isEmpty())
                    .toList();
            if (subjects.isEmpty()) continue;

            double avg = subjects.stream().mapToDouble(Subject::average).average().orElse(0);
            double high = subjects.stream().mapToDouble(Subject::highest).max().orElse(0);
            double low = subjects.stream().mapToDouble(Subject::lowest).min().orElse(0);
            double spread = subjects.stream().mapToDouble(Subject::average).max().orElse(0)
                    - subjects.stream().mapToDouble(Subject::average).min().orElse(0);
            double pass = 100.0 * subjects.stream().filter(Subject::hasPassing).count() / subjects.size();

            System.out.printf("%-18s %8s%% %8s%% %8s%% %8s %7s%%%n",
                    clip(subject, 18), DF.format(avg), DF.format(high), DF.format(low), DF.format(spread), DF.format(pass));
        }

        System.out.println();
        String toughest = Student.getCreditTable().keySet().stream()
                .min(Comparator.comparingDouble(this::subjectAverage))
                .orElse("N/A");
        String strongest = Student.getCreditTable().keySet().stream()
                .max(Comparator.comparingDouble(this::subjectAverage))
                .orElse("N/A");
        metric("Lowest class average", toughest + " — " + DF.format(subjectAverage(toughest)) + "%");
        metric("Highest class average", strongest + " — " + DF.format(subjectAverage(strongest)) + "%");
    }

    private void yearAnalytics() {
        Map<Integer, List<Student>> byYear = activeStudents().stream()
                .collect(Collectors.groupingBy(Student::getYear, LinkedHashMap::new, Collectors.toList()));

        System.out.printf("%-10s %-8s %-12s %-12s %-12s%n", "Year", "Count", "Avg GPA", "Top GPA", "Credits Earned");
        System.out.println("-".repeat(60));
        for (Map.Entry<Integer, List<Student>> entry : byYear.entrySet()) {
            List<Student> students = entry.getValue();
            double avg = students.stream().mapToDouble(Student::weightedGPA).average().orElse(0);
            double top = students.stream().mapToDouble(Student::weightedGPA).max().orElse(0);
            double credits = students.stream().mapToDouble(Student::earnedCreditHours).sum();
            System.out.printf("%-10s %-8d %-12s %-12s %-12s%n",
                    yearLabel(entry.getKey()), students.size(), DF.format(avg), DF.format(top), DF.format(credits));
        }
    }

    private void riskMonitor() {
        List<Student> risks = roster.stream()
                .filter(Student::isEnrolled)
                .filter(s -> !riskReasons(s).isEmpty())
                .sorted(Comparator.comparingDouble(Student::weightedGPA))
                .toList();

        System.out.println("ACADEMIC RISK MONITOR");
        System.out.println("---------------------");
        if (risks.isEmpty()) {
            System.out.println("No students currently meet the risk thresholds.");
            return;
        }
        for (Student s : risks) {
            System.out.printf("%s (%s) GPA=%s%% → %s%n",
                    s.getName(), s.getId(), DF.format(s.weightedGPA()), String.join(", ", riskReasons(s)));
        }
        System.out.println();
        System.out.println("Thresholds: GPA < 55, failing subject, or earned-credit completion < 70%.");
    }

    private void gradeDistribution() {
        Map<String, Long> distribution = activeStudents().stream()
                .collect(Collectors.groupingBy(s -> bucket(Student.letterGrade(s.weightedGPA())), LinkedHashMap::new, Collectors.counting()));
        System.out.println("GPA / LETTER DISTRIBUTION");
        distribution.forEach((bucket, count) -> System.out.printf("%-10s %s %d%n", bucket, "█".repeat((int) (count * 3)), count));
    }

    private void recordNewScore() {
        System.out.print("Student ID: ");
        String id = scanner.nextLine().trim();
        Student student = roster.stream().filter(s -> s.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
        if (student == null) {
            System.out.println("Student not found.");
            return;
        }
        System.out.print("Subject: ");
        String subject = scanner.nextLine().trim();
        if (!student.getSubjects().containsKey(subject)) {
            System.out.println("Unknown subject. Valid subjects: " + String.join(", ", Student.getCreditTable().keySet()));
            return;
        }
        System.out.print("Score (0-100): ");
        String raw = scanner.nextLine().trim();
        try {
            double score = Double.parseDouble(raw);
            student.addGrade(subject, score);
            System.out.println("Recorded " + DF.format(score) + " for " + student.getName() + " in " + subject + ".");
            System.out.println("New weighted GPA: " + DF.format(student.weightedGPA()) + "%");
        } catch (NumberFormatException e) {
            System.out.println("Score must be numeric.");
        } catch (InvalidGradeException e) {
            System.out.println("Rejected: " + e.getMessage());
        }
    }

    private void toggleEnrollment() {
        System.out.print("Student ID: ");
        String id = scanner.nextLine().trim();
        Student student = roster.stream().filter(s -> s.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
        if (student == null) {
            System.out.println("Student not found.");
            return;
        }
        student.setEnrolled(!student.isEnrolled());
        System.out.println(student.getName() + " is now " + (student.isEnrolled() ? "ENROLLED" : "INACTIVE") + ".");
    }

    private void exportCsv() {
        Path output = Path.of("student_roster_export.csv");
        List<String> lines = new ArrayList<>();
        lines.add("id,name,year,enrolled,weighted_gpa,gpa_4_scale,letter,standing,credits_attempted,credits_earned,strongest_subject,weakest_subject,risk_flags");
        for (Student s : roster) {
            lines.add(String.join(",",
                    csv(s.getId()), csv(s.getName()), String.valueOf(s.getYear()), String.valueOf(s.isEnrolled()),
                    DF.format(s.weightedGPA()), DF.format(s.gpaOn4Scale()), csv(Student.letterGrade(s.weightedGPA())),
                    csv(s.academicStanding().name()), DF.format(s.totalCreditHoursAttempted()), DF.format(s.earnedCreditHours()),
                    csv(s.strongestSubject().map(Subject::getName).orElse("")),
                    csv(s.weakestSubject().map(Subject::getName).orElse("")), csv(String.join(" | ", riskReasons(s)))));
        }
        try {
            Files.write(output, lines);
            System.out.println("Exported " + lines.size() + " rows to " + output.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    private void auditRoster() {
        System.out.println("ROSTER INTEGRITY AUDIT");
        System.out.println("----------------------");
        List<String> errors = new ArrayList<>();

        Map<String, Long> duplicateIds = roster.stream()
                .collect(Collectors.groupingBy(Student::getId, Collectors.counting()));
        duplicateIds.forEach((id, count) -> {
            if (count > 1) errors.add("Duplicate student ID: " + id);
        });

        roster.forEach(s -> {
            if (s.getName().isBlank()) errors.add("Blank name: " + s.getId());
            if (s.getSubjects().isEmpty()) errors.add("No subjects: " + s.getId());
            s.getSubjects().forEach((name, subject) -> {
                if (subject.getCreditHours() <= 0) errors.add("Invalid credits: " + s.getId() + "/" + name);
                subject.getScores().forEach(score -> {
                    if (score < 0 || score > 100) errors.add("Invalid score: " + s.getId() + "/" + name + ": " + score);
                });
            });
        });

        if (errors.isEmpty()) {
            System.out.println("PASS — no structural issues found across " + roster.size() + " students.");
        } else {
            System.out.println("FAIL — " + errors.size() + " issues:");
            errors.forEach(e -> System.out.println("• " + e));
        }
    }

    private List<Student> activeStudents() {
        return roster.stream().filter(Student::isEnrolled).toList();
    }

    private double subjectAverage(String subject) {
        return activeStudents().stream()
                .map(s -> s.getSubjects().get(subject))
                .filter(java.util.Objects::nonNull)
                .filter(s -> !s.getScores().isEmpty())
                .mapToDouble(Subject::average)
                .average().orElse(0);
    }

    private double scoreStandardDeviation(Student student) {
        List<Double> values = student.getSubjects().values().stream()
                .flatMap(s -> s.getScores().stream())
                .toList();
        if (values.size() < 2) return 0;
        double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return Math.sqrt(values.stream().mapToDouble(v -> Math.pow(v - avg, 2)).average().orElse(0));
    }

    private double improvementScore(Student student) {
        List<Double> values = student.getSubjects().values().stream()
                .flatMap(s -> s.getScores().stream())
                .toList();
        if (values.size() < 2) return 0;
        return values.get(values.size() - 1) - values.get(0);
    }

    private List<String> riskReasons(Student s) {
        List<String> reasons = new ArrayList<>();
        if (s.weightedGPA() < 55) reasons.add("low GPA");
        if (s.hasFailingSubjects()) reasons.add("failing subject");
        if (s.totalCreditHoursAttempted() > 0 && percentage(s.earnedCreditHours(), s.totalCreditHoursAttempted()) < 70) {
            reasons.add("low credit completion");
        }
        return reasons;
    }

    private static double percentage(double numerator, double denominator) {
        return denominator == 0 ? 0 : numerator * 100.0 / denominator;
    }

    private static String bucket(String letter) {
        if (letter.startsWith("A")) return "A range";
        if (letter.startsWith("B")) return "B range";
        if (letter.startsWith("C")) return "C range";
        if (letter.equals("D")) return "D";
        return "F";
    }

    private static String bar(long count, long total, int width) {
        if (total == 0) return "";
        int filled = (int) Math.round((count * width) / (double) total);
        return "█".repeat(Math.max(0, filled));
    }

    private static String signed(double value) {
        return (value >= 0 ? "+" : "") + DF.format(value);
    }

    private static String yearLabel(int year) {
        return switch (year) {
            case 1 -> "Freshman";
            case 2 -> "Sophomore";
            case 3 -> "Junior";
            case 4 -> "Senior";
            default -> "Year " + year;
        };
    }

    private static String clip(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private static String csv(String value) {
        if (value == null) return "";
        String safe = value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private static void metric(String label, String value) {
        System.out.printf("%-28s %s%n", label + ":", value);
    }

    private void pause() {
        System.out.print("\nPress Enter to return to the menu...");
        scanner.nextLine();
        System.out.println();
    }

    private static List<Student> seedRoster() {
        String[][] students = {
                {"S001", "Alice Chen", "3"}, {"S002", "Bob Patel", "2"},
                {"S003", "Carol Smith", "4"}, {"S004", "David Kim", "1"},
                {"S005", "Eva Rodriguez", "3"}, {"S006", "Frank Johnson", "2"},
                {"S007", "Grace Lee", "4"}, {"S008", "Hiro Tanaka", "1"}
        };
        double[][][] grades = {
                {{4,88},{4,92},{3,79},{3,85},{4,95},{2,70},{3,82},{3,88},{3,91},{2,76}},
                {{4,72},{4,68},{3,75},{3,80},{4,70},{2,65},{3,71},{3,74},{3,69},{2,77}},
                {{4,55},{4,60},{3,58},{3,91},{4,62},{2,94},{3,61},{3,70},{3,65},{2,95}},
                {{4,45},{4,82},{3,90},{3,78},{4,88},{2,55},{3,0},{3,72},{3,84},{2,68}},
                {{4,83},{4,87},{3,80},{3,88},{4,85},{2,79},{3,84},{3,86},{3,82},{2,81}},
                {{4,38},{4,42},{3,35},{3,50},{4,40},{2,55},{3,45},{3,38},{3,41},{2,48}},
                {{4,96},{4,94},{3,91},{3,98},{4,99},{2,89},{3,93},{3,95},{3,97},{2,92}},
                {{4,76},{4,80},{3,74},{3,77},{4,83},{2,69},{3,78},{3,75},{3,81},{2,72}}
        };
        String[] subjects = {"Mathematics","Physics","Chemistry","English","Computer Sci","History","Biology","Economics","Statistics","Literature"};

        List<Student> roster = new ArrayList<>();
        for (int i = 0; i < students.length; i++) {
            Student s = new Student(students[i][0], students[i][1], Integer.parseInt(students[i][2]));
            for (int j = 0; j < subjects.length; j++) {
                s.addGrade(subjects[j], grades[i][j][0], grades[i][j][1]);
            }
            roster.add(s);
        }
        return roster;
    }
}
