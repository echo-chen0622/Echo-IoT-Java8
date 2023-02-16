package org.echoiot.server.dao.model.sqlts.latest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TsKvLatestCompositeKey implements Serializable{

    @Transient
    private static final long serialVersionUID = -4089175869616037523L;

    private UUID entityId;
    private int key;
}
