package org.echoiot.server.dao.service;

import org.echoiot.server.dao.exception.DataValidationException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class DataValidatorTest {

    @Test
    public void validateEmail() {
        @NotNull String email = "aZ1_!#$%&'*+/=?`{|}~^.-@mail.io";
        DataValidator.validateEmail(email);
    }

    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail1() {
        @NotNull String email = "test:1@mail.io";
        DataValidator.validateEmail(email);
    }
    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail2() {
        @NotNull String email = "test()1@mail.io";
        DataValidator.validateEmail(email);
    }

    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail3() {
        @NotNull String email = "test[]1@mail.io";
        DataValidator.validateEmail(email);
    }

    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail4() {
        @NotNull String email = "test\\1@mail.io";
        DataValidator.validateEmail(email);
    }

    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail5() {
        @NotNull String email = "test\"1@mail.io";
        DataValidator.validateEmail(email);
    }

    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail6() {
        @NotNull String email = "test<>1@mail.io";
        DataValidator.validateEmail(email);
    }
}
