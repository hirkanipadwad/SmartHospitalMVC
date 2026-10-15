import java.util.*;

/** VIEW: renders HTML for the browser. No JDBC and no business calculations. */
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
        <header><div><strong>✚ Smart Hospital</strong><br/><span>Java Web Application · MVC Architecture</span></div><span>Educational project · Java + JDBC + SQLite</span></header>
        <nav>
        <a class="%s" href="/">Dashboard</a><a class="%s" href="/patients">Patients</a>
        <a class="%s" href="/appointments">Appointments</a><a class="%s" href="/billing">Billing</a>
        <a class="%s" href="/notes">Treatment Records</a><a class="%s" href="/drafts">Discharge Drafts</a><a class="%s" href="/mvc">How MVC Works</a>
        </nav><main>%s</main></body></html>
        """.formatted(escape(title),active.equals("home")?"active":"",active.equals("patients")?"active":"",
            active.equals("appointments")?"active":"",active.equals("billing")?"active":"",
            active.equals("notes")?"active":"",active.equals("drafts")?"active":"",active.equals("mvc")?"active":"",content);
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
            stat("Bills",m.countBills(),"Generated bills")+stat("Records",m.countNotes(),"Treatment records")+stat("Drafts",m.countDrafts(),"Editable discharge summaries")+"</div>"+
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
    /** Displays editable discharge summary drafts (Memento Pattern, Practical 7). */
    String drafts(HospitalModel m) {
        StringBuilder sb = new StringBuilder(good("Discharge Summary Drafts",
            "Save and restore drafts before finalization. Finalized summaries cannot be silently undone."));
        sb.append("<div class='panel'><h2>Create a new draft</h2><form action='/drafts/create' method='post'>")
          .append("<label>Patient<select name='patientId' required>").append(patientOptions(m)).append("</select></label>")
          .append("<button>Create draft</button></form></div>");
        for (DischargeSummaryDraft d : m.drafts()) {
            Patient p = m.getPatient(d.patientId);
            sb.append("<div class='panel'><h2>Draft #").append(d.id).append(" — ")
              .append(p==null?"Unknown patient":escape(p.name)).append(" <span class='tag'>")
              .append(d.finalized?"FINALIZED":"EDITABLE").append("</span></h2>")
              .append("<p class='muted'>Saved versions: ").append(m.savedVersions(d.id)).append("</p>")
              .append("<form action='/drafts/update' method='post'><input type='hidden' name='id' value='").append(d.id).append("'>")
              .append("<label>Diagnosis<textarea name='diagnosis' ").append(d.finalized?"disabled":"")
              .append(">").append(escape(d.diagnosis)).append("</textarea></label>")
              .append("<label>Medicines<textarea name='medicines' ").append(d.finalized?"disabled":"")
              .append(">").append(escape(d.medicines)).append("</textarea></label>")
              .append("<label>Follow-up<textarea name='followUp' ").append(d.finalized?"disabled":"")
              .append(">").append(escape(d.followUp)).append("</textarea></label>")
              .append("<label>Test advice<textarea name='testAdvice' ").append(d.finalized?"disabled":"")
              .append(">").append(escape(d.testAdvice)).append("</textarea></label>")
              .append("<label>Discharge remarks<textarea name='remarks' ").append(d.finalized?"disabled":"")
              .append(">").append(escape(d.remarks)).append("</textarea></label>");
            if (!d.finalized) sb.append("<button>Update current draft</button>");
            sb.append("</form>");
            if (!d.finalized) {
                sb.append("<div class='flow' style='margin-top:18px'>")
                  .append("<form action='/drafts/save' method='post'><input type='hidden' name='id' value='").append(d.id)
                  .append("'><button>Save version</button></form>")
                  .append("<form action='/drafts/restore' method='post'><input type='hidden' name='id' value='").append(d.id)
                  .append("'><button>Restore last saved version</button></form>")
                  .append("<form action='/drafts/finalize' method='post'><input type='hidden' name='id' value='").append(d.id)
                  .append("'><button class='danger' onclick=\"return confirm('Finalize this draft? It will be locked.')\">Finalize</button></form></div>");
            } else {
                sb.append("<p class='tip'>Finalized. Corrections should be recorded as amendments; this demo does not implement an amendment workflow.</p>");
            }
            sb.append("</div>");
        }
        sb.append("<p class='tip'>Suggested demo: Create draft → enter details → Save version → change details → Restore last saved version → Finalize.</p>");
        return layout("Discharge Drafts", sb.toString(), "drafts");
    }
    String mvc() {
        String html=good("How MVC Works","Follow the request flow from a browser form to saved data and back to the screen.")+
            "<div class='panel'><div class='flow'><span class='node'>View<br/><small>HospitalView</small></span> → <span class='node'>Controller<br/><small>HospitalController</small></span> → <span class='node'>Model<br/><small>HospitalModel</small></span> → <span class='node'>DAO (JDBC)<br/><small>HospitalDao</small></span> → <span class='node'>SQLite<br/><small>data/hospital.db</small></span></div></div>"+
            "<div class='grid'>"+
            "<div class='panel'><h2>View</h2><p>Generates HTML, forms and tables. It does not validate or store patient records.</p></div>"+
            "<div class='panel'><h2>Controller</h2><p>Receives URLs and form submissions, extracts input, calls model methods and selects a page.</p></div>"+
            "<div class='panel'><h2>Model</h2><p>Validates input, applies billing strategies and calls HospitalDao, which executes parameterized SQL through JDBC.</p></div></div>"+
            "<div class='panel'><h2>Trace one action</h2><p>1. Staff enters patient details in View → 2. Controller receives <code>POST /patients/add</code> → 3. Model validates and saves → 4. Controller redirects → 5. View renders the updated patient list.</p></div>"+
            "<div class='panel'><h2>Practical 8 mapping</h2><p>This app implements MVC, SQLite/JDBC data access, Strategy billing, and Memento draft snapshots. The interface is a Java-served HTML browser interface instead of Swing. The full Mediator and Template Method workflow from Practical 6 is not included.</p></div>";
        return layout("How MVC Works",html,"mvc");
    }
}
