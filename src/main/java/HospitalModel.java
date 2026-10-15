import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** MODEL: domain rules and calls to DAO; never accesses HttpExchange or renders HTML. */
class HospitalModel {
    private final HospitalDao dao;
    private final BillingService billingService = new BillingService();
    private final DischargeSummaryHistory history;
    HospitalModel(HospitalDao dao) { this.dao=dao; this.history=new DischargeSummaryHistory(dao); }

    List<Patient> patients() { return dao.patients(); }
    List<Appointment> appointments() { return dao.appointments(); }
    List<Bill> bills() { return dao.bills(); }
    List<TreatmentNote> notes() { return dao.notes(); }
    List<DischargeSummaryDraft> drafts() { return dao.drafts(); }
    Patient getPatient(int id) { return dao.getPatient(id); }
    DischargeSummaryDraft getDraft(int id) { return dao.getDraft(id); }
    int savedVersions(int draftId) { return dao.snapshotCount(draftId); }

    private Patient requirePatient(int id) {
        Patient p=dao.getPatient(id);
        if (p==null) throw new IllegalArgumentException("Select a registered patient.");
        return p;
    }
    private DischargeSummaryDraft requireDraft(int id) {
        DischargeSummaryDraft d=dao.getDraft(id);
        if (d==null) throw new IllegalArgumentException("Draft not found.");
        return d;
    }
    private static void validatePatient(String name,int age,String category,String phone) {
        if(name==null || name.isBlank() || name.length()>100 || age<1 || age>130)
            throw new IllegalArgumentException("Enter a valid patient name and age (1-130).");
        if(!Set.of("Outpatient","Inpatient","Emergency").contains(category))
            throw new IllegalArgumentException("Invalid patient category.");
        if(phone.length()>25) throw new IllegalArgumentException("Phone number is too long.");
    }
    void addPatient(String name,int age,String category,String phone) {
        validatePatient(name,age,category,phone); dao.insertPatient(name,age,category,phone);
    }
    void updatePatient(int id,String name,int age,String category,String phone) {
        validatePatient(name,age,category,phone); dao.updatePatient(id,name,age,category,phone);
    }
    void deletePatient(int id) { dao.deletePatient(id); }

    void addAppointment(int patientId,String date,String doctor,String reason) {
        requirePatient(patientId);
        try { LocalDate.parse(date); }
        catch(Exception e) { throw new IllegalArgumentException("Enter an appointment date in YYYY-MM-DD format."); }
        if(doctor.isBlank() || doctor.length()>100) throw new IllegalArgumentException("Doctor name is required.");
        dao.insertAppointment(patientId,date,doctor,reason);
    }
    void deleteAppointment(int id) { dao.deleteAppointment(id); }

    Bill createBill(int patientId,double baseAmount,String policy) {
        requirePatient(patientId);
        if(!Double.isFinite(baseAmount) || baseAmount < 0 || baseAmount > 100_000_000)
            throw new IllegalArgumentException("Enter a non-negative billing amount up to 100,000,000.");
        long baseCents=BigDecimal.valueOf(baseAmount).movePointRight(2)
                         .setScale(0, RoundingMode.HALF_UP).longValueExact();
        BillingStrategy strategy=billingService.choose(policy);
        long finalCents=strategy.payableCents(baseCents);
        dao.insertBill(patientId,baseCents,finalCents,strategy.name());
        return new Bill(0,patientId,baseCents/100.0,finalCents/100.0,strategy.name());
    }

    void addNote(int patientId,String diagnosis,String medicines,String instructions) {
        requirePatient(patientId);
        if(diagnosis.isBlank()) throw new IllegalArgumentException("Diagnosis is required.");
        dao.insertNote(patientId,diagnosis,medicines,instructions);
    }
    void deleteNote(int id) { dao.deleteNote(id); }

    // Memento: only editable discharge summary drafts support restore.
    void createDraft(int patientId) { requirePatient(patientId); dao.createDraft(patientId); }
    void updateDraft(int id,String diagnosis,String medicines,String followUp,String testAdvice,String remarks) {
        DischargeSummaryDraft d=requireDraft(id);
        if(d.finalized) throw new IllegalArgumentException("Finalized summary cannot be edited. Create an amendment separately.");
        d.diagnosis=diagnosis;d.medicines=medicines;d.followUp=followUp;d.testAdvice=testAdvice;d.remarks=remarks;
        dao.updateDraft(d);
    }
    void saveDraftVersion(int id) {
        DischargeSummaryDraft d=requireDraft(id);
        history.saveState(id,d.save());
    }
    void restoreDraftVersion(int id) { history.restoreLastState(requireDraft(id)); }
    void finalizeDraft(int id) { dao.finalizeDraft(id); }

    int countPatients() { return dao.count("patients"); }
    int countAppointments() { return dao.count("appointments"); }
    int countBills() { return dao.count("bills"); }
    int countNotes() { return dao.count("treatment_records"); }
    int countDrafts() { return dao.count("discharge_drafts"); }
}
