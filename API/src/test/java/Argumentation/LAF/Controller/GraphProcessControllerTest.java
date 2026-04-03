package Argumentation.LAF.Controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class GraphProcessControllerTest {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldReturnServiceUnavailableWhenLlmIsNotConfigured() throws Exception {
        mockMvc.perform(post("/api/graph/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("Narrative generation service is temporarily unavailable."))
                .andExpect(jsonPath("$.path").value("/api/graph/process"));
    }

    @Test
    void shouldReturnBadRequestWhenFactsListIsEmpty() throws Exception {
        mockMvc.perform(post("/api/graph/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildInvalidRequestJsonWithEmptyFacts()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", Matchers.containsString("facts")))
                .andExpect(jsonPath("$.path").value("/api/graph/process"));
    }

    @Test
    void shouldReturnBadRequestForGraphEndpointWhenFactsListIsEmpty() throws Exception {
        mockMvc.perform(post("/api/graph")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildInvalidRequestJsonWithEmptyFacts()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", Matchers.containsString("facts")))
                .andExpect(jsonPath("$.path").value("/api/graph"));
    }

    private String buildRequestJson() {
        return """
                {
                  "facts": [
                    {"name":"basicServices","argument":"houseA","attributes":["0.75","0.95"],"sourceKey":"FACT|basicServices|houseA"},
                    {"name":"gangOperate","argument":"houseA","attributes":["0.5","1.0"],"sourceKey":"FACT|gangOperate|houseA"}
                  ],
                  "rules": [
                    {"headName":"goodArea","bodyLiterals":["basicServices"],"attributes":["0.85","0.95"],"sourceKey":"RULE|goodArea|basicServices"},
                    {"headName":"~goodArea","bodyLiterals":["gangOperate"],"attributes":["0.5","0.8"],"sourceKey":"RULE|~goodArea|gangOperate"}
                  ],
                  "operations": {
                    "labels": [
                      {"labelName":"label_1","supportFunction":"X + Y","aggregationFunction":"X + Y","conflictFunction":"max(X-Y,0)"},
                      {"labelName":"label_2","supportFunction":"X + Y","aggregationFunction":"X + Y","conflictFunction":"max(X-Y,0)"}
                    ]
                  }
                }
                """;
    }

    private String buildInvalidRequestJsonWithEmptyFacts() {
        return """
                {
                  "facts": [],
                  "rules": [
                    {"headName":"goodArea","bodyLiterals":["basicServices"],"attributes":["0.85","0.95"],"sourceKey":"RULE|goodArea|basicServices"}
                  ],
                  "operations": {
                    "labels": [
                      {"labelName":"label_1","supportFunction":"X + Y","aggregationFunction":"X + Y","conflictFunction":"max(X-Y,0)"}
                    ]
                  }
                }
                """;
    }
}
