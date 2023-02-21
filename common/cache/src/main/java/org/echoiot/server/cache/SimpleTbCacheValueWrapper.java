package org.echoiot.server.cache;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.cache.Cache;

@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SimpleTbCacheValueWrapper<T> implements TbCacheValueWrapper<T> {

    @NotNull
    private final T value;

    @Override
    public T get() {
        return value;
    }

    @NotNull
    public static <T> SimpleTbCacheValueWrapper<T> empty() {
        return new SimpleTbCacheValueWrapper<>(null);
    }

    @NotNull
    public static <T> SimpleTbCacheValueWrapper<T> wrap(T value) {
        return new SimpleTbCacheValueWrapper<>(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> SimpleTbCacheValueWrapper<T> wrap(@Nullable Cache.ValueWrapper source) {
        return source == null ? null : new SimpleTbCacheValueWrapper<>((T) source.get());
    }
}
