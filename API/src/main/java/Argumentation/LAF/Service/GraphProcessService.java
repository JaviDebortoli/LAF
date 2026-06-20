package Argumentation.LAF.Service;

import Argumentation.LAF.DTO.Request.GraphRequest;
import Argumentation.LAF.DTO.Response.ExplainabilityResponse;
import Argumentation.LAF.DTO.Response.GraphProcessResponse;
import Argumentation.LAF.DTO.Response.GraphResponse;
import Argumentation.LAF.DTO.Response.NarrationMetaResponse;
import Argumentation.LAF.DTO.Response.NarrativeTraceResponse;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GraphProcessService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphProcessService.class);
    private static final String EXPLAINABILITY_STATUS_OK = "ok";
    private static final String EXPLAINABILITY_STATUS_DISABLED = "disabled";
    private static final String EXPLAINABILITY_STATUS_UNAVAILABLE = "unavailable";
    private static final String EXPLAINABILITY_DISABLED_MESSAGE = "Explainability is disabled for this run.";

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
        LOGGER.info(
                "Starting graph process flow factsCount={} rulesCount={}",
                request.getFacts().size(),
                request.getRules().size());

        var facts = programMapperService.mapFacts(request.getFacts());
        var rules = programMapperService.mapRules(request.getRules());
        var operations = algebraMapperService.mapOperations(request.getOperations());

        var argumentativeGraph = inferenceService.buildGraph(facts, rules, operations);
        GraphResponse graphResponse = graphBuilderService.toGraphResponse(argumentativeGraph);

        GraphProcessResponse response = new GraphProcessResponse();
        response.setGraph(graphResponse);

        if (!request.isExplainabilityEnabledOrDefault()) {
            response.setExplainability(buildExplainability(
                    false,
                    EXPLAINABILITY_STATUS_DISABLED,
                    EXPLAINABILITY_DISABLED_MESSAGE));
            LOGGER.info("Explainability skipped by request configuration");
            LOGGER.info(
                    "Finished graph process flow nodesCount={} edgesCount={}",
                    graphResponse.getNodes().size(),
                    graphResponse.getEdges().size());
            return response;
        }

        NarrativeTraceResponse trace = narrativeTraceBuilderService.build(graphResponse);
        response.setTrace(trace);

        try {
            LlmNarrativeService.GenerationResult generated = llmNarrativeService.generateNarrative(trace);
            LOGGER.info(
                    "Narrative generated model={} promptVersion={}",
                    generated.model(),
                    generated.promptVersion());

            NarrationMetaResponse meta = new NarrationMetaResponse();
            meta.setModel(generated.model());
            meta.setPromptVersion(generated.promptVersion());
            meta.setGeneratedAt(Instant.now().toString());

            response.setNarrative(generated.narrative());
            response.setMeta(meta);
            response.setExplainability(buildExplainability(true, EXPLAINABILITY_STATUS_OK, null));
        } catch (NarrativeServiceUnavailableException exception) {
            LOGGER.warn("Explainability unavailable errorType={}", exception.getClass().getSimpleName());
            response.setExplainability(buildExplainability(
                    true,
                    EXPLAINABILITY_STATUS_UNAVAILABLE,
                    LlmNarrativeService.TEMPORARILY_UNAVAILABLE_MESSAGE));
        }

        LOGGER.info(
                "Finished graph process flow nodesCount={} edgesCount={}",
                graphResponse.getNodes().size(),
                graphResponse.getEdges().size());
        return response;
    }

    private ExplainabilityResponse buildExplainability(boolean enabled, String status, String message) {
        ExplainabilityResponse explainability = new ExplainabilityResponse();
        explainability.setEnabled(enabled);
        explainability.setStatus(status);
        explainability.setMessage(message);
        return explainability;
    }
}
