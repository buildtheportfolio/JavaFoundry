import java.util.*;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSExport;

/** Browser-facing portfolio entry point. TeaVM compiles this Java class to JS for GitHub Pages. */
public class PortfolioApp {
    private static final List<Student> roster = buildRoster();
    private static String selectedId = "S007";

    public static void main(String[] args) {
        setShell(buildShell());
        renderDashboard();
    }

    @JSExport
    public static void renderDashboard() {
        setPanel(dashboardHtml());
    }

    @JSExport
    public static void renderStudents(String query, String sort) {
        setPanel(studentTableHtml(query, sort));
    }

    @JSExport
    public static void selectStudent(String id) {
        selectedId = id;
        setPanel(studentDetailHtml(id));
    }

    @JSExport
    public static void toggleEnrollment(String id) {
        Student s = find(id);
        if (s != null) s.setEnrolled(!s.isEnrolled());
        selectedId = id;
        setPanel(studentDetailHtml(id));
    }

    @JSExport
    public static void addScore(String id, String subject, double score) {
        Student s = find(id);
        if (s == null) {
            setPanel(alertHtml("Student not found."));
            return;
        }
        try {
            s.addGrade(subject, score);
            selectedId = id;
            setPanel(studentDetailHtml(id));
        } catch (RuntimeException ex) {
            setPanel(alertHtml(ex.getMessage()));
        }
    }

    @JSExport
    public static void renderSubjects() {
        setPanel(subjectHtml());
    }

    @JSExport
    public static void renderRisk() {
        setPanel(riskHtml());
    }

    @JSExport
    public static void exportCsv() {
        downloadCsv(csv());
    }

    @JSExport
    public static String getSelectedId() {
        return selectedId;
    }

    private static List<Student> buildRoster() {
        String[][] people = {
                {"S001", "Alice Chen", "3"}, {"S002", "Bob Patel", "2"},
                {"S003", "Carol Smith", "4"}, {"S004", "David Kim", "1"},
                {"S005", "Eva Rodriguez", "3"}, {"S006", "Frank Johnson", "2"},
                {"S007", "Grace Lee", "4"}, {"S008", "Hiro Tanaka", "1"}
        };
        double[][][] scores = {
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
        List<Student> result = new ArrayList<>();
        for (int i = 0; i < people.length; i++) {
            Student s = new Student(people[i][0], people[i][1], Integer.parseInt(people[i][2]));
            for (int j = 0; j < subjects.length; j++) {
                try { s.addGrade(subjects[j], scores[i][j][0], scores[i][j][1]); } catch (InvalidGradeException ignored) { }
            }
            result.add(s);
        }
        return result;
    }

    private static Student find(String id) {
        for (Student s : roster) if (s.getId().equals(id)) return s;
        return null;
    }

    private static String buildShell() {
        return "<div class='app'><header><div><span class='eyebrow'>JAVAFX → BROWSER</span><h1>Core Academic Portal</h1><p>Java-first student analytics compiled for the web.</p></div><div class='status'>LIVE JAVA LOGIC</div></header>"
                + "<nav><button onclick='renderDashboard()'>Dashboard</button><button onclick='renderStudents(\"\",\"gpa\")'>Students</button><button onclick='renderSubjects()'>Subjects</button><button onclick='renderRisk()'>Risk Monitor</button><button onclick='exportCsv()'>Export CSV</button></nav><main id='panel'></main><footer>Java domain model · TeaVM browser build · GitHub Pages</footer></div>";
    }

    private static String dashboardHtml() {
        double avg = 0, highest = -1, lowest = 101;
        int honors = 0, probation = 0, enrolled = 0;
        for (Student s : roster) {
            double g = s.weightedGPA(); avg += g; highest = Math.max(highest, g); lowest = Math.min(lowest, g);
            if (s.academicStanding() == Student.Standing.HONORS) honors++;
            if (s.academicStanding() == Student.Standing.PROBATION) probation++;
            if (s.isEnrolled()) enrolled++;
        }
        avg /= roster.size();
        return "<section class='hero'><div><span class='eyebrow'>PORTFOLIO DEMO</span><h2>Academic analytics, running from Java in your browser.</h2><p>The same Student model powers rankings, reports, risk checks and live updates. No frontend framework owns the business logic.</p></div><div class='hero-number'><small>CLASS AVERAGE</small><strong>" + f(avg) + "%</strong><span>weighted across credit hours</span></div></section>"
                + stats("Students", roster.size(), "Average", f(avg) + "%", "Honors", honors, "Probation", probation, "Enrolled", enrolled, "Highest", f(highest) + "%")
                + "<section class='grid2'><div class='card'><h3>Top performers</h3>" + rankingRows(4) + "</div><div class='card'><h3>Academic health</h3>" + riskRows(4) + "</div></section>";
    }

    private static String studentTableHtml(String query, String sort) {
        List<Student> rows = new ArrayList<>(roster);
        rows.removeIf(s -> query != null && !query.isEmpty() && !hay(s).contains(query.toLowerCase()));
        if ("name".equals(sort)) rows.sort(Comparator.comparing(Student::getName));
        else if ("year".equals(sort)) rows.sort(Comparator.comparingInt(Student::getYear).thenComparing(Student::getName));
        else rows.sort(Comparator.comparingDouble(Student::weightedGPA).reversed().thenComparing(Student::getName));
        StringBuilder b = new StringBuilder("<section class='card'><div class='toolbar'><input id='q' placeholder='Search students…' value='" + esc(query) + "' oninput='renderStudents(this.value,document.getElementById(\"sort\").value)'><select id='sort' onchange='renderStudents(document.getElementById(\"q\").value,this.value)'><option value='gpa'>GPA ↓</option><option value='name'" + sel(sort,"name") + ">Name</option><option value='year'" + sel(sort,"year") + ">Year</option></select></div><div class='tablewrap'><table><thead><tr><th>#</th><th>Student</th><th>Year</th><th>GPA</th><th>Grade</th><th>Standing</th><th>Credits</th></tr></thead><tbody>");
        int rank = 1;
        for (Student s : rows) {
            b.append("<tr onclick='selectStudent(\"" + s.getId() + "\")'><td>" + rank++ + "</td><td><strong>" + esc(s.getName()) + "</strong><small>" + s.getId() + "</small></td><td>" + s.yearLabel() + "</td><td><b>" + f(s.weightedGPA()) + "%</b><div class='bar'><i style='width:" + f(Math.min(100,s.weightedGPA())) + "%'></i></div></td><td>" + Student.letterGrade(s.weightedGPA()) + "</td><td><span class='pill " + s.academicStanding().name() + "'>" + s.academicStanding() + "</span></td><td>" + f(s.earnedCreditHours()) + " / " + f(s.totalCreditHoursAttempted()) + "</td></tr>");
        }
        if (rows.isEmpty()) b.append("<tr><td colspan='7'>No students match the search.</td></tr>");
        return b.append("</tbody></table></div></section>").toString();
    }

    private static String studentDetailHtml(String id) {
        Student s = find(id);
        if (s == null) return alertHtml("Student not found.");
        StringBuilder b = new StringBuilder("<section class='card'><div class='detailHead'><div><span class='eyebrow'>STUDENT PROFILE</span><h2>" + esc(s.getName()) + "</h2><p>" + s.getId() + " · " + s.yearLabel() + " · " + (s.isEnrolled() ? "Enrolled" : "Inactive") + "</p></div><div><strong class='big'>" + f(s.weightedGPA()) + "%</strong><span>" + Student.letterGrade(s.weightedGPA()) + " · " + s.academicStanding() + "</span></div></div>");
        b.append("<div class='stats mini'>").append(statBox("Credits", f(s.earnedCreditHours()) + " / " + f(s.totalCreditHoursAttempted()))).append(statBox("Best subject", s.strongestSubject().map(x -> x.getName()).orElse("—"))).append(statBox("Needs attention", s.weakestSubject().map(x -> x.getName()).orElse("—"))).append(statBox("Score floor", f(s.lowestSubjectAverage()) + "%")).append("</div>");
        b.append("<div class='subjectGrid'>");
        for (Subject sub : s.getSubjects().values()) if (!sub.getScores().isEmpty()) b.append("<div class='subject'><strong>" + esc(sub.getName()) + "</strong><span>" + f(sub.average()) + "%</span><small>" + Student.letterGrade(sub.average()) + " · " + f(sub.getCreditHours()) + " credits</small><div class='bar'><i style='width:" + f(sub.average()) + "%'></i></div></div>");
        b.append("</div><div class='actions'><button onclick='toggleEnrollment(\"" + id + "\")'>Toggle enrollment</button><select id='subj'>");
        for (String subject : s.getSubjects().keySet()) b.append("<option>" + esc(subject) + "</option>");
        b.append("</select><input id='score' type='number' min='0' max='100' placeholder='score'><button onclick='addScore(\"" + id + "\",document.getElementById(\"subj\").value,Number(document.getElementById(\"score\").value))'>Add score</button></div></section>");
        return b.toString();
    }

    private static String subjectHtml() {
        StringBuilder b = new StringBuilder("<section class='card'><span class='eyebrow'>SUBJECT ANALYTICS</span><h2>Class performance by subject</h2><div class='subjectGrid large'>");
        for (String name : Student.getCreditTable().keySet()) {
            double total=0, high=0, low=101; int count=0, pass=0;
            for (Student s : roster) { Subject sub=s.getSubjects().get(name); if(sub!=null && !sub.getScores().isEmpty()){ double a=sub.average(); total+=a; high=Math.max(high,a); low=Math.min(low,a); count++; if(sub.hasPassing()) pass++; } }
            if(count==0) continue;
            double avg=total/count;
            b.append("<div class='subject'><strong>"+esc(name)+"</strong><span>"+f(avg)+"%</span><small>high "+f(high)+" · low "+f(low)+" · pass "+(pass*100/count)+"%</small><div class='bar'><i style='width:"+f(avg)+"%'></i></div></div>");
        }
        return b.append("</div></section>").toString();
    }

    private static String riskHtml() {
        StringBuilder b = new StringBuilder("<section class='card'><span class='eyebrow'>RISK MONITOR</span><h2>Intervention queue</h2><p class='muted'>Students with low GPA, failing subjects or weak credit completion.</p>");
        for(Student s: roster){ boolean risk=s.academicStanding()==Student.Standing.PROBATION||s.academicStanding()==Student.Standing.WARNING||s.hasFailingSubjects(); if(risk){ b.append("<div class='risk'><div><strong>"+esc(s.getName())+"</strong><small>"+s.getId()+" · "+s.academicStanding()+"</small></div><span>"+f(s.weightedGPA())+"%</span><button onclick='selectStudent(\""+s.getId()+"\")'>Review</button></div>"); } }
        return b.append("</section>").toString();
    }

    private static String rankingRows(int limit) {
        List<Student> list=new ArrayList<>(roster); list.sort(Comparator.comparingDouble(Student::weightedGPA).reversed()); StringBuilder b=new StringBuilder(); for(int i=0;i<Math.min(limit,list.size());i++){Student s=list.get(i); b.append("<div class='row'><span>#"+(i+1)+"</span><strong>"+esc(s.getName())+"</strong><b>"+f(s.weightedGPA())+"%</b></div>");} return b.toString(); }
    private static String riskRows(int limit) { List<Student> list=new ArrayList<>(roster); list.sort(Comparator.comparingDouble(Student::weightedGPA)); StringBuilder b=new StringBuilder(); int n=0; for(Student s:list){ if(s.academicStanding()==Student.Standing.PROBATION||s.hasFailingSubjects()){b.append("<div class='row'><span>⚠</span><strong>"+esc(s.getName())+"</strong><b>"+Student.letterGrade(s.weightedGPA())+"</b></div>"); if(++n==limit) break;}} return n==0?"<p class='muted'>No immediate academic risk flags.</p>":b.toString(); }
    private static String stats(String a,Object av,String b,Object bv,String c,Object cv,String d,Object dv,String e,Object ev,String f,Object fv){return "<section class='stats'>"+statBox(a,String.valueOf(av))+statBox(b,String.valueOf(bv))+statBox(c,String.valueOf(cv))+statBox(d,String.valueOf(dv))+statBox(e,String.valueOf(ev))+statBox(f,String.valueOf(fv))+"</section>";}
    private static String statBox(String label,String value){return "<div class='stat'><small>"+esc(label)+"</small><strong>"+esc(value)+"</strong></div>";}
    private static String alertHtml(String message){return "<section class='card'><div class='alert'>"+esc(message)+"</div><button onclick='renderDashboard()'>Back to dashboard</button></section>";}
    private static String csv(){StringBuilder b=new StringBuilder("id,name,year,gpa,letter,standing,enrolled,creditsAttempted,creditsEarned\n");for(Student s:roster)b.append(s.getId()).append(',').append(csv(s.getName())).append(',').append(s.yearLabel()).append(',').append(f(s.weightedGPA())).append(',').append(Student.letterGrade(s.weightedGPA())).append(',').append(s.academicStanding()).append(',').append(s.isEnrolled()).append(',').append(f(s.totalCreditHoursAttempted())).append(',').append(f(s.earnedCreditHours())).append('\n');return b.toString();}
    private static String csv(String s){return "\""+s.replace("\"","\"\"")+"\"";}
    private static String hay(Student s){return (s.getId()+" "+s.getName()+" "+s.yearLabel()+" "+s.academicStanding()).toLowerCase();}
    private static String sel(String a,String b){return b.equals(a)?" selected":"";}
    private static String esc(String s){return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    private static String f(double d){return String.format(Locale.US,"%.2f",d);}

    @JSBody(params={"html"}, script="document.getElementById('app').innerHTML=html")
    private static native void setShell(String html);
    @JSBody(params={"html"}, script="document.getElementById('panel').innerHTML=html")
    private static native void setPanel(String html);
    @JSBody(params={"content"}, script="const blob=new Blob([content],{type:'text/csv'}); const a=document.createElement('a'); a.href=URL.createObjectURL(blob); a.download='academic-roster.csv'; a.click(); setTimeout(()=>URL.revokeObjectURL(a.href),500)")
    private static native void downloadCsv(String content);
}
