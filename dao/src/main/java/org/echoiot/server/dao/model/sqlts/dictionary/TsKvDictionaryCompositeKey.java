package org.echoiot.server.dao.model.sqlts.dictionary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Transient;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TsKvDictionaryCompositeKey implements Serializable{

    @Transient
    private static final long serialVersionUID = -4089175869616037523L;

    private String key;
}
