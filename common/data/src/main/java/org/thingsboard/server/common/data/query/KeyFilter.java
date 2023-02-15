package org.thingsboard.server.common.data.query;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@ApiModel
@Data
public class KeyFilter implements Serializable {

    private EntityKey key;
    private EntityKeyValueType valueType;
    private KeyFilterPredicate predicate;

}
