import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/** Entry point: wires together the MVC components and starts the Java HTTP server. */
public class HospitalApp {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        Path dbPath = Path.of(System.getenv().getOrDefault("DATABASE_PATH", "data/hospital.db"));

        Database database = new Database(dbPath);
        database.initialize(); // Runs schema.sql; tables are created if they do not exist.

        HospitalDao dao = new HospitalDao(database); // JDBC/data access layer
        HospitalModel model = new HospitalModel(dao); // Validation and business rules
        HospitalView view = new HospitalView();      // HTML presentation
        HospitalController controller = new HospitalController(model, view); // HTTP routing

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", controller::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Smart Hospital MVC + SQLite running at http://localhost:" + port);
        System.out.println("SQLite DB: " + dbPath.toAbsolutePath());
        System.out.println("Press Ctrl+C to stop.");
    }
}
