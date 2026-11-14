package com.matriz.contract;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.models.media.Schema;

/**
 * Checks a JSON value against a schema of the contract, once the parser has resolved the references. It is a
 * small checker made for this contract, not a full JSON Schema validator: it covers what the contract uses
 * (types, including the "null" of OpenAPI 3.1, enumerations, required properties, items, oneOf, allOf, the
 * formats date, date-time and time, and minimum, maximum and length limits), and it also refuses properties the
 * contract does not list, which is what shows the contract and the answers drifting apart.
 */
final class SchemaChecker {

    private SchemaChecker() {
    }

    /** The problems found, each with the path in the value where it is; empty when the value fits the schema. */
    static List<String> check(JsonNode value, Schema<?> schema) {
        return check(value, schema, Map.of());
    }

    /**
     * As above. A schema that refers to itself (a task has subtasks that are tasks) is left as a reference by
     * the parser, so the schemas of the contract are given by name to follow those references.
     */
    static List<String> check(JsonNode value, Schema<?> schema, Map<String, Schema> components) {
        List<String> problems = new ArrayList<>();
        check(value, schema, "$", problems, components);
        return problems;
    }

    private static void check(JsonNode value, Schema<?> schema, String path, List<String> problems,
            Map<String, Schema> components) {
        if (schema == null) {
            return;
        }
        if (schema.get$ref() != null) {
            Schema<?> target = components.get(schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1));
            if (target == null) {
                problems.add(path + ": the reference " + schema.get$ref() + " cannot be followed");
            } else {
                check(value, target, path, problems, components);
            }
            return;
        }
        if (schema.getOneOf() != null && !schema.getOneOf().isEmpty()) {
            boolean anyFits = schema.getOneOf().stream().anyMatch(option -> check(value, option, components).isEmpty());
            if (!anyFits) {
                problems.add(path + ": the value fits none of the oneOf options (" + shortText(value) + ")");
            }
            return;
        }
        Set<String> types = typesOf(schema);
        if (!types.isEmpty() && types.stream().noneMatch(type -> hasType(value, type))) {
            problems.add(path + ": expected " + types + " but found " + value.getNodeType() + " (" + shortText(value) + ")");
            return;
        }
        if (value.isNull()) {
            checkEnum(value, schema, path, problems);
            return;
        }
        checkEnum(value, schema, path, problems);
        checkFormat(value, schema, path, problems);
        if (value.isNumber()) {
            if (schema.getMinimum() != null && value.decimalValue().compareTo(schema.getMinimum()) < 0) {
                problems.add(path + ": " + value + " is below the minimum " + schema.getMinimum());
            }
            if (schema.getMaximum() != null && value.decimalValue().compareTo(schema.getMaximum()) > 0) {
                problems.add(path + ": " + value + " is above the maximum " + schema.getMaximum());
            }
        }
        if (value.isTextual()) {
            int length = value.asText().codePointCount(0, value.asText().length());
            if (schema.getMaxLength() != null && length > schema.getMaxLength()) {
                problems.add(path + ": " + length + " characters, more than the " + schema.getMaxLength() + " allowed");
            }
            if (schema.getMinLength() != null && length < schema.getMinLength()) {
                problems.add(path + ": " + length + " characters, fewer than the " + schema.getMinLength() + " required");
            }
        }
        if (value.isObject()) {
            checkObject(value, schema, path, problems, components);
        }
        if (value.isArray() && schema.getItems() != null) {
            for (int i = 0; i < value.size(); i++) {
                check(value.get(i), schema.getItems(), path + "[" + i + "]", problems, components);
            }
        }
    }

    /** An object schema with the parts of its allOf put together: what the object may have and must have. */
    private record Shape(Map<String, Schema<?>> properties, Set<String> required, boolean additionalAllowed) {
    }

    private static Shape shapeOf(Schema<?> schema) {
        Map<String, Schema<?>> properties = new LinkedHashMap<>();
        Set<String> required = new LinkedHashSet<>();
        boolean[] additional = {additionalPropertiesAllowed(schema)};
        collect(schema, properties, required, additional);
        return new Shape(properties, required, additional[0]);
    }

    private static void collect(Schema<?> schema, Map<String, Schema<?>> properties, Set<String> required, boolean[] additional) {
        if (schema.getProperties() != null) {
            schema.getProperties().forEach((name, property) -> properties.put(name, property));
        }
        if (schema.getRequired() != null) {
            required.addAll(schema.getRequired());
        }
        additional[0] |= additionalPropertiesAllowed(schema);
        if (schema.getAllOf() != null) {
            schema.getAllOf().forEach(part -> collect(part, properties, required, additional));
        }
    }

    private static void checkObject(JsonNode value, Schema<?> schema, String path, List<String> problems,
            Map<String, Schema> components) {
        Shape shape = shapeOf(schema);
        for (String name : shape.required()) {
            if (!value.has(name)) {
                problems.add(path + "." + name + ": required by the contract but missing");
            }
        }
        value.fieldNames().forEachRemaining(name -> {
            Schema<?> property = shape.properties().get(name);
            if (property != null) {
                check(value.get(name), property, path + "." + name, problems, components);
            } else if (!shape.additionalAllowed()) {
                problems.add(path + "." + name + ": the contract does not list this property");
            }
        });
    }

    private static boolean additionalPropertiesAllowed(Schema<?> schema) {
        Object additional = schema.getAdditionalProperties();
        return additional instanceof Schema || Boolean.TRUE.equals(additional);
    }

    private static void checkEnum(JsonNode value, Schema<?> schema, String path, List<String> problems) {
        List<?> allowed = schema.getEnum();
        if (allowed == null || allowed.isEmpty()) {
            return;
        }
        boolean found = allowed.stream().anyMatch(option -> option == null ? value.isNull()
                : !value.isNull() && option.toString().equals(value.asText()));
        if (!found) {
            problems.add(path + ": " + shortText(value) + " is not one of " + allowed);
        }
    }

    private static void checkFormat(JsonNode value, Schema<?> schema, String path, List<String> problems) {
        if (!value.isTextual() || schema.getFormat() == null) {
            return;
        }
        try {
            switch (schema.getFormat()) {
                case "date" -> LocalDate.parse(value.asText());
                case "date-time" -> OffsetDateTime.parse(value.asText());
                case "time" -> LocalTime.parse(value.asText());
                default -> {
                }
            }
        } catch (RuntimeException e) {
            problems.add(path + ": " + shortText(value) + " is not a valid " + schema.getFormat());
        }
    }

    private static Set<String> typesOf(Schema<?> schema) {
        Set<String> types = new LinkedHashSet<>();
        if (schema.getTypes() != null) {
            types.addAll(schema.getTypes());
        } else if (schema.getType() != null) {
            types.add(schema.getType());
            if (Boolean.TRUE.equals(schema.getNullable())) {
                types.add("null");
            }
        }
        return types;
    }

    private static boolean hasType(JsonNode value, String type) {
        return switch (type) {
            case "null" -> value.isNull();
            case "string" -> value.isTextual();
            case "integer" -> value.isIntegralNumber();
            case "number" -> value.isNumber();
            case "boolean" -> value.isBoolean();
            case "array" -> value.isArray();
            case "object" -> value.isObject();
            default -> true;
        };
    }

    private static String shortText(JsonNode value) {
        String text = value.toString();
        return text.length() > 60 ? text.substring(0, 60) + "..." : text;
    }
}
