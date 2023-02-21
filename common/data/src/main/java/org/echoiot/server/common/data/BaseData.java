package org.echoiot.server.common.data;

import org.echoiot.server.common.data.id.IdBased;
import org.echoiot.server.common.data.id.UUIDBased;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public abstract class BaseData<I extends UUIDBased> extends IdBased<I> implements Serializable {

    private static final long serialVersionUID = 5422817607129962637L;

    protected long createdTime;

    public BaseData() {
        super();
    }

    public BaseData(I id) {
        super(id);
    }

    public BaseData(@NotNull BaseData<I> data) {
        super(data.getId());
        this.createdTime = data.getCreatedTime();
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (int) (createdTime ^ (createdTime >>> 32));
        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(@NotNull Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        @NotNull BaseData other = (BaseData) obj;
        return createdTime == other.createdTime;
    }

    @Override
    public String toString() {
        String builder = "BaseData [createdTime=" + createdTime + ", id=" + id + "]";
        return builder;
    }

}
