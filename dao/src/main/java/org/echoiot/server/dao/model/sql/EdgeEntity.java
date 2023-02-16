package org.echoiot.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.dao.util.mapping.JsonStringType;

import javax.persistence.Entity;
import javax.persistence.Table;

import static org.echoiot.server.dao.model.ModelConstants.EDGE_COLUMN_FAMILY_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = EDGE_COLUMN_FAMILY_NAME)
public class EdgeEntity extends AbstractEdgeEntity<Edge> {

    public EdgeEntity() {
        super();
    }

    public EdgeEntity(Edge edge) {
        super(edge);
    }

    @Override
    public Edge toData() {
        return super.toEdge();
    }
}
