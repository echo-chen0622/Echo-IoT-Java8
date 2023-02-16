package org.echoiot.server.dao.sqlts.dictionary;

import org.echoiot.server.dao.model.sqlts.dictionary.TsKvDictionary;
import org.echoiot.server.dao.model.sqlts.dictionary.TsKvDictionaryCompositeKey;
import org.echoiot.server.dao.util.SqlTsOrTsLatestAnyDao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@SqlTsOrTsLatestAnyDao
public interface TsKvDictionaryRepository extends JpaRepository<TsKvDictionary, TsKvDictionaryCompositeKey> {

    Optional<TsKvDictionary> findByKeyId(int keyId);

}
