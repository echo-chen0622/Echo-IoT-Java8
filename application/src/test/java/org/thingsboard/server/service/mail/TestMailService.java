package org.thingsboard.server.service.mail;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.exception.ThingsboardException;

@Profile("test")
@Configuration
public class TestMailService {

    public static String currentActivateToken;
    public static String currentResetPasswordToken;

    @Bean
    @Primary
    public MailService mailService() throws ThingsboardException {
        MailService mailService = Mockito.mock(MailService.class);
        Mockito.doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                String activationLink = (String) args[0];
                currentActivateToken = activationLink.split("=")[1];
                return null;
            }
        }).when(mailService).sendActivationEmail(Mockito.anyString(), Mockito.anyString());
        Mockito.doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                String passwordResetLink = (String) args[0];
                currentResetPasswordToken = passwordResetLink.split("=")[1];
                return null;
            }
        }).when(mailService).sendResetPasswordEmailAsync(Mockito.anyString(), Mockito.anyString());
        return mailService;
    }

}
