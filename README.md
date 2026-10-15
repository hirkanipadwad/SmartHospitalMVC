# Smart Hospital Management System — MVC + SQLite/JDBC (Practical 8)

A **real Java web application** for teaching Model–View–Controller architecture with a persistent **SQLite database accessed using JDBC**.

**No Java or database server installation is needed on students' computers if you use GitHub Codespaces.** Maven automatically downloads the required SQLite JDBC driver the first time you run the project. Internet connectivity is needed for this initial dependency download.

## Features

- **Patients:** register, display, update, delete (CRUD)
- **Appointments:** schedule and cancel consultations
- **Billing:** calculate and save bills using Strategy Pattern (Self-Pay, Insurance, Corporate, Senior Citizen)
- **Treatment records:** enter, display and remove teaching records
- **Discharge summary drafts:** create, edit, save snapshots, restore a previous draft, finalize (Memento Pattern)
- **Persistence:** six SQLite tables in `data/hospital.db`, created automatically
- **MVC guide:** in-app page explaining View → Controller → Model → DAO → SQLite

## Run in GitHub Codespaces (recommended)

1. Create a **new GitHub repository** (for example, `SmartHospitalMVC-SQLite`).
2. **Unzip** this project on your computer. Upload its **contents** to your repository, keeping `pom.xml`, `.devcontainer/`, and `src/` at the repository root. Alternatively, open Codespaces first and upload the extracted project files in the file Explorer.
3. From the repository's **Code** button, select **Codespaces → Create codespace on main**.
4. In the Codespaces terminal, run:

   ```bash
   mvn compile exec:java
   ```

5. When it prints `Smart Hospital MVC + SQLite running at http://localhost:8080`, click **Ports** (usually in the bottom panel). Open the forwarded URL for port **8080**. If Codespaces does not automatically forward it, add port `8080`.
6. Stop the application with **Ctrl+C** in the terminal.

> `.devcontainer/devcontainer.json` provides Java 21 and forwards port 8080. If your Codespace already exists, rebuilding it may be necessary for the new configuration to take effect.

**Important:** This project is configured for Maven, so the old commands `javac HospitalApp.java` and `java HospitalApp` are **not** sufficient on their own. Maven downloads and adds the SQLite JDBC driver to the classpath.

## Run the built-in JDBC smoke test

In the project root in the Codespaces terminal:

```bash
mvn -q compile exec:java -Dexec.mainClass=HospitalJdbcSmokeTest
```

This tests SQL INSERT/SELECT/UPDATE/DELETE, Strategy billing, Memento save/restore, finalization rules, and foreign-key cascading. It uses a temporary test database, **not** the main `data/hospital.db`.

## MVC and JDBC code map

| File | Responsibility |
|---|---|
| `HospitalApp.java` | Starts server and connects MVC components |
| `HospitalController.java` | Receives GET/POST, reads form values, calls Model, redirects |
| `HospitalView.java` | Generates browser-based HTML UI; contains no SQL |
| `HospitalModel.java` | Performs business validation; uses Billing Strategy and Memento |
| `HospitalDao.java` | Uses JDBC `Connection`, `PreparedStatement`, and `ResultSet` for CRUD |
| `Database.java` | Opens JDBC connections, initializes tables from `schema.sql` |
| `HospitalData.java` | Entity types: Patient, Appointment, Bill, TreatmentNote |
| `BillingStrategy.java` | Practical 7 Strategy pattern (illustrative policies) |
| `DischargeSummaryDraft.java` | Practical 7 Memento pattern (draft-only restore) |
| `src/main/resources/schema.sql` | SQLite schema including foreign keys |
| `HospitalJdbcSmokeTest.java` | Executable end-to-end checks (requires JDBC dependency) |

## SQL examples (inspect actual database)

From a Codespaces terminal, if the `sqlite3` CLI is available:

```bash
sqlite3 data/hospital.db ".tables"
sqlite3 data/hospital.db "SELECT id, name, age, category FROM patients;"
sqlite3 data/hospital.db "SELECT * FROM appointments;"
sqlite3 data/hospital.db "SELECT * FROM bills;"
```

If `sqlite3` is not installed, use the **Python standard library** (already present in many Codespace images):

```bash
python3 -c 'import sqlite3; c=sqlite3.connect("data/hospital.db"); print(c.execute("SELECT name FROM sqlite_master WHERE type=\x27table\x27").fetchall())'
```

The database remains in `data/hospital.db` between program restarts **while your Codespace is retained**. Files are lost if the Codespace is deleted; commit/export only fictional test data. The old `hospital-data.ser` serialization file is not migrated or read by this version.

## Student demonstration sequence

1. Register a fictional patient, refresh **Patients**, and observe the row is stored.
2. Edit patient details and add an appointment.
3. Generate two example bills with different billing policies; compare payable amounts.
4. Add a treatment record.
5. Create a discharge draft, update it, **Save version**, change it again, **Restore last saved version**, then **Finalize**.
6. Restart the app and verify that records remain in the database.
7. Open the **How MVC Works** page. Trace the `POST /patients/add` request through the Controller, Model, DAO and SQL database.

## Design scope and safety

- This is a **Java web application**, not a Java Swing desktop GUI. It fulfills MVC and JDBC/SQLite learning goals but **does not implement the Swing-specific requirement** in the original Practical 8 statement.
- The full Practical 6 HospitalCoordinator Mediator and PatientTreatmentProcess Template Method workflow are **not wired into this starter project**. Practical 7 Strategy and draft Memento **are included**.
- Example discounts are only for explaining Strategy; **not real billing rules**.
- For fictional demo patients **only**. There is no login, roles, encryption, session security, or production-grade audit trail. Do **not** publish a public port with real patient records. Prefer a **private** forwarded port when demonstrating.
- Persisted treatment-record deletion is for classroom CRUD; real medical record systems require controlled amendments and access/audit safeguards.
- Input is parameterized in JDBC SQL to illustrate safe queries; this app is not production-ready.
