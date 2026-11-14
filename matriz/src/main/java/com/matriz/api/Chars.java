package com.matriz.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * The length of a text in characters, as R14 counts them: letters, punctuation, spaces and symbols,
 * each one once. {@code @Size} counts UTF-16 units, so an emoji would count twice and a text that
 * fits in the database column would be refused.
 */
@Documented
@Constraint(validatedBy = CharsValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
@interface Chars {

    String message() default "Invalid length.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int min() default 0;

    int max() default Integer.MAX_VALUE;
}
