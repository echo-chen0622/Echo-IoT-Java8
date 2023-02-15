package org.thingsboard.server.dao.service;

import org.junit.Test;
import org.thingsboard.server.dao.exception.DataValidationException;

public class DataValidatorTest {

    @Test
    public void validateEmail() {
        String email = "aZ1_!#$%&'*+/=?`{|}~^.-@mail.io";
        DataValidator.validateEmail(email);
    }

    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail1() {
        String email = "test:1@mail.io";
        DataValidator.validateEmail(email);
    }
    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail2() {
        String email = "test()1@mail.io";
        DataValidator.validateEmail(email);
    }

    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail3() {
        String email = "test[]1@mail.io";
        DataValidator.validateEmail(email);
    }

    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail4() {
        String email = "test\\1@mail.io";
        DataValidator.validateEmail(email);
    }

    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail5() {
        String email = "test\"1@mail.io";
        DataValidator.validateEmail(email);
    }

    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail6() {
        String email = "test<>1@mail.io";
        DataValidator.validateEmail(email);
    }
}
