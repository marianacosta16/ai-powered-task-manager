package com.matriz.api;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validator of {@link Chars}: counts code points. A missing value is valid (use @NotNull for that). */
class CharsValidator implements ConstraintValidator<Chars, CharSequence> {

    private int min;
    private int max;

    @Override
    public void initialize(Chars annotation) {
        this.min = annotation.min();
        this.max = annotation.max();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int characters = Character.codePointCount(value, 0, value.length());
        return characters >= min && characters <= max;
    }
}
