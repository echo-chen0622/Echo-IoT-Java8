package org.echoiot.server.dao.sql.user;

import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.model.sql.UserEntity;
import org.echoiot.server.dao.sql.JpaAbstractSearchTextDao;
import org.echoiot.server.dao.user.UserDao;
import org.echoiot.server.dao.util.SqlDao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.UUID;

import static org.echoiot.server.dao.model.ModelConstants.NULL_UUID;

/**
 * @author Valerii Sosliuk
 */
@Component
@SqlDao
public class JpaUserDao extends JpaAbstractSearchTextDao<UserEntity, User> implements UserDao {

    @Resource
    private UserRepository userRepository;

    @Override
    protected Class<UserEntity> getEntityClass() {
        return UserEntity.class;
    }

    @Override
    protected JpaRepository<UserEntity, UUID> getRepository() {
        return userRepository;
    }

    @Override
    public User findByEmail(TenantId tenantId, String email) {
        return DaoUtil.getData(userRepository.findByEmail(email));
    }

    @Override
    public User findByTenantIdAndEmail(TenantId tenantId, String email) {
        return DaoUtil.getData(userRepository.findByTenantIdAndEmail(tenantId.getId(), email));
    }

    @Override
    public PageData<User> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findByTenantId(
                                tenantId,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<User> findTenantAdmins(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findUsersByAuthority(
                                tenantId,
                                NULL_UUID,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                Authority.TENANT_ADMIN,
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<User> findCustomerUsers(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findUsersByAuthority(
                                tenantId,
                                customerId,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                Authority.CUSTOMER_USER,
                                DaoUtil.toPageable(pageLink)));

    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return userRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.USER;
    }

}
