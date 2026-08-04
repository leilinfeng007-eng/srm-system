package com.srm.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractExportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void exportsDeterministicInternalAndSupplierContractsFromActualControllers() throws Exception {
        Path outputDirectory = Path.of("target", "generated-openapi");
        Files.createDirectories(outputDirectory);

        JsonNode internal = export("/v3/api-docs/internal");
        JsonNode supplier = export("/v3/api-docs/supplier");
        assertThat(internal.path("paths").has("/api/v1/workbench/baseline")).isTrue();
        assertThat(supplier.path("paths").isEmpty()).isTrue();
        internal.path("paths").forEach(path -> path.forEach(operation ->
                assertThat(operation.path("summary").asText())
                        .as("Every exported operation has a useful summary")
                        .isNotBlank()));

        write(outputDirectory.resolve("internal-api.json"), internal);
        write(outputDirectory.resolve("supplier-api.json"), supplier);
    }

    private JsonNode export(String path) throws Exception {
        String content = mockMvc.perform(get(path))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return sort(objectMapper.readTree(content));
    }

    private void write(Path output, JsonNode contract) throws Exception {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), contract);
        Files.writeString(output, Files.readString(output) + System.lineSeparator());
    }

    private JsonNode sort(JsonNode node) {
        if (node instanceof ObjectNode object) {
            ObjectNode sorted = objectMapper.createObjectNode();
            List<String> names = new ArrayList<>();
            object.fieldNames().forEachRemaining(names::add);
            names.stream().sorted(Comparator.naturalOrder())
                    .forEach(name -> sorted.set(name, sort(object.get(name))));
            return sorted;
        }
        if (node instanceof ArrayNode array) {
            ArrayNode sorted = objectMapper.createArrayNode();
            array.forEach(value -> sorted.add(sort(value)));
            return sorted;
        }
        return node;
    }
}
