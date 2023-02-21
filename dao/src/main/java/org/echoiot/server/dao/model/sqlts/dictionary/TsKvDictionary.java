package org.echoiot.server.dao.model.sqlts.dictionary;

import lombok.Data;
import org.echoiot.server.dao.model.ModelConstants;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;

@Data
@Entity
@Table(name = "ts_kv_dictionary")
@IdClass(TsKvDictionaryCompositeKey.class)
public final class TsKvDictionary {

    @Id
    @Column(name = ModelConstants.KEY_COLUMN)
    private String key;

    @Column(name = ModelConstants.KEY_ID_COLUMN, unique = true, columnDefinition="int")
    @Generated(GenerationTime.INSERT)
    private int keyId;

}
