package org.echoiot.server.dao.sql.user;

import org.echoiot.server.dao.model.sql.UserEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.sql.JpaAbstractSearchTextDao;
import org.echoiot.server.dao.user.UserDao;
import org.echoiot.server.dao.util.SqlDao;

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

    @NotNull
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
    public User findByTenantIdAndEmail(@NotNull TenantId tenantId, String email) {
        return DaoUtil.getData(userRepository.findByTenantIdAndEmail(tenantId.getId(), email));
    }

    @NotNull
    @Override
    public PageData<User> findByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findByTenantId(
                                tenantId,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<User> findTenantAdmins(UUID tenantId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findUsersByAuthority(
                                tenantId,
                                NULL_UUID,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                Authority.TENANT_ADMIN,
                                DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<User> findCustomerUsers(UUID tenantId, UUID customerId, @NotNull PageLink pageLink) {
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
    public Long countByTenantId(@NotNull TenantId tenantId) {
        return userRepository.countByTenantId(tenantId.getId());
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.USER;
    }

}
