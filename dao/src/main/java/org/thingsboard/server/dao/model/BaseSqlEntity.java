package org.thingsboard.server.dao.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

/**
 * Created by ashvayka on 13.07.17.
 */
@Data
@MappedSuperclass
public abstract class BaseSqlEntity<D> implements BaseEntity<D> {

    @Id
    @Column(name = ModelConstants.ID_PROPERTY, columnDefinition = "uuid")
    protected UUID id;

    @Column(name = ModelConstants.CREATED_TIME_PROPERTY, updatable = false)
    protected long createdTime;

    @Override
    public UUID getUuid() {
        return id;
    }

    @Override
    public void setUuid(UUID id) {
        this.id = id;
    }

    @Override
    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        if (createdTime > 0) {
            this.createdTime = createdTime;
        }
    }
}
