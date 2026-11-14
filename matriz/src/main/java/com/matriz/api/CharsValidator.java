package com.matriz.api;

import com.matriz.shared.Texts;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator of {@link Chars}: counts code points, and refuses a text of only blank characters when asked to. A
 * missing value is valid (use @NotNull for that). The message says which of the two was broken.
 */
class CharsValidator implements ConstraintValidator<Chars, CharSequence> {

    static final String BLANK_MESSAGE = "Must not be only spaces.";

    private int min;
    private int max;
    private boolean notBlank;

    @Override
    public void initialize(Chars annotation) {
        this.min = annotation.min();
        this.max = annotation.max();
        this.notBlank = annotation.notBlank();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int characters = Character.codePointCount(value, 0, value.length());
        if (characters >= min && characters <= max && !(notBlank && Texts.isOnlySpaces(value))) {
            return true;
        }
        if (characters >= min && characters <= max) { // the length is fine, so it is the blank text
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(BLANK_MESSAGE).addConstraintViolation();
        }
        return false;
    }
}
