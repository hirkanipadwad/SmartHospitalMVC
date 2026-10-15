import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** CONTROLLER: translates HTTP requests into model actions; never writes SQL or builds HTML. */
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
                        case "/drafts/create" -> model.createDraft(number(form,"patientId"));
                        case "/drafts/update" -> model.updateDraft(number(form,"id"),value(form,"diagnosis"),
                            value(form,"medicines"),value(form,"followUp"),value(form,"testAdvice"),value(form,"remarks"));
                        case "/drafts/save" -> model.saveDraftVersion(number(form,"id"));
                        case "/drafts/restore" -> model.restoreDraftVersion(number(form,"id"));
                        case "/drafts/finalize" -> model.finalizeDraft(number(form,"id"));
                        default -> { send(ex,404,"Not found"); return; }
                    }
                    String target=switch(path.split("/")[1]) {
                        case "patients" -> "/patients";
                        case "appointments" -> "/appointments";
                        case "billing" -> "/billing";
                        case "drafts" -> "/drafts";
                        default -> "/notes";
                    };
                    ex.getResponseHeaders().set("Location", target+"?ok=1");
                    ex.sendResponseHeaders(303,-1);
                } catch (Exception e) {
                    send(ex,400,view.layout("Error", "<div class='notice error'>"+HospitalView.escape(e.getMessage())+"</div><a href='/'>Return to dashboard</a>",""));
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
                case "/drafts" -> view.drafts(model);
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

