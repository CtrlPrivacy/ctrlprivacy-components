package org.ctrlprivacy.web.error;

import lombok.Value;

/**
 * One reason a request failed validation, listed in a {@code VALIDATION_FAILED} problem's
 * {@code errors}.
 */
@Value
public class InvalidField {

    /** The field or parameter, such as {@code title}, or the object's name for a rule on the whole. */
    private final String field;

    /** Why it's invalid, such as {@code must not be blank}. */
    private final String message;
}
