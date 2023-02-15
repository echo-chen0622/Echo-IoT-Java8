package org.thingsboard.common.util;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.LinkedHashMap;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class LinkedHashMapRemoveEldestTest {

    public static final long MAX_ENTRIES = 10L;
    long removeCount = 0;

    void removalConsumer(Long id, String name) {
        removeCount++;
        assertThat(id, is(Matchers.lessThan(MAX_ENTRIES)));
        assertThat(name, is(id.toString()));
    }

    @Test
    public void givenMap_whenOverSized_thenVerifyRemovedEldest() {
        //given
        LinkedHashMapRemoveEldest<Long, String> map =
                new LinkedHashMapRemoveEldest<>(MAX_ENTRIES, this::removalConsumer);

        assertThat(map.getMaxEntries(), is(MAX_ENTRIES));
        assertThat(map.getRemovalConsumer(), notNullValue());
        assertThat(map, instanceOf(LinkedHashMap.class));
        assertThat(map, instanceOf(LinkedHashMapRemoveEldest.class));
        assertThat(map.size(), is(0));

        //when
        for (long i = 0; i < MAX_ENTRIES * 2; i++) {
            map.put(i, String.valueOf(i));
        }

        //then
        assertThat((long) map.size(), is(MAX_ENTRIES));
        assertThat(removeCount, is(MAX_ENTRIES));
        for (long i = MAX_ENTRIES; i < MAX_ENTRIES * 2; i++) {
            assertThat(map.get(i), is(String.valueOf(i)));
        }
    }

}
