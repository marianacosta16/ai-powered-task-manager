package com.matriz.api;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.matriz.shared.Page;

/** Answers with one page of a list: the items in the body and the size of the whole list in {@code X-Total-Count} (A31). */
final class Pages {

    static final String TOTAL_HEADER = "X-Total-Count";

    private Pages() {
    }

    static <T> ResponseEntity<List<T>> of(Page<T> page) {
        return ResponseEntity.ok().header(TOTAL_HEADER, Long.toString(page.total())).body(page.items());
    }
}
