package org.echoiot.server.dao.sql.component;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.echoiot.server.common.data.id.ComponentDescriptorId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.plugin.ComponentDescriptor;
import org.echoiot.server.common.data.plugin.ComponentScope;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.component.ComponentDescriptorDao;
import org.echoiot.server.dao.model.sql.ComponentDescriptorEntity;
import org.echoiot.server.dao.sql.JpaAbstractSearchTextDao;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
public class JpaBaseComponentDescriptorDao extends JpaAbstractSearchTextDao<ComponentDescriptorEntity, ComponentDescriptor>
        implements ComponentDescriptorDao {

    @Resource
    private ComponentDescriptorRepository componentDescriptorRepository;

    @Resource
    private ComponentDescriptorInsertRepository componentDescriptorInsertRepository;

    @NotNull
    @Override
    protected Class<ComponentDescriptorEntity> getEntityClass() {
        return ComponentDescriptorEntity.class;
    }

    @Override
    protected JpaRepository<ComponentDescriptorEntity, UUID> getRepository() {
        return componentDescriptorRepository;
    }

    @NotNull
    @Override
    public Optional<ComponentDescriptor> saveIfNotExist(TenantId tenantId, @NotNull ComponentDescriptor component) {
        if (component.getId() == null) {
            @NotNull UUID uuid = Uuids.timeBased();
            component.setId(new ComponentDescriptorId(uuid));
            component.setCreatedTime(Uuids.unixTimestamp(uuid));
        }
        if (!componentDescriptorRepository.existsById(component.getId().getId())) {
            @NotNull ComponentDescriptorEntity componentDescriptorEntity = new ComponentDescriptorEntity(component);
            ComponentDescriptorEntity savedEntity = componentDescriptorInsertRepository.saveOrUpdate(componentDescriptorEntity);
            return Optional.of(savedEntity.toData());
        }
        return Optional.empty();
    }

    @Override
    public ComponentDescriptor findById(TenantId tenantId, @NotNull ComponentDescriptorId componentId) {
        return findById(tenantId, componentId.getId());
    }

    @Override
    public ComponentDescriptor findByClazz(TenantId tenantId, String clazz) {
        return DaoUtil.getData(componentDescriptorRepository.findByClazz(clazz));
    }

    @NotNull
    @Override
    public PageData<ComponentDescriptor> findByTypeAndPageLink(TenantId tenantId, ComponentType type, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(componentDescriptorRepository
                .findByType(
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<ComponentDescriptor> findByScopeAndTypeAndPageLink(TenantId tenantId, ComponentScope scope, ComponentType type, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(componentDescriptorRepository
                .findByScopeAndType(
                        type,
                        scope,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    @Transactional
    public void deleteById(TenantId tenantId, @NotNull ComponentDescriptorId componentId) {
        removeById(tenantId, componentId.getId());
    }

    @Override
    @Transactional
    public void deleteByClazz(TenantId tenantId, String clazz) {
        componentDescriptorRepository.deleteByClazz(clazz);
    }
}
