import java.sql.*;
import java.util.*;

/** DAO / Repository: the ONLY class that runs SQL for application data. */
class HospitalDao {
    private final Database db;
    HospitalDao(Database db) { this.db=db; }

    @FunctionalInterface private interface RowMapper<T> { T map(ResultSet rs) throws SQLException; }

    private <T> List<T> list(String sql, RowMapper<T> mapper) {
        List<T> rows = new ArrayList<>();
        try (Connection c=db.connect(); PreparedStatement ps=c.prepareStatement(sql);
             ResultSet rs=ps.executeQuery()) {
            while (rs.next()) rows.add(mapper.map(rs));
            return rows;
        } catch (SQLException e) { throw fail(e); }
    }

    private int execute(String sql, Object... args) {
        try (Connection c=db.connect(); PreparedStatement ps=c.prepareStatement(sql)) {
            bind(ps, args);
            return ps.executeUpdate();
        } catch (SQLException e) { throw fail(e); }
    }

    private static void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i=0; i<args.length; i++) ps.setObject(i+1, args[i]);
    }
    private static IllegalStateException fail(SQLException e) {
        return new IllegalStateException("Database error: " + e.getMessage(), e);
    }

    private static Patient patientRow(ResultSet r) throws SQLException {
        return new Patient(r.getInt("id"), r.getString("name"), r.getInt("age"),
                r.getString("category"), r.getString("phone"));
    }
    List<Patient> patients() {
        return list("SELECT * FROM patients ORDER BY id DESC", HospitalDao::patientRow);
    }
    Patient getPatient(int id) {
        try (Connection c=db.connect();
             PreparedStatement ps=c.prepareStatement("SELECT * FROM patients WHERE id=?")) {
            ps.setInt(1,id);
            try (ResultSet rs=ps.executeQuery()) {
                return rs.next()?patientRow(rs):null;
            }
        } catch (SQLException e) { throw fail(e); }
    }
    void insertPatient(String name,int age,String category,String phone) {
        execute("INSERT INTO patients(name,age,category,phone) VALUES(?,?,?,?)",name,age,category,phone);
    }
    void updatePatient(int id,String name,int age,String category,String phone) {
        if (execute("UPDATE patients SET name=?,age=?,category=?,phone=? WHERE id=?",
                  name,age,category,phone,id)==0)
            throw new IllegalArgumentException("Patient not found.");
    }
    void deletePatient(int id) {
        // ON DELETE CASCADE automatically deletes associated rows in child tables.
        if (execute("DELETE FROM patients WHERE id=?",id)==0)
            throw new IllegalArgumentException("Patient not found.");
    }

    List<Appointment> appointments() {
        return list("SELECT * FROM appointments ORDER BY id DESC", r -> new Appointment(
           r.getInt("id"),r.getInt("patient_id"),r.getString("visit_date"),
           r.getString("doctor"),r.getString("reason")));
    }
    void insertAppointment(int patientId,String date,String doctor,String reason) {
        execute("INSERT INTO appointments(patient_id,visit_date,doctor,reason) VALUES(?,?,?,?)",
                patientId,date,doctor,reason);
    }
    void deleteAppointment(int id) { execute("DELETE FROM appointments WHERE id=?",id); }

    List<Bill> bills() {
        return list("SELECT * FROM bills ORDER BY id DESC", r -> new Bill(
            r.getInt("id"), r.getInt("patient_id"),
            r.getLong("base_cents")/100.0, r.getLong("total_cents")/100.0,
            r.getString("strategy")));
    }
    void insertBill(int patientId,long baseCents,long totalCents,String policy) {
        execute("INSERT INTO bills(patient_id,base_cents,total_cents,strategy) VALUES(?,?,?,?)",
                patientId,baseCents,totalCents,policy);
    }

    List<TreatmentNote> notes() {
        return list("SELECT * FROM treatment_records ORDER BY id DESC", r -> new TreatmentNote(
            r.getInt("id"),r.getInt("patient_id"),r.getString("diagnosis"),
            r.getString("medicines"),r.getString("instructions")));
    }
    void insertNote(int patientId,String diagnosis,String medicines,String instructions) {
        execute("INSERT INTO treatment_records(patient_id,diagnosis,medicines,instructions) VALUES(?,?,?,?)",
                patientId,diagnosis,medicines,instructions);
    }
    void deleteNote(int id) { execute("DELETE FROM treatment_records WHERE id=?",id); }

    private static DischargeSummaryDraft draftRow(ResultSet r) throws SQLException {
        return new DischargeSummaryDraft(r.getInt("id"),r.getInt("patient_id"),
            r.getString("diagnosis"),r.getString("medicines"),r.getString("follow_up"),
            r.getString("test_advice"),r.getString("remarks"),r.getInt("finalized")==1);
    }
    List<DischargeSummaryDraft> drafts() {
        return list("SELECT * FROM discharge_drafts ORDER BY id DESC",HospitalDao::draftRow);
    }
    DischargeSummaryDraft getDraft(int id) {
        try (Connection c=db.connect(); PreparedStatement ps=c.prepareStatement(
                "SELECT * FROM discharge_drafts WHERE id=?")) {
            ps.setInt(1,id);
            try (ResultSet rs=ps.executeQuery()) { return rs.next()?draftRow(rs):null; }
        } catch (SQLException e) { throw fail(e); }
    }
    void createDraft(int patientId) {
        execute("INSERT INTO discharge_drafts(patient_id) VALUES(?)",patientId);
    }
    void updateDraft(DischargeSummaryDraft d) {
        if (execute("UPDATE discharge_drafts SET diagnosis=?,medicines=?,follow_up=?,test_advice=?,remarks=? WHERE id=? AND finalized=0",
            d.diagnosis,d.medicines,d.followUp,d.testAdvice,d.remarks,d.id)==0)
            throw new IllegalArgumentException("Draft cannot be edited after finalization.");
    }
    void finalizeDraft(int draftId) {
        if (execute("UPDATE discharge_drafts SET finalized=1 WHERE id=? AND finalized=0",draftId)==0)
            throw new IllegalArgumentException("Draft is already finalized or missing.");
    }
    void insertSnapshot(int draftId,DischargeSummaryMemento m) {
        execute("INSERT INTO draft_versions(draft_id,diagnosis,medicines,follow_up,test_advice,remarks) VALUES(?,?,?,?,?,?)",
            draftId,m.diagnosis(),m.medicines(),m.followUp(),m.testAdvice(),m.remarks());
    }
    int snapshotCount(int draftId) {
        try (Connection c=db.connect(); PreparedStatement ps=c.prepareStatement(
                "SELECT COUNT(*) FROM draft_versions WHERE draft_id=?")) {
            ps.setInt(1,draftId);
            try (ResultSet rs=ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw fail(e); }
    }

    /** Restore-and-pop is a database transaction: update draft and consume snapshot together. */
    void restorePreviousDraft(DischargeSummaryDraft draft) {
        if (draft.finalized) throw new IllegalArgumentException("Finalized summaries cannot be silently restored.");
        try (Connection c=db.connect()) {
            c.setAutoCommit(false);
            try {
                int snapshotId;
                DischargeSummaryMemento m;
                try (PreparedStatement ps=c.prepareStatement(
                        "SELECT * FROM draft_versions WHERE draft_id=? ORDER BY id DESC LIMIT 1")) {
                    ps.setInt(1,draft.id);
                    try (ResultSet r=ps.executeQuery()) {
                        if (!r.next()) throw new IllegalArgumentException("No saved draft version to restore.");
                        snapshotId=r.getInt("id");
                        m=new DischargeSummaryMemento(r.getString("diagnosis"),r.getString("medicines"),
                            r.getString("follow_up"),r.getString("test_advice"),r.getString("remarks"));
                    }
                }
                draft.restore(m); // Memento restores the Originator's internal state.
                try (PreparedStatement ps=c.prepareStatement(
                        "UPDATE discharge_drafts SET diagnosis=?,medicines=?,follow_up=?,test_advice=?,remarks=? WHERE id=? AND finalized=0")) {
                    bind(ps,draft.diagnosis,draft.medicines,draft.followUp,draft.testAdvice,draft.remarks,draft.id);
                    if (ps.executeUpdate()==0) throw new IllegalArgumentException("Draft was finalized; cannot restore.");
                }
                try (PreparedStatement ps=c.prepareStatement("DELETE FROM draft_versions WHERE id=?")) {
                    ps.setInt(1,snapshotId); ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) { throw fail(e); }
    }

    int count(String table) {
        // Hard-coded whitelist. Never concatenate user input into SQL identifiers.
        if (!Set.of("patients","appointments","bills","treatment_records","discharge_drafts").contains(table))
            throw new IllegalArgumentException("Invalid table name.");
        try (Connection c=db.connect(); Statement s=c.createStatement();
             ResultSet rs=s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next(); return rs.getInt(1);
        } catch (SQLException e) { throw fail(e); }
    }
}
