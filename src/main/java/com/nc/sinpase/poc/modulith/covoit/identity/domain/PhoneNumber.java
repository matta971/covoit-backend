package com.nc.sinpase.poc.modulith.covoit.identity.domain;

import java.util.Objects;

public record PhoneNumber(String dialCode, String number) {

    public PhoneNumber {
        Objects.requireNonNull(dialCode, "dialCode must not be null");
        Objects.requireNonNull(number, "number must not be null");
        if (dialCode.isBlank()) throw new IllegalArgumentException("dialCode must not be blank");
        if (number.isBlank()) throw new IllegalArgumentException("number must not be blank");
    }
}
