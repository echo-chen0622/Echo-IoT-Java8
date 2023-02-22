package org.echoiot.server.cache;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.springframework.cache.Cache;

@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SimpleTbCacheValueWrapper<T> implements TbCacheValueWrapper<T> {

    private final T value;

    @Override
    public T get() {
        return value;
    }

    @Contract(" -> new")
    public static <T> SimpleTbCacheValueWrapper<T> empty() {
        return new SimpleTbCacheValueWrapper<>(null);
    }

    @Contract("_ -> new")
    public static <T> SimpleTbCacheValueWrapper<T> wrap(T value) {
        return new SimpleTbCacheValueWrapper<>(value);
    }

    @Contract("null -> null; !null -> new")
    @SuppressWarnings("unchecked")
    public static <T> SimpleTbCacheValueWrapper<T> wrap(@Nullable Cache.ValueWrapper source) {
        return source == null ? null : new SimpleTbCacheValueWrapper<>((T) source.get());
    }
}
