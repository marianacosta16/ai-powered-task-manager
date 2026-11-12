package com.matriz.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

class OpenApiContractTest {

    @Test
    void openapiYamlIsValid() {
        // Given the contract file at the project root
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        // When it is parsed
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation("openapi.yaml", null, options);

        // Then there are no errors and it has the 28 operations of v1
        assertThat(result.getMessages()).isEmpty();
        assertThat(result.getOpenAPI()).isNotNull();
        long operations = result.getOpenAPI().getPaths().values().stream()
                .mapToLong(p -> p.readOperations().size()).sum();
        assertThat(operations).isEqualTo(28);
    }
}
