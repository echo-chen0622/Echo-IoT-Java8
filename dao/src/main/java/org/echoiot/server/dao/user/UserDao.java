package org.echoiot.server.dao.user;

import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.Dao;
import org.echoiot.server.dao.TenantEntityDao;

import java.util.UUID;

public interface UserDao extends Dao<User>, TenantEntityDao {

    /**
     * Save or update user object
     *
     * @param user the user object
     * @return saved user entity
     */
    User save(TenantId tenantId, User user);

    /**
     * Find user by email.
     *
     * @param email the email
     * @return the user entity
     */
    User findByEmail(TenantId tenantId, String email);

    /**
     * Find user by tenant id and email.
     *
     * @param tenantId the tenant id
     * @param email the email
     * @return the user entity
     */
    User findByTenantIdAndEmail(TenantId tenantId, String email);

    /**
     * Find users by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of user entities
     */
    PageData<User> findByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find tenant admin users by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of user entities
     */
    PageData<User> findTenantAdmins(UUID tenantId, PageLink pageLink);

    /**
     * Find customer users by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of user entities
     */
    PageData<User> findCustomerUsers(UUID tenantId, UUID customerId, PageLink pageLink);

}
