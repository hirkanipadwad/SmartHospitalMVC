/** PRACTICAL 7: MEMENTO - the Originator can save and restore draft state. */
class DischargeSummaryDraft {
    final int id, patientId;
    String diagnosis, medicines, followUp, testAdvice, remarks;
    boolean finalized;

    DischargeSummaryDraft(int id, int patientId, String diagnosis, String medicines,
                          String followUp, String testAdvice, String remarks, boolean finalized) {
        this.id=id; this.patientId=patientId; this.diagnosis=diagnosis; this.medicines=medicines;
        this.followUp=followUp; this.testAdvice=testAdvice; this.remarks=remarks; this.finalized=finalized;
    }
    DischargeSummaryMemento save() {
        if (finalized) throw new IllegalArgumentException("A finalized summary cannot be saved as a draft.");
        return new DischargeSummaryMemento(diagnosis, medicines, followUp, testAdvice, remarks);
    }
    void restore(DischargeSummaryMemento earlier) {
        if (finalized) throw new IllegalArgumentException("Finalized summaries cannot be silently restored.");
        diagnosis=earlier.diagnosis(); medicines=earlier.medicines(); followUp=earlier.followUp();
        testAdvice=earlier.testAdvice(); remarks=earlier.remarks();
    }
}

/** Immutable saved draft state; Caretaker holds it without changing the fields. */
record DischargeSummaryMemento(String diagnosis, String medicines,
        String followUp, String testAdvice, String remarks) {}

/** Caretaker: manages saved mementos through the JDBC repository. */
class DischargeSummaryHistory {
    private final HospitalDao dao;
    DischargeSummaryHistory(HospitalDao dao) { this.dao=dao; }
    void saveState(int draftId, DischargeSummaryMemento snapshot) { dao.insertSnapshot(draftId, snapshot); }
    void restoreLastState(DischargeSummaryDraft draft) { dao.restorePreviousDraft(draft); }
}
