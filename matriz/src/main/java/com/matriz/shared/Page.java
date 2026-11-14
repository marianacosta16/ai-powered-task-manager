package com.matriz.shared;

import java.util.List;

/** One page of a list and the total number of items in the whole list, for the {@code X-Total-Count} header. */
public record Page<T>(List<T> items, long total) {
}
