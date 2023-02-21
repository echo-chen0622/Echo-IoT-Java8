package org.echoiot.server.dao.sql.oauth2;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.dao.model.sql.OAuth2ClientRegistrationTemplateEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.echoiot.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.oauth2.OAuth2ClientRegistrationTemplateDao;
import org.echoiot.server.dao.sql.JpaAbstractDao;
import org.echoiot.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@SqlDao
public class JpaOAuth2ClientRegistrationTemplateDao extends JpaAbstractDao<OAuth2ClientRegistrationTemplateEntity, OAuth2ClientRegistrationTemplate> implements OAuth2ClientRegistrationTemplateDao {
    @NotNull
    private final OAuth2ClientRegistrationTemplateRepository repository;

    @NotNull
    @Override
    protected Class<OAuth2ClientRegistrationTemplateEntity> getEntityClass() {
        return OAuth2ClientRegistrationTemplateEntity.class;
    }

    @Override
    protected JpaRepository<OAuth2ClientRegistrationTemplateEntity, UUID> getRepository() {
        return repository;
    }

    @NotNull
    @Override
    public Optional<OAuth2ClientRegistrationTemplate> findByProviderId(String providerId) {
        OAuth2ClientRegistrationTemplate oAuth2ClientRegistrationTemplate = DaoUtil.getData(repository.findByProviderId(providerId));
        return Optional.ofNullable(oAuth2ClientRegistrationTemplate);
    }

    @NotNull
    @Override
    public List<OAuth2ClientRegistrationTemplate> findAll() {
        @NotNull Iterable<OAuth2ClientRegistrationTemplateEntity> entities = repository.findAll();
        @NotNull List<OAuth2ClientRegistrationTemplate> result = new ArrayList<>();
        entities.forEach(entity -> result.add(DaoUtil.getData(entity)));
        return result;
    }
}
