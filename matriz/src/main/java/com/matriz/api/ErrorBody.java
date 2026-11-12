package com.matriz.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Error body of the contract: a code and a message that says why and what to do (NF08). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorBody(String code, String message, List<Detail> details) {

    public record Detail(String field, String issue) {
    }

    public static ErrorBody of(String code, String message) {
        return new ErrorBody(code, message, null);
    }
}
