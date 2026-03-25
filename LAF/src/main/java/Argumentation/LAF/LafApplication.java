package Argumentation.LAF;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point of the LAF web application.
 *
 * <p>
 * This class bootstraps the Spring Boot application and initializes
 * all configured components, including controllers, services and
 * domain elements required to run the Label-Based Argumentation
 * Framework backend.
 * </p>
 *
 * <p>
 * The application exposes REST endpoints for loading knowledge bases,
 * defining algebraic operations and generating argumentation graphs
 * based on the LAF formalism.
 * </p>
 *
 * @author Javi Debórtoli
 */
@SpringBootApplication
public class LafApplication {
    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments passed during application startup
     */
    public static void main(String[] args) {
        SpringApplication.run(LafApplication.class, args);
    }
}