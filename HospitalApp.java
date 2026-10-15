import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * SMART HOSPITAL — educational server-side MVC application.
 * No external dependencies: requires JDK 17+.
 * MODEL: HospitalModel, Patient, Appointment, Bill, TreatmentNote and BillingStrategy.
 * VIEW: HospitalView constructs HTML pages.
 * CONTROLLER: HospitalController interprets HTTP requests and calls Model + View.
 */
public class HospitalApp {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HospitalModel model = new HospitalModel(Path.of("hospital-data.ser"));
        HospitalView view = new HospitalView();
        HospitalController controller = new HospitalController(model, view);
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", controller::handle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Smart Hospital MVC running on http://localhost:" + port);
        System.out.println("Press Ctrl+C to stop. Data saved in hospital-data.ser");
    }
}

// ================================ MODEL ================================
class Patient implements Serializable {
    private static final long serialVersionUID = 1L;
    int id; String name, category, phone; int age;
    Patient(int id, String name, int age, String category, String phone) {
        this.id=id; this.name=name; this.age=age; this.category=category; this.phone=phone;
    }
}
class Appointment implements Serializable {
    private static final long serialVersionUID = 1L;
    int id, patientId; String date, doctor, reason;
    Appointment(int id, int patientId, String date, String doctor, String reason) {
        this.id=id; this.patientId=patientId; this.date=date; this.doctor=doctor; this.reason=reason;
    }
}
class Bill implements Serializable {
    private static final long serialVersionUID = 1L;
    int id, patientId; double base, total; String strategy;
    Bill(int id, int patientId, double base, double total, String strategy) {
        this.id=id; this.patientId=patientId; this.base=base; this.total=total; this.strategy=strategy;
    }
}
class TreatmentNote implements Serializable {
    private static final long serialVersionUID = 1L;
    int id, patientId; String diagnosis, medicines, instructions;
    TreatmentNote(int id, int patientId, String diagnosis, String medicines, String instructions) {
        this.id=id; this.patientId=patientId; this.diagnosis=diagnosis;
        this.medicines=medicines; this.instructions=instructions;
    }
}

// Practical 7 Strategy: choose calculation without changing the Billing module.
interface BillingStrategy {
    double calculate(double amount);
    String name();
}
class SelfPayStrategy implements BillingStrategy {
    public double calculate(double amount) { return amount; }
    public String name() { return "Self-Pay"; }
}
class InsuranceStrategy implements BillingStrategy {
    public double calculate(double amount) { return amount * 0.30; } // classroom illustration: 70% covered
    public String name() { return "Insurance"; }
}
class CorporateStrategy implements BillingStrategy {
    public double calculate(double amount) { return amount * 0.75; }
    public String name() { return "Corporate"; }
}
class SeniorCitizenStrategy implements BillingStrategy {
    public double calculate(double amount) { return amount * 0.70; }
    public String name() { return "Senior Citizen"; }
}
class BillingService {
    BillingStrategy choose(String key) {
        return switch (key) {
            case "Insurance" -> new InsuranceStrategy();
            case "Corporate" -> new CorporateStrategy();
            case "Senior Citizen" -> new SeniorCitizenStrategy();
            default -> new SelfPayStrategy();
        };
    }
}

class HospitalModel {
    static class State implements Serializable {
        private static final long serialVersionUID = 1L;
        int nextPatient=1, nextAppointment=1, nextBill=1, nextNote=1;
        Map<Integer, Patient> patients = new LinkedHashMap<>();
        Map<Integer, Appointment> appointments = new LinkedHashMap<>();
        Map<Integer, Bill> bills = new LinkedHashMap<>();
        Map<Integer, TreatmentNote> notes = new LinkedHashMap<>();
    }
    private final Path path;
    private State state;
    private final BillingService billingService = new BillingService();
    HospitalModel(Path path) {
        this.path=path; state=load();
    }
    private State load() {
        if (Files.exists(path)) {
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
                return (State) in.readObject();
            } catch (Exception e) { System.err.println("Could not read data: " + e.getMessage()); }
        }
        return new State();
    }
    private void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(path))) {
            out.writeObject(state);
        } catch (IOException e) { throw new IllegalStateException("Failed to save data: " + e.getMessage(), e); }
    }
    synchronized Collection<Patient> patients() { return List.copyOf(state.patients.values()); }
    synchronized Collection<Appointment> appointments() { return List.copyOf(state.appointments.values()); }
    synchronized Collection<Bill> bills() { return List.copyOf(state.bills.values()); }
    synchronized Collection<TreatmentNote> notes() { return List.copyOf(state.notes.values()); }
    synchronized Patient getPatient(int id) { return state.patients.get(id); }
    synchronized Patient addPatient(String name, int age, String category, String phone) {
        if (name.isBlank() || age<=0 || age>130) throw new IllegalArgumentException("Enter a valid patient name and age.");
        Patient p = new Patient(state.nextPatient++, name, age, category, phone);
        state.patients.put(p.id,p); save(); return p;
    }
    synchronized void deletePatient(int id) {
        if (state.patients.remove(id)==null) throw new IllegalArgumentException("Patient not found.");
        state.appointments.values().removeIf(a->a.patientId==id);
        state.bills.values().removeIf(b->b.patientId==id);
        state.notes.values().removeIf(n->n.patientId==id);
        save();
    }
    synchronized void updatePatient(int id,String name,int age,String category,String phone) {
        Patient p=requirePatient(id);
        if(name.isBlank() || age<=0 || age>130) throw new IllegalArgumentException("Enter a valid patient name and age.");
        p.name=name; p.age=age; p.category=category; p.phone=phone; save();
    }
    private Patient requirePatient(int id) {
        Patient p=state.patients.get(id);
        if(p==null) throw new IllegalArgumentException("Select an existing patient.");
        return p;
    }
    synchronized void addAppointment(int patientId,String date,String doctor,String reason) {
        requirePatient(patientId);
        if(date.isBlank() || doctor.isBlank()) throw new IllegalArgumentException("Date and doctor are required.");
        Appointment a=new Appointment(state.nextAppointment++,patientId,date,doctor,reason);
        state.appointments.put(a.id,a); save();
    }
    synchronized void deleteAppointment(int id) { state.appointments.remove(id); save(); }
    synchronized Bill createBill(int patientId,double base,String policy) {
        requirePatient(patientId);
        if(base<0 || !Double.isFinite(base)) throw new IllegalArgumentException("Bill amount must be non-negative.");
        BillingStrategy strategy=billingService.choose(policy);
        Bill b=new Bill(state.nextBill++,patientId,base,strategy.calculate(base),strategy.name());
        state.bills.put(b.id,b); save(); return b;
    }
    synchronized void addNote(int patientId,String diagnosis,String medicines,String instructions) {
        requirePatient(patientId);
        if(diagnosis.isBlank()) throw new IllegalArgumentException("Diagnosis is required.");
        TreatmentNote n=new TreatmentNote(state.nextNote++,patientId,diagnosis,medicines,instructions);
        state.notes.put(n.id,n); save();
    }
    synchronized void deleteNote(int id) { state.notes.remove(id); save(); }
    synchronized int countPatients() { return state.patients.size(); }
    synchronized int countAppointments() { return state.appointments.size(); }
    synchronized int countBills() { return state.bills.size(); }
    synchronized int countNotes() { return state.notes.size(); }
}

// ================================ CONTROLLER ================================
class HospitalController {
    private final HospitalModel model;
    private final HospitalView view;
    HospitalController(HospitalModel model, HospitalView view) { this.model=model; this.view=view; }

    void handle(HttpExchange ex) throws IOException {
        try {
            String path=ex.getRequestURI().getPath();
            String method=ex.getRequestMethod();
            if(path.equals("/favicon.ico")) { ex.sendResponseHeaders(204,-1); return; }
            if(method.equals("POST")) {
                Map<String,String> form=parse(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                try {
                    switch(path) {
                        case "/patients/add" -> model.addPatient(value(form,"name"), number(form,"age"), value(form,"category"), value(form,"phone"));
                        case "/patients/update" -> model.updatePatient(number(form,"id"),value(form,"name"),number(form,"age"),value(form,"category"),value(form,"phone"));
                        case "/patients/delete" -> model.deletePatient(number(form,"id"));
                        case "/appointments/add" -> model.addAppointment(number(form,"patientId"),value(form,"date"),value(form,"doctor"),value(form,"reason"));
                        case "/appointments/delete" -> model.deleteAppointment(number(form,"id"));
                        case "/billing/add" -> model.createBill(number(form,"patientId"),Double.parseDouble(value(form,"base")),value(form,"policy"));
                        case "/notes/add" -> model.addNote(number(form,"patientId"),value(form,"diagnosis"),value(form,"medicines"),value(form,"instructions"));
                        case "/notes/delete" -> model.deleteNote(number(form,"id"));
                        default -> { send(ex,404,"Not found"); return; }
                    }
                    String target=switch(path.split("/")[1]) {
                        case "patients" -> "/patients";
                        case "appointments" -> "/appointments";
                        case "billing" -> "/billing";
                        default -> "/notes";
                    };
                    ex.getResponseHeaders().set("Location", target+"?ok=1");
                    ex.sendResponseHeaders(303,-1);
                } catch (Exception e) {
                    send(ex,400,view.layout("Error", "<div class='notice error'>"+HospitalView.escape(e.getMessage())+"</div><a href='javascript:history.back()'>Go back</a>",""));
                }
                return;
            }
            if(!method.equals("GET")) { send(ex,405,"Method not allowed"); return; }
            String html=switch(path) {
                case "/" -> view.dashboard(model);
                case "/patients" -> view.patients(model);
                case "/appointments" -> view.appointments(model);
                case "/billing" -> view.billing(model);
                case "/notes" -> view.notes(model);
                case "/mvc" -> view.mvc();
                default -> view.layout("Not found","<p>Page not found.</p>","");
            };
            send(ex,200,html);
        } catch(Exception e) {
            send(ex,500,"Server error: "+HospitalView.escape(e.getMessage()));
        } finally { ex.close(); }
    }
    private String value(Map<String,String> form,String name) { return form.getOrDefault(name,"").trim(); }
    private int number(Map<String,String> form,String name) { return Integer.parseInt(value(form,name)); }
    static Map<String,String> parse(String raw) {
        Map<String,String> result=new HashMap<>();
        for(String pair:raw.split("&")) {
            int idx=pair.indexOf('=');
            if(idx<0) continue;
            result.put(URLDecoder.decode(pair.substring(0,idx),StandardCharsets.UTF_8),
                URLDecoder.decode(pair.substring(idx+1),StandardCharsets.UTF_8));
        }
        return result;
    }
    private void send(HttpExchange ex,int code,String body) throws IOException {
        byte[] data=body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type","text/html; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control","no-store");
        ex.sendResponseHeaders(code,data.length);
        try(OutputStream os=ex.getResponseBody()) { os.write(data); }
    }
}

// ================================ VIEW ================================
class HospitalView {
    static String escape(String s) {
        if(s==null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
    }
    String layout(String title,String content,String active) {
        return """
        <!doctype html><html lang="en"><head><meta charset="utf-8"/><meta name="viewport" content="width=device-width,initial-scale=1"/>
        <title>%s | Smart Hospital</title><style>
        :root{color-scheme:light;font-family:Inter,system-ui,-apple-system,Segoe UI,sans-serif;background:#f5f7fb;color:#15243b}
        *{box-sizing:border-box}body{margin:0}header{background:#111e38;color:#fff;padding:22px 4vw;display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap}
        header strong{font-size:22px}header span{font-size:13px;color:#bbcee9}
        nav{display:flex;gap:8px;flex-wrap:wrap;background:#fff;padding:13px 4vw;border-bottom:1px solid #e1e7f0}
        nav a{color:#334966;text-decoration:none;padding:9px 13px;border-radius:9px;font-weight:600;font-size:14px}
        nav a:hover,nav a.active{background:#eaf1ff;color:#164db4}
        main{max-width:1200px;margin:32px auto;padding:0 22px 60px}
        h1{font-size:30px;margin:0 0 8px}h2{font-size:19px;margin:0 0 18px}.subtitle{color:#63758e;margin-bottom:26px}
        .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:20px}
        .panel{background:#fff;border:1px solid #e2e9f2;border-radius:15px;padding:24px;margin-bottom:22px;box-shadow:0 2px 12px #0f284d09}
        .stat b{display:block;color:#174eb7;font-size:36px;margin:12px 0}.stat span{color:#607289}
        form{display:flex;gap:13px;align-items:end;flex-wrap:wrap}
        label{display:flex;flex-direction:column;gap:6px;font-weight:600;font-size:13px;color:#384d67;flex:1;min-width:150px}
        input,select,textarea{padding:11px 12px;border:1px solid #cfd9e7;border-radius:9px;background:#fff;color:#1e3048;font:inherit;width:100%%;font-size:14px}
        textarea{min-height:90px}button,.btn{border:0;border-radius:9px;background:#1c57ca;color:white;padding:12px 16px;font-size:14px;font-weight:700;cursor:pointer;text-decoration:none}
        button:hover,.btn:hover{background:#1443a1}.danger{background:#fff0ef;color:#b73526;padding:8px 10px}.danger:hover{background:#ffe0de}
        .tablewrap{overflow-x:auto}table{border-collapse:collapse;width:100%%;font-size:14px}th,td{text-align:left;padding:12px 10px;border-bottom:1px solid #e9edf3;vertical-align:top}th{color:#60748c;background:#f7f9fc}
        .muted{color:#6b7b90}.notice{padding:15px 19px;border-radius:10px;background:#e7f6ed;color:#167647;margin-bottom:18px}.error{background:#ffeded;color:#a42725}
        .tag{display:inline-block;background:#e7efff;padding:4px 8px;border-radius:7px;color:#174fa8;font-weight:700;font-size:12px}
        .flow{display:flex;gap:12px;align-items:center;flex-wrap:wrap}.flow .node{background:#e7efff;color:#174caa;border-radius:12px;padding:18px;font-weight:800}
        .tip{border-left:4px solid #527de1;padding:10px 16px;background:#f1f5ff;color:#3a526e}
        </style></head><body>
        <header><div><strong>✚ Smart Hospital</strong><br/><span>Java Web Application · MVC Architecture</span></div><span>Educational project · No external libraries</span></header>
        <nav>
        <a class="%s" href="/">Dashboard</a><a class="%s" href="/patients">Patients</a>
        <a class="%s" href="/appointments">Appointments</a><a class="%s" href="/billing">Billing</a>
        <a class="%s" href="/notes">Treatment Records</a><a class="%s" href="/mvc">How MVC Works</a>
        </nav><main>%s</main></body></html>
        """.formatted(escape(title),active.equals("home")?"active":"",active.equals("patients")?"active":"",
            active.equals("appointments")?"active":"",active.equals("billing")?"active":"",
            active.equals("notes")?"active":"",active.equals("mvc")?"active":"",content);
    }
    String good(String title,String subtitle) { return "<h1>"+title+"</h1><p class='subtitle'>"+subtitle+"</p>"; }
    String success() { return "<div class='notice'>Operation completed successfully.</div>"; }
    String empty(String message) {return "<p class='muted'>"+message+"</p>";}
    String patientOptions(HospitalModel m) {
        StringBuilder s=new StringBuilder();
        for(Patient p:m.patients()) s.append("<option value='").append(p.id).append("'>").append(p.id).append(" — ").append(escape(p.name)).append("</option>");
        return s.toString();
    }
    String dashboard(HospitalModel m) {
        String html=good("Hospital Dashboard","Register patients, manage appointments, generate bills and maintain treatment records.") +
            "<div class='grid'>"+
            stat("Patients",m.countPatients(),"Registered patients")+stat("Appointments",m.countAppointments(),"Scheduled visits")+
            stat("Bills",m.countBills(),"Generated bills")+stat("Records",m.countNotes(),"Treatment records")+"</div>"+
            "<div class='panel'><h2>Start here</h2><p>Go to <a href='/patients'>Patients</a> to add a patient, then schedule an appointment, generate a bill, or record treatment details.</p>"+
            "<p class='tip'><strong>MVC separation:</strong> browser shows the View, the Controller receives each action, and the Model validates and stores the data.</p></div>";
        return layout("Dashboard",html,"home");
    }
    String stat(String title,int number,String sub) {return "<div class='panel stat'><span>"+title+"</span><b>"+number+"</b><span>"+sub+"</span></div>";}
    String patients(HospitalModel m) {
        StringBuilder sb=new StringBuilder(good("Patient Registration","Create, view, edit and delete patient records."));
        sb.append("<div class='panel'><h2>Register new patient</h2><form action='/patients/add' method='post'>")
          .append("<label>Full name<input name='name' required maxlength='100' placeholder='Asha Sharma'></label>")
          .append("<label>Age<input name='age' type='number' min='1' max='130' required placeholder='35'></label>")
          .append("<label>Category<select name='category'><option>Outpatient</option><option>Inpatient</option><option>Emergency</option></select></label>")
          .append("<label>Phone<input name='phone' maxlength='25' placeholder='Mobile number'></label>")
          .append("<button type='submit'>Register patient</button></form></div>");
        sb.append("<div class='panel'><h2>Patient records</h2><div class='tablewrap'><table><thead><tr><th>ID</th><th>Patient</th><th>Age</th><th>Category</th><th>Phone</th><th>Actions</th></tr></thead><tbody>");
        for(Patient p:m.patients()) {
            sb.append("<tr><td>#").append(p.id).append("</td><td>").append(escape(p.name)).append("</td><td>").append(p.age)
              .append("</td><td><span class='tag'>").append(escape(p.category)).append("</span></td><td>").append(escape(p.phone)).append("</td><td>")
              .append("<form action='/patients/delete' method='post'><input type='hidden' name='id' value='").append(p.id).append("'>")
              .append("<button class='danger' onclick=\"return confirm('Delete this patient and linked records?')\">Delete</button></form></td></tr>");
        }
        sb.append("</tbody></table></div></div>");
        sb.append("<div class='panel'><h2>Edit a patient</h2><form action='/patients/update' method='post'>")
          .append("<label>Patient<select name='id' required>").append(patientOptions(m)).append("</select></label>")
          .append("<label>New name<input name='name' required></label><label>New age<input name='age' type='number' min='1' max='130' required></label>")
          .append("<label>New category<select name='category'><option>Outpatient</option><option>Inpatient</option><option>Emergency</option></select></label>")
          .append("<label>New phone<input name='phone'></label><button>Save changes</button></form></div>");
        return layout("Patients",sb.toString(),"patients");
    }
    String appointments(HospitalModel m) {
        StringBuilder sb=new StringBuilder(good("Appointments","Schedule a doctor consultation for a registered patient."));
        sb.append("<div class='panel'><h2>Schedule appointment</h2><form action='/appointments/add' method='post'>")
          .append("<label>Patient<select name='patientId' required>").append(patientOptions(m)).append("</select></label>")
          .append("<label>Appointment date<input type='date' name='date' required></label>")
          .append("<label>Doctor<input name='doctor' required placeholder='Dr. Kulkarni'></label>")
          .append("<label>Reason<input name='reason' placeholder='Routine check-up'></label>")
          .append("<button>Schedule</button></form></div>");
        sb.append("<div class='panel'><h2>Scheduled appointments</h2><div class='tablewrap'><table><thead><tr><th>ID</th><th>Patient</th><th>Date</th><th>Doctor</th><th>Reason</th><th></th></tr></thead><tbody>");
        for(Appointment a:m.appointments()) {
            Patient p=m.getPatient(a.patientId);
            sb.append("<tr><td>#").append(a.id).append("</td><td>").append(p==null?"—":escape(p.name)).append("</td><td>").append(escape(a.date))
              .append("</td><td>").append(escape(a.doctor)).append("</td><td>").append(escape(a.reason)).append("</td><td>")
              .append("<form action='/appointments/delete' method='post'><input type='hidden' name='id' value='").append(a.id)
              .append("'><button class='danger'>Cancel</button></form></td></tr>");
        }
        sb.append("</tbody></table></div></div>");
        return layout("Appointments",sb.toString(),"appointments");
    }
    String billing(HospitalModel m) {
        StringBuilder sb=new StringBuilder(good("Patient Billing","Select a billing strategy dynamically — Practical 7 Strategy Pattern."));
        sb.append("<div class='panel'><h2>Generate bill</h2><form action='/billing/add' method='post'>")
          .append("<label>Patient<select name='patientId' required>").append(patientOptions(m)).append("</select></label>")
          .append("<label>Base amount (₹)<input type='number' name='base' min='0' step='0.01' required placeholder='5000'></label>")
          .append("<label>Billing policy<select name='policy'><option>Self-Pay</option><option>Insurance</option><option>Corporate</option><option>Senior Citizen</option></select></label>")
          .append("<button>Generate bill</button></form><p class='muted'>Demonstration rates only: Insurance pays 30%, Corporate pays 75%, Senior Citizen pays 70%.</p></div>");
        sb.append("<div class='panel'><h2>Generated bills</h2><div class='tablewrap'><table><thead><tr><th>Bill</th><th>Patient</th><th>Policy</th><th>Base (₹)</th><th>Payable (₹)</th></tr></thead><tbody>");
        for(Bill b:m.bills()) {
            Patient p=m.getPatient(b.patientId);
            sb.append("<tr><td>#").append(b.id).append("</td><td>").append(p==null?"—":escape(p.name)).append("</td><td>").append(escape(b.strategy))
              .append("</td><td>").append(String.format(Locale.US,"%.2f",b.base)).append("</td><td><b>")
              .append(String.format(Locale.US,"%.2f",b.total)).append("</b></td></tr>");
        }
        sb.append("</tbody></table></div></div>");
        return layout("Billing",sb.toString(),"billing");
    }
    String notes(HospitalModel m) {
        StringBuilder sb=new StringBuilder(good("Treatment Records","Maintain patient consultation and treatment notes."));
        sb.append("<div class='panel'><h2>Add treatment record</h2><form action='/notes/add' method='post'>")
          .append("<label>Patient<select name='patientId' required>").append(patientOptions(m)).append("</select></label>")
          .append("<label>Diagnosis<textarea name='diagnosis' required placeholder='Clinical summary'></textarea></label>")
          .append("<label>Medicines<textarea name='medicines' placeholder='Prescribed medicines'></textarea></label>")
          .append("<label>Follow-up instructions<textarea name='instructions' placeholder='Follow-up instructions'></textarea></label>")
          .append("<button>Save record</button></form></div>");
        sb.append("<div class='panel'><h2>Saved records</h2><div class='tablewrap'><table><thead><tr><th>ID</th><th>Patient</th><th>Diagnosis</th><th>Medicines</th><th>Follow-up</th><th></th></tr></thead><tbody>");
        for(TreatmentNote n:m.notes()) {
            Patient p=m.getPatient(n.patientId);
            sb.append("<tr><td>#").append(n.id).append("</td><td>").append(p==null?"—":escape(p.name)).append("</td><td>").append(escape(n.diagnosis))
              .append("</td><td>").append(escape(n.medicines)).append("</td><td>").append(escape(n.instructions)).append("</td><td>")
              .append("<form action='/notes/delete' method='post'><input type='hidden' name='id' value='").append(n.id)
              .append("'><button class='danger'>Delete</button></form></td></tr>");
        }
        sb.append("</tbody></table></div></div><p class='tip'>Teaching demonstration only. Real hospital record systems require access controls, audit logs, encryption and compliant amendment handling.</p>");
        return layout("Treatment Records",sb.toString(),"notes");
    }
    String mvc() {
        String html=good("How MVC Works","Follow the request flow from a browser form to saved data and back to the screen.")+
            "<div class='panel'><div class='flow'><span class='node'>View<br/><small>HospitalView</small></span> → <span class='node'>Controller<br/><small>HospitalController</small></span> → <span class='node'>Model<br/><small>HospitalModel</small></span> → <span class='node'>Saved Data<br/><small>hospital-data.ser</small></span></div></div>"+
            "<div class='grid'>"+
            "<div class='panel'><h2>View</h2><p>Generates HTML, forms and tables. It does not validate or store patient records.</p></div>"+
            "<div class='panel'><h2>Controller</h2><p>Receives URLs and form submissions, extracts input, calls model methods and selects a page.</p></div>"+
            "<div class='panel'><h2>Model</h2><p>Validates, stores, retrieves, updates and deletes data. Contains billing strategies.</p></div></div>"+
            "<div class='panel'><h2>Trace one action</h2><p>1. Staff enters patient details in View → 2. Controller receives <code>POST /patients/add</code> → 3. Model validates and saves → 4. Controller redirects → 5. View renders the updated patient list.</p></div>"+
            "<div class='panel'><h2>Practical 8 mapping</h2><p>The previous practicals can be integrated into the Model/services layer. This starter implementation demonstrates a real Java web server and Strategy billing. Swing and JDBC are intentionally not used in this browser version.</p></div>";
        return layout("How MVC Works",html,"mvc");
    }
}
