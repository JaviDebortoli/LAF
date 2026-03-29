package Argumentation.LAF.Service;

import Argumentation.LAF.DTO.Request.GraphRequest;
import Argumentation.LAF.DTO.Response.GraphProcessResponse;
import Argumentation.LAF.DTO.Response.GraphResponse;
import Argumentation.LAF.DTO.Response.NarrationMetaResponse;
import Argumentation.LAF.DTO.Response.NarrativeTraceResponse;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class GraphProcessService {
    private final ProgramMapperService programMapperService;
    private final AlgebraMapperService algebraMapperService;
    private final InferenceService inferenceService;
    private final GraphBuilderService graphBuilderService;
    private final NarrativeTraceBuilderService narrativeTraceBuilderService;
    private final LlmNarrativeService llmNarrativeService;

    public GraphProcessService(
            ProgramMapperService programMapperService,
            AlgebraMapperService algebraMapperService,
            InferenceService inferenceService,
            GraphBuilderService graphBuilderService,
            NarrativeTraceBuilderService narrativeTraceBuilderService,
            LlmNarrativeService llmNarrativeService) {
        this.programMapperService = programMapperService;
        this.algebraMapperService = algebraMapperService;
        this.inferenceService = inferenceService;
        this.graphBuilderService = graphBuilderService;
        this.narrativeTraceBuilderService = narrativeTraceBuilderService;
        this.llmNarrativeService = llmNarrativeService;
    }

    public GraphProcessResponse process(GraphRequest request) {
        var facts = programMapperService.mapFacts(request.getFacts());
        var rules = programMapperService.mapRules(request.getRules());
        var operations = algebraMapperService.mapOperations(request.getOperations());

        var argumentativeGraph = inferenceService.buildGraph(facts, rules, operations);
        GraphResponse graphResponse = graphBuilderService.toGraphResponse(argumentativeGraph);
        NarrativeTraceResponse trace = narrativeTraceBuilderService.build(graphResponse);

        LlmNarrativeService.GenerationResult generated = llmNarrativeService.generateNarrative(trace);

        NarrationMetaResponse meta = new NarrationMetaResponse();
        meta.setModel(generated.model());
        meta.setPromptVersion(generated.promptVersion());
        meta.setGeneratedAt(Instant.now().toString());

        GraphProcessResponse response = new GraphProcessResponse();
        response.setGraph(graphResponse);
        response.setTrace(trace);
        response.setNarrative(generated.narrative());
        response.setMeta(meta);
        return response;
    }
}
