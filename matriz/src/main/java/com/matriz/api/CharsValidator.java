package com.matriz.api;

import com.matriz.shared.Texts;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator of {@link Chars}: counts code points. With {@code notBlank} (names and titles) the minimum counts the
 * text without the spaces at its ends, and a text of only spaces is refused with its own message. A missing value is
 * valid (use @NotNull for that).
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
        if (notBlank && characters > 0 && characters <= max && Texts.isOnlySpaces(value)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(BLANK_MESSAGE).addConstraintViolation();
            return false;
        }
        int counted = notBlank ? Texts.visibleLength(value.toString()) : characters;
        return counted >= min && characters <= max;
    }
}
