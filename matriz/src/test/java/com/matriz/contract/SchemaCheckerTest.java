package com.matriz.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

/** The checker itself: it must find what is wrong, or the conformance test would pass for nothing. */
class SchemaCheckerTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Schema<?> PERSON = schema("""
            type: object
            required: [id, name]
            properties:
              id: {type: integer, minimum: 1}
              name: {type: string, maxLength: 5}
              role: {type: string, enum: [Manager, Member]}
              born: {type: string, format: date}
              nick: {type: [string, 'null']}
              tags: {type: array, items: {type: string}}
            """);

    @Test
    void aValueThatFitsHasNoProblems() {
        assertThat(check("{\"id\":1,\"name\":\"Ana\",\"role\":\"Member\",\"born\":\"2000-01-31\",\"nick\":null,\"tags\":[\"a\"]}"))
                .isEmpty();
    }

    @Test
    void aMissingRequiredPropertyIsFound() {
        assertThat(check("{\"id\":1}")).containsExactly("$.name: required by the contract but missing");
    }

    @Test
    void aPropertyTheContractDoesNotListIsFound() {
        assertThat(check("{\"id\":1,\"name\":\"Ana\",\"extra\":true}"))
                .containsExactly("$.extra: the contract does not list this property");
    }

    @Test
    void aWrongTypeNullEnumFormatAndLimitsAreFound() {
        assertThat(check("{\"id\":\"1\",\"name\":\"Anabela\",\"role\":\"Boss\",\"born\":\"31/01/2000\",\"nick\":5}"))
                .hasSize(5);
        assertThat(check("{\"id\":0,\"name\":null}")).hasSize(2); // below the minimum; null is not a string here
    }

    @Test
    void aNullIsAcceptedOnlyWhereTheTypesAllowIt() {
        assertThat(check("{\"id\":1,\"name\":\"Ana\",\"nick\":null}")).isEmpty();
        assertThat(check("{\"id\":1,\"name\":\"Ana\",\"tags\":[\"a\",3]}")).hasSize(1);
    }

    @Test
    void oneOfNeedsOneOptionToFit() {
        Schema<?> either = schema("""
                oneOf:
                  - {type: integer}
                  - {type: 'null'}
                """);
        assertThat(SchemaChecker.check(json("5"), either)).isEmpty();
        assertThat(SchemaChecker.check(json("null"), either)).isEmpty();
        assertThat(SchemaChecker.check(json("\"x\""), either)).hasSize(1);
    }

    @Test
    void thePartsOfAnAllOfAreOneObject() {
        Schema<?> both = schema("""
                allOf:
                  - type: object
                    required: [id]
                    properties: {id: {type: integer}}
                  - type: object
                    required: [name]
                    properties: {name: {type: string}}
                """);
        assertThat(SchemaChecker.check(json("{\"id\":1,\"name\":\"Ana\"}"), both)).isEmpty();
        assertThat(SchemaChecker.check(json("{\"id\":1}"), both)).containsExactly("$.name: required by the contract but missing");
        assertThat(SchemaChecker.check(json("{\"id\":1,\"name\":\"Ana\",\"x\":1}"), both))
                .containsExactly("$.x: the contract does not list this property");
    }

    private static List<String> check(String json) {
        return SchemaChecker.check(json(json), PERSON);
    }

    private static JsonNode json(String text) {
        try {
            return JSON.readTree(text);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /** A schema written in YAML, read by the same parser as the contract. */
    private static Schema<?> schema(String yaml) {
        String indented = yaml.lines().map(line -> "        " + line).reduce("", (a, b) -> a + b + "\n");
        String document = "openapi: 3.1.0\ninfo: {title: t, version: '1'}\npaths: {}\ncomponents:\n  schemas:\n    S:\n" + indented;
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        return new OpenAPIV3Parser().readContents(document, null, options).getOpenAPI().getComponents().getSchemas().get("S");
    }
}
