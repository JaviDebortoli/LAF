package Argumentation.LAF.Controller;

import Argumentation.LAF.DTO.Request.OperationInputRequest;
import Argumentation.LAF.DTO.Request.ProgramInputRequest;
import Argumentation.LAF.Service.AlgebraService;
import Argumentation.LAF.Service.ProgramLoaderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller responsible for managing the knowledge base of the system.
 *
 * <p>
 * This controller exposes endpoints for uploading programs (facts and rules)
 * and algebraic operations that define how argument labels are combined
 * during inference.
 * </p>
 *
 * @see ProgramLoaderService
 * @see AlgebraService
 * 
 * @author JaviDebórtoli
 */
@RestController
@RequestMapping("/api")
public class KnowledgeBaseController {
    /** Service responsible for loading and storing the current program. */
    private final ProgramLoaderService programLoaderService;
    /** Service responsible for loading and managing algebraic operations associated with labels. */
    private final AlgebraService algebraService;

    /**
     * Constructs a {@code KnowledgeBaseController} with the required services
     * to manage the knowledge base of the argumentation system.
     *
     * <p>
     * This controller relies on {@link ProgramLoaderService} to load facts and
     * rules into the system, and on {@link AlgebraService} to register the
     * algebraic operations associated with argument labels.
     * </p>
     *
     * @param programLoaderService service responsible for loading facts and
     *                             rules provided by the client
     * @param algebraService service responsible for managing algebraic
     *                       operations over argument labels
     */
    public KnowledgeBaseController(ProgramLoaderService programLoaderService, AlgebraService algebraService) {
        this.programLoaderService = programLoaderService;
        this.algebraService = algebraService;
    }

    /**
     * Uploads a new program consisting of facts and rules.
     *
     * <p>
     * Any previously loaded program is replaced by the new one.
     * </p>
     *
     * @param request the request containing the program definition
     * @return an empty HTTP response with status 204 (No Content)
     */
    @PostMapping("/program")
    public ResponseEntity<Void> uploadProgram(@RequestBody ProgramInputRequest request) {
        programLoaderService.loadProgram(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Uploads the algebraic operations associated with argument labels.
     *
     * <p>
     * These operations define how support, aggregation and conflict are
     * computed during the inference process.
     * </p>
     *
     * @param request the request containing the operation definitions
     * @return an empty HTTP response with status 204 (No Content)
     */
    @PostMapping("/operations")
    public ResponseEntity<Void> uploadOperations(@RequestBody OperationInputRequest request) {
        algebraService.loadOperations(request);
        return ResponseEntity.noContent().build();
    }
}