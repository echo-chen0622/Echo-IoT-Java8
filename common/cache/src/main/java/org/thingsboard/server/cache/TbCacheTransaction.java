package org.thingsboard.server.cache;

public interface TbCacheTransaction<K, V> {

    void putIfAbsent(K key, V value);

    boolean commit();

    void rollback();

}
