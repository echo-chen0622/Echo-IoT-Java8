package org.thingsboard.server.dao.model.sqlts.ts;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TsKvCompositeKey implements Serializable {

    @Transient
    private static final long serialVersionUID = -4089175869616037523L;

    private UUID entityId;
    private int key;
    private long ts;
}
