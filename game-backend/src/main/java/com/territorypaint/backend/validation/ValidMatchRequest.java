package com.territorypaint.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for SaveMatchRequest.
 * Validates complex constraints across multiple fields.
 * Requirements: 9.6, 9.7
 */
@Constraint(validatedBy = MatchRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMatchRequest {
    
    String message() default "Invalid match request";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
