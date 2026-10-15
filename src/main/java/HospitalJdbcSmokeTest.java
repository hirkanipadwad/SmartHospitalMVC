import java.nio.file.Files;
import java.nio.file.Path;

/** Run: mvn -q compile exec:java -Dexec.mainClass=HospitalJdbcSmokeTest */
public class HospitalJdbcSmokeTest {
    private static void check(boolean ok, String step) {
        if (!ok) throw new AssertionError("FAILED: " + step);
        System.out.println("PASS: " + step);
    }
    public static void main(String[] args) throws Exception {
        Path dbPath = Files.createTempFile("hospital-mvc-test-", ".db");
        try {
            Database database = new Database(dbPath);
            database.initialize();
            HospitalModel m = new HospitalModel(new HospitalDao(database));

            m.addPatient("Sample Patient",32,"Outpatient","0000000000");
            check(m.countPatients()==1,"INSERT and SELECT patient");
            int patientId = m.patients().get(0).id;
            m.updatePatient(patientId,"Updated Patient",33,"Inpatient","1111111111");
            check(m.getPatient(patientId).name.equals("Updated Patient"),"UPDATE patient");

            m.addAppointment(patientId,"2026-10-15","Dr. Demo","Follow-up visit");
            check(m.countAppointments()==1,"INSERT appointment");

            Bill b = m.createBill(patientId,1000,"Insurance");
            check(b.total==300.0 && m.countBills()==1,"Strategy calculates bill + JDBC insert");
            m.addNote(patientId,"Demonstration diagnosis","Medicine X","Return in 3 days");
            check(m.countNotes()==1,"INSERT treatment record");

            m.createDraft(patientId);
            int draftId = m.drafts().get(0).id;
            m.updateDraft(draftId,"First diagnosis","Medicine A","3 days","CBC","Stable");
            m.saveDraftVersion(draftId);
            check(m.savedVersions(draftId)==1,"SAVE Memento snapshot in SQLite");
            m.updateDraft(draftId,"Changed diagnosis","Medicine B","5 days","","Observation");
            m.restoreDraftVersion(draftId);
            check(m.getDraft(draftId).diagnosis.equals("First diagnosis") &&
                  m.savedVersions(draftId)==0,"RESTORE Memento in transaction");
            m.finalizeDraft(draftId);
            boolean prevented = false;
            try { m.restoreDraftVersion(draftId); }
            catch(IllegalArgumentException expected) { prevented = true; }
            check(prevented,"Finalized draft cannot be restored");

            m.deletePatient(patientId);
            check(m.countPatients()==0 && m.countAppointments()==0 && m.countBills()==0
                  && m.countNotes()==0 && m.countDrafts()==0,"DELETE patient + FK cascade");
            System.out.println("ALL JDBC TESTS PASSED");
        } finally {
            Files.deleteIfExists(dbPath);
        }
    }
}
