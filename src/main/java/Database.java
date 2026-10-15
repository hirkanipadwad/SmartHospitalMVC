import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

/** JDBC utility: owns connection setup and initial schema creation, not business rules. */
class Database {
    private final Path path;
    Database(Path path) { this.path = path; }

    Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("PRAGMA busy_timeout=5000");
        }
        return c;
    }

    void initialize() throws Exception {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Class.forName("org.sqlite.JDBC"); // Provided automatically by sqlite-jdbc Maven dependency.
        String sql;
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            if (in == null) throw new IllegalStateException("schema.sql missing from classpath");
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Connection c = connect(); Statement s = c.createStatement()) {
            // Teaching schema has no semicolons inside SQL strings.
            for (String part : sql.split(";")) {
                String statement = part.lines().filter(line -> !line.stripLeading().startsWith("--"))
                         .reduce("", (a,b) -> a + "\n" + b).trim();
                if (!statement.isBlank()) s.execute(statement);
            }
        }
    }
}
