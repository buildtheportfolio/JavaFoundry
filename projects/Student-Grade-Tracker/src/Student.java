import java.util.*;
import java.util.stream.*;

class InvalidGradeException extends RuntimeException {
    private final String subject;
    private final double grade;

    InvalidGradeException(String subject, double grade) {
        super(String.format(
                "Invalid grade %.2f for subject '%s'. Grades must be in range 0.0–100.0.",
                grade, subject));
        this.subject = subject;
        this.grade = grade;
    }

    String getSubject() {
        return subject;
    }

    double getGrade() {
        return grade;
    }
}

class Subject {
    private final String name;
    private final double creditHours;
    private final List<Double> scores;

    Subject(String name, double creditHours) {
        if (creditHours <= 0)
            throw new IllegalArgumentException("Credit hours must be positive for: " + name);
        this.name = name;
        this.creditHours = creditHours;
        this.scores = new ArrayList<>();
    }

    void addScore(double score) {
        if (score < 0 || score > 100)
            throw new InvalidGradeException(name, score);
        scores.add(score);
    }

    String getName() {
        return name;
    }

    double getCreditHours() {
        return creditHours;
    }

    List<Double> getScores() {
        return Collections.unmodifiableList(scores);
    }

    double average() {
        if (scores.isEmpty())
            return 0.0;
        return scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    double highest() {
        return scores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    double lowest() {
        return scores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
    }

    boolean hasPassing() {
        return average() >= 40.0;
    }

    @Override
    public String toString() {
        return String.format("Subject{name='%s', credits=%.1f, avg=%.2f, scores=%s}",
                name, creditHours, average(), scores);
    }
}

public class Student {

    enum Standing {
        HONORS, GOOD, SATISFACTORY, WARNING, PROBATION
    }

    private final String id;
    private final String name;
    private final int year;
    private final Map<String, Subject> subjects;
    private boolean enrolled;

    private static final Map<String, Double> CREDIT_TABLE = new LinkedHashMap<>();
    static {
        CREDIT_TABLE.put("Mathematics", 4.0);
        CREDIT_TABLE.put("Physics", 4.0);
        CREDIT_TABLE.put("Chemistry", 3.0);
        CREDIT_TABLE.put("English", 3.0);
        CREDIT_TABLE.put("Computer Sci", 4.0);
        CREDIT_TABLE.put("History", 2.0);
        CREDIT_TABLE.put("Biology", 3.0);
        CREDIT_TABLE.put("Economics", 3.0);
        CREDIT_TABLE.put("Statistics", 3.0);
        CREDIT_TABLE.put("Literature", 2.0);
    }

    Student(String id, String name, int year) {
        this.id = id;
        this.name = name;
        this.year = year;
        this.subjects = new LinkedHashMap<>();
        this.enrolled = true;
    }

    static Map<String, Double> getCreditTable() {
        return Collections.unmodifiableMap(CREDIT_TABLE);
    }

    void addSubject(String subjectName, double creditHours) {
        subjects.putIfAbsent(subjectName, new Subject(subjectName, creditHours));
    }

    void addGrade(String subjectName, double score) {
        Subject sub = subjects.get(subjectName);
        if (sub == null)
            throw new IllegalArgumentException("Subject not found: " + subjectName);
        sub.addScore(score);
    }

    void addGrade(String subjectName, double creditHours, double score) {
        subjects.putIfAbsent(subjectName, new Subject(subjectName, creditHours));
        subjects.get(subjectName).addScore(score);
    }

    double weightedGPA() {
        double totalPoints = 0.0;
        double totalCredits = 0.0;
        for (Subject s : subjects.values()) {
            if (s.getScores().isEmpty())
                continue;
            totalPoints += s.average() * s.getCreditHours();
            totalCredits += s.getCreditHours();
        }
        return totalCredits == 0 ? 0.0 : totalPoints / totalCredits;
    }

    double gpaOn4Scale() {
        return percentToGPA(weightedGPA());
    }

    static double percentToGPA(double pct) {
        if (pct >= 90)
            return 4.0;
        if (pct >= 80)
            return 3.0 + (pct - 80) / 10.0;
        if (pct >= 70)
            return 2.0 + (pct - 70) / 10.0;
        if (pct >= 60)
            return 1.0 + (pct - 60) / 10.0;
        return Math.max(0.0, pct / 60.0);
    }

    static String letterGrade(double pct) {
        if (pct >= 90)
            return "A+";
        if (pct >= 85)
            return "A";
        if (pct >= 80)
            return "A-";
        if (pct >= 75)
            return "B+";
        if (pct >= 70)
            return "B";
        if (pct >= 65)
            return "B-";
        if (pct >= 60)
            return "C+";
        if (pct >= 55)
            return "C";
        if (pct >= 50)
            return "C-";
        if (pct >= 45)
            return "D";
        return "F";
    }

    Standing academicStanding() {
        double gpa = weightedGPA();
        if (gpa >= 85)
            return Standing.HONORS;
        if (gpa >= 70)
            return Standing.GOOD;
        if (gpa >= 55)
            return Standing.SATISFACTORY;
        if (gpa >= 40)
            return Standing.WARNING;
        return Standing.PROBATION;
    }

    boolean hasFailingSubjects() {
        return subjects.values().stream().anyMatch(s -> !s.getScores().isEmpty() && !s.hasPassing());
    }

    double highestSubjectAverage() {
        return subjects.values().stream()
                .filter(s -> !s.getScores().isEmpty())
                .mapToDouble(Subject::average)
                .max().orElse(0.0);
    }

    double lowestSubjectAverage() {
        return subjects.values().stream()
                .filter(s -> !s.getScores().isEmpty())
                .mapToDouble(Subject::average)
                .min().orElse(0.0);
    }

    Optional<Subject> strongestSubject() {
        return subjects.values().stream()
                .filter(s -> !s.getScores().isEmpty())
                .max(Comparator.comparingDouble(Subject::average));
    }

    Optional<Subject> weakestSubject() {
        return subjects.values().stream()
                .filter(s -> !s.getScores().isEmpty())
                .min(Comparator.comparingDouble(Subject::average));
    }

    double totalCreditHoursAttempted() {
        return subjects.values().stream()
                .filter(s -> !s.getScores().isEmpty())
                .mapToDouble(Subject::getCreditHours)
                .sum();
    }

    double earnedCreditHours() {
        return subjects.values().stream()
                .filter(s -> !s.getScores().isEmpty() && s.hasPassing())
                .mapToDouble(Subject::getCreditHours)
                .sum();
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    int getYear() {
        return year;
    }

    boolean isEnrolled() {
        return enrolled;
    }

    void setEnrolled(boolean e) {
        enrolled = e;
    }

    Map<String, Subject> getSubjects() {
        return Collections.unmodifiableMap(subjects);
    }

    String yearLabel() {
        return switch (year) {
            case 1 -> "Freshman";
            case 2 -> "Sophomore";
            case 3 -> "Junior";
            case 4 -> "Senior";
            default -> "Year " + year;
        };
    }

    void printReport() {
        System.out.printf("%n  ┌─────────────────────────────────────────────────────┐%n");
        System.out.printf("  │  %-20s  %-10s  ID: %s%n", name, yearLabel(), id);
        System.out.printf("  │  Weighted GPA: %5.2f%%  (%.2f/4.0)  [%s]  %s%n",
                weightedGPA(), gpaOn4Scale(), letterGrade(weightedGPA()), academicStanding());
        System.out.printf("  │  Credits attempted: %.1f   earned: %.1f%n",
                totalCreditHoursAttempted(), earnedCreditHours());
        System.out.printf("  ├──────────────────┬──────────┬──────────┬─────┬───────%n");
        System.out.printf("  │ %-16s │ %8s │ %8s │ %3s │ %5s%n",
                "Subject", "Avg", "Credits", "Grd", "Hi/Lo");
        System.out.printf("  ├──────────────────┼──────────┼──────────┼─────┼───────%n");
        for (Subject s : subjects.values()) {
            if (s.getScores().isEmpty())
                continue;
            System.out.printf("  │ %-16s │ %7.2f%% │ %7.1f  │ %-3s │ %.0f/%.0f%n",
                    s.getName(), s.average(), s.getCreditHours(),
                    letterGrade(s.average()), s.highest(), s.lowest());
        }
        System.out.printf("  └──────────────────┴──────────┴──────────┴─────┴───────%n");
        if (hasFailingSubjects()) {
            System.out.println("  ⚠  WARNING: One or more subjects below passing threshold (40%).");
        }
        System.out.println();
    }

    @Override
    public String toString() {
        return String.format("Student{id='%s', name='%s', year=%d, gpa=%.2f, standing=%s}",
                id, name, year, weightedGPA(), academicStanding());
    }
}