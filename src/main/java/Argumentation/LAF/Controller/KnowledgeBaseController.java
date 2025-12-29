/*
 * 
 */
package Argumentation.LAF.Controller;

import Argumentation.LAF.DTO.Request.OperationInputRequest;
import Argumentation.LAF.DTO.Request.ProgramInputRequest;
import Argumentation.LAF.Service.AlgebraService;
import Argumentation.LAF.Service.ProgramLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class KnowledgeBaseController {
    
    @Autowired
    private final ProgramLoaderService programLoaderService;
    private final AlgebraService algebraService;

    public KnowledgeBaseController(ProgramLoaderService programLoaderService, AlgebraService algebraService) {
        this.programLoaderService = programLoaderService;
        this.algebraService = algebraService;
    }

    // Recibir programa (hechos + reglas) en JSON
    @PostMapping("/program")
    public ResponseEntity<Void> uploadProgram(@RequestBody ProgramInputRequest request) {
        programLoaderService.loadProgram(request);
        return ResponseEntity.noContent().build();
    }

    // Recibir funciones del álgebra (3 por etiqueta) en JSON
    @PostMapping("/operations")
    public ResponseEntity<Void> uploadOperations(@RequestBody OperationInputRequest request) {
        algebraService.loadOperations(request);
        return ResponseEntity.noContent().build();
    }
    
}
