package com.matriz.api;

import com.matriz.shared.Texts;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator of {@link Chars}: counts code points. With {@code notBlank} (names and titles) the rule of
 * {@link Texts#check} applies: a text of only spaces gets its own message, and the minimum counts the text without
 * the spaces at its ends. A missing value is valid (use @NotNull for that).
 */
class CharsValidator implements ConstraintValidator<Chars, CharSequence> {

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
        var problem = Texts.check(value.toString(), min, max, notBlank);
        if (problem.isEmpty()) {
            return true;
        }
        if (problem.get() == Texts.Problem.ONLY_SPACES) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(Texts.ONLY_SPACES_MESSAGE).addConstraintViolation();
        }
        return false; // the other problems keep the message of the annotation
    }
}
