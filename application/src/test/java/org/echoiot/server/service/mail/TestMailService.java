package org.echoiot.server.service.mail;

import org.echoiot.rule.engine.api.MailService;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Configuration
public class TestMailService {

    public static String currentActivateToken;
    public static String currentResetPasswordToken;

    @Bean
    @Primary
    public MailService mailService() throws EchoiotException {
        MailService mailService = Mockito.mock(MailService.class);
        Mockito.doAnswer(new Answer<Void>() {
            @Nullable
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                String activationLink = (String) args[0];
                currentActivateToken = activationLink.split("=")[1];
                return null;
            }
        }).when(mailService).sendActivationEmail(Mockito.anyString(), Mockito.anyString());
        Mockito.doAnswer(new Answer<Void>() {
            @Nullable
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
