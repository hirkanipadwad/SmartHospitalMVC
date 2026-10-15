/** Model entities: plain Java objects representing database rows. */
class Patient {
    final int id, age;
    final String name, category, phone;
    Patient(int id, String name, int age, String category, String phone) {
        this.id = id; this.name = name; this.age = age;
        this.category = category; this.phone = phone;
    }
}
class Appointment {
    final int id, patientId;
    final String date, doctor, reason;
    Appointment(int id, int patientId, String date, String doctor, String reason) {
        this.id=id; this.patientId=patientId; this.date=date; this.doctor=doctor; this.reason=reason;
    }
}
class Bill {
    final int id, patientId;
    final double base, total;
    final String strategy;
    Bill(int id, int patientId, double base, double total, String strategy) {
        this.id=id; this.patientId=patientId; this.base=base; this.total=total; this.strategy=strategy;
    }
}
class TreatmentNote {
    final int id, patientId;
    final String diagnosis, medicines, instructions;
    TreatmentNote(int id, int patientId, String diagnosis, String medicines, String instructions) {
        this.id=id; this.patientId=patientId; this.diagnosis=diagnosis;
        this.medicines=medicines; this.instructions=instructions;
    }
}
