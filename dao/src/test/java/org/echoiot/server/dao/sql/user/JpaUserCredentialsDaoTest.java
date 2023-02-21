package org.echoiot.server.dao.sql.user;

import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.security.UserCredentials;
import org.echoiot.server.dao.service.AbstractServiceTest;
import org.echoiot.server.dao.user.UserCredentialsDao;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.echoiot.server.dao.AbstractJpaDaoTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Valerii Sosliuk on 4/22/2017.
 */
public class JpaUserCredentialsDaoTest extends AbstractJpaDaoTest {

    public static final String ACTIVATE_TOKEN = "ACTIVATE_TOKEN_0";
    public static final String RESET_TOKEN = "RESET_TOKEN_0";
    public static final int COUNT_USER_CREDENTIALS = 2;
    List<UserCredentials> userCredentialsList;
    UserCredentials neededUserCredentials;

    @Resource
    private UserCredentialsDao userCredentialsDao;

    @Before
    public void setUp() {
        userCredentialsList = new ArrayList<>();
        for (int i=0; i<COUNT_USER_CREDENTIALS; i++) {
            userCredentialsList.add(createUserCredentials(i));
        }
        neededUserCredentials = userCredentialsList.get(0);
        assertNotNull(neededUserCredentials);
    }

    UserCredentials createUserCredentials(int number) {
        @NotNull UserCredentials userCredentials = new UserCredentials();
        userCredentials.setEnabled(true);
        userCredentials.setUserId(new UserId(UUID.randomUUID()));
        userCredentials.setPassword("password");
        userCredentials.setActivateToken("ACTIVATE_TOKEN_" + number);
        userCredentials.setResetToken("RESET_TOKEN_" + number);
        return userCredentialsDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, userCredentials);
    }

    @After
    public void after() {
        for (@NotNull UserCredentials userCredentials : userCredentialsList) {
            userCredentialsDao.removeById(TenantId.SYS_TENANT_ID, userCredentials.getUuidId());
        }
    }

    @Test
    public void testFindAll() {
        List<UserCredentials> userCredentials = userCredentialsDao.find(AbstractServiceTest.SYSTEM_TENANT_ID);
        assertEquals(COUNT_USER_CREDENTIALS + 1, userCredentials.size());
    }

    @Test
    public void testFindByUserId() {
        UserCredentials foundedUserCredentials = userCredentialsDao.findByUserId(AbstractServiceTest.SYSTEM_TENANT_ID, neededUserCredentials.getUserId().getId());
        assertNotNull(foundedUserCredentials);
        assertEquals(neededUserCredentials, foundedUserCredentials);
    }

    @Test
    public void testFindByActivateToken() {
        UserCredentials foundedUserCredentials = userCredentialsDao.findByActivateToken(AbstractServiceTest.SYSTEM_TENANT_ID, ACTIVATE_TOKEN);
        assertNotNull(foundedUserCredentials);
        Assert.assertEquals(neededUserCredentials.getId(), foundedUserCredentials.getId());
    }

    @Test
    public void testFindByResetToken() {
        UserCredentials foundedUserCredentials = userCredentialsDao.findByResetToken(AbstractServiceTest.SYSTEM_TENANT_ID, RESET_TOKEN);
        assertNotNull(foundedUserCredentials);
        Assert.assertEquals(neededUserCredentials.getId(), foundedUserCredentials.getId());
    }
}
