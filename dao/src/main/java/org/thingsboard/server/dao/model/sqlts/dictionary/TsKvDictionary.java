package org.thingsboard.server.dao.model.sqlts.dictionary;

import lombok.Data;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import static org.thingsboard.server.dao.model.ModelConstants.KEY_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.KEY_ID_COLUMN;

@Data
@Entity
@Table(name = "ts_kv_dictionary")
@IdClass(TsKvDictionaryCompositeKey.class)
public final class TsKvDictionary {

    @Id
    @Column(name = KEY_COLUMN)
    private String key;

    @Column(name = KEY_ID_COLUMN, unique = true, columnDefinition="int")
    @Generated(GenerationTime.INSERT)
    private int keyId;

}
