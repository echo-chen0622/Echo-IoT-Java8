package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class IdBased<I extends UUIDBased> implements HasId<I> {

	protected I id;

	public IdBased() {
		super();
	}

	public IdBased(I id) {
		super();
		this.id = id;
	}

    @JsonSetter
	public void setId(I id) {
		this.id = id;
	}

	public I getId() {
		return id;
	}

	@Nullable
	@JsonIgnore
	public UUID getUuidId() {
		if (id != null) {
			return id.getId();
		}
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdBased other = (IdBased) obj;
		if (id == null) {
			return other.id == null;
		} else return id.equals(other.id);
	}

}
