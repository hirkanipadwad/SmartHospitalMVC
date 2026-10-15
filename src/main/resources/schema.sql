-- Practical 8 - SQL database schema. Executed once at startup (IF NOT EXISTS).
-- SQLite foreign keys are enabled on every JDBC connection.
CREATE TABLE IF NOT EXISTS patients (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    age INTEGER NOT NULL CHECK(age BETWEEN 1 AND 130),
    category TEXT NOT NULL,
    phone TEXT NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS appointments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    patient_id INTEGER NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    visit_date TEXT NOT NULL,
    doctor TEXT NOT NULL,
    reason TEXT NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS bills (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    patient_id INTEGER NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    base_cents INTEGER NOT NULL CHECK(base_cents >= 0),
    total_cents INTEGER NOT NULL CHECK(total_cents >= 0),
    strategy TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS treatment_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    patient_id INTEGER NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    diagnosis TEXT NOT NULL,
    medicines TEXT NOT NULL DEFAULT '',
    instructions TEXT NOT NULL DEFAULT ''
);

-- Memento: current draft + historic snapshots (separate tables).
CREATE TABLE IF NOT EXISTS discharge_drafts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    patient_id INTEGER NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    diagnosis TEXT NOT NULL DEFAULT '',
    medicines TEXT NOT NULL DEFAULT '',
    follow_up TEXT NOT NULL DEFAULT '',
    test_advice TEXT NOT NULL DEFAULT '',
    remarks TEXT NOT NULL DEFAULT '',
    finalized INTEGER NOT NULL DEFAULT 0 CHECK(finalized IN (0,1))
);

CREATE TABLE IF NOT EXISTS draft_versions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    draft_id INTEGER NOT NULL REFERENCES discharge_drafts(id) ON DELETE CASCADE,
    diagnosis TEXT NOT NULL,
    medicines TEXT NOT NULL,
    follow_up TEXT NOT NULL,
    test_advice TEXT NOT NULL,
    remarks TEXT NOT NULL,
    saved_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
