package com.srm.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiEnabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentationCanBeEnabledExplicitlyOutsideProduction() throws Exception {
        mockMvc.perform(get("/v3/api-docs/internal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("SRM 内部管理端 API"))
                .andExpect(content().string(containsString("/api/v1/workbench/baseline")));
        mockMvc.perform(get("/v3/api-docs/supplier"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("SRM 供应商端 API"))
                .andExpect(jsonPath("$.paths").isMap());
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
