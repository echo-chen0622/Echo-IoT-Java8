package org.thingsboard.server.cache;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface TbTransactionalCache<K extends Serializable, V extends Serializable> {

    String getCacheName();

    TbCacheValueWrapper<V> get(K key);

    void put(K key, V value);

    void putIfAbsent(K key, V value);

    void evict(K key);

    void evict(Collection<K> keys);

    void evictOrPut(K key, V value);

    TbCacheTransaction<K, V> newTransactionForKey(K key);

    /**
     * Note that all keys should be in the same cache slot for redis. You may control the cache slot using '{}' bracers.
     * See CLUSTER KEYSLOT command for more details.
     * @param keys - list of keys to use
     * @return transaction object
     */
    TbCacheTransaction<K, V> newTransactionForKeys(List<K> keys);

    default V getAndPutInTransaction(K key, Supplier<V> dbCall, boolean cacheNullValue) {
        TbCacheValueWrapper<V> cacheValueWrapper = get(key);
        if (cacheValueWrapper != null) {
            return cacheValueWrapper.get();
        }
        var cacheTransaction = newTransactionForKey(key);
        try {
            V dbValue = dbCall.get();
            if (dbValue != null || cacheNullValue) {
                cacheTransaction.putIfAbsent(key, dbValue);
                cacheTransaction.commit();
                return dbValue;
            } else {
                cacheTransaction.rollback();
                return null;
            }
        } catch (Throwable e) {
            cacheTransaction.rollback();
            throw e;
        }
    }

    default <R> R getAndPutInTransaction(K key, Supplier<R> dbCall, Function<V, R> cacheValueToResult, Function<R, V> dbValueToCacheValue, boolean cacheNullValue) {
        TbCacheValueWrapper<V> cacheValueWrapper = get(key);
        if (cacheValueWrapper != null) {
            var cacheValue = cacheValueWrapper.get();
            return cacheValue == null ? null : cacheValueToResult.apply(cacheValue);
        }
        var cacheTransaction = newTransactionForKey(key);
        try {
            R dbValue = dbCall.get();
            if (dbValue != null || cacheNullValue) {
                cacheTransaction.putIfAbsent(key, dbValueToCacheValue.apply(dbValue));
                cacheTransaction.commit();
                return dbValue;
            } else {
                cacheTransaction.rollback();
                return null;
            }
        } catch (Throwable e) {
            cacheTransaction.rollback();
            throw e;
        }
    }

}
