package com.matriz.shared;

import java.util.Arrays;
import java.util.Optional;

/** An enum whose values have a label in the contract (for example "In progress"). */
public interface Labelled {

    String label();

    /** The value of the enum with this label, if there is one. */
    static <E extends Enum<E> & Labelled> Optional<E> find(Class<E> type, String label) {
        return Arrays.stream(type.getEnumConstants()).filter(e -> e.label().equals(label)).findFirst();
    }
}
