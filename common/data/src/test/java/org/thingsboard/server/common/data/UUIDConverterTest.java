package org.thingsboard.server.common.data;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 14.07.17.
 */
@RunWith(MockitoJUnitRunner.class)
public class UUIDConverterTest {

    @Test
    public void basicUuidToStringTest() {
        UUID original = UUID.fromString("58e0a7d7-eebc-11d8-9669-0800200c9a66");
        String result = UUIDConverter.fromTimeUUID(original);
        Assert.assertEquals("1d8eebc58e0a7d796690800200c9a66", result);
    }


    @Test
    public void basicUuid() {
        System.out.println(UUIDConverter.fromString("1e746126eaaefa6a91992ebcb67fe33"));
    }

    @Test
    public void basicUuidConversion() {
        UUID original = UUID.fromString("3dd11790-abf2-11ea-b151-83a091b9d4cc");
        Assert.assertEquals(Uuids.unixTimestamp(original), 1591886749577L);
    }

    @Test
    public void basicStringToUUIDTest() {
        UUID result = UUIDConverter.fromString("1d8eebc58e0a7d796690800200c9a66");
        Assert.assertEquals(UUID.fromString("58e0a7d7-eebc-11d8-9669-0800200c9a66"), result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonV1UuidToStringTest() {
        UUIDConverter.fromTimeUUID(UUID.fromString("58e0a7d7-eebc-01d8-9669-0800200c9a66"));
    }

    @Test
    public void basicUuidComperisonTest() {
        Random r = new Random(System.currentTimeMillis());
        for (int i = 0; i < 100000; i++) {
            long ts = System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 365 * 10;
            long before = (long) (Math.random() * ts);
            long after = (long) (Math.random() * ts);
            if (before > after) {
                long tmp = after;
                after = before;
                before = tmp;
            }

            String beforeStr = UUIDConverter.fromTimeUUID(Uuids.startOf(before));
            String afterStr = UUIDConverter.fromTimeUUID(Uuids.startOf(after));

            if (afterStr.compareTo(beforeStr) < 0) {
                System.out.println("Before: " + before + " | " + beforeStr);
                System.out.println("After: " + after + " | " + afterStr);
            }
            Assert.assertTrue(afterStr.compareTo(beforeStr) >= 0);
        }
    }


}
