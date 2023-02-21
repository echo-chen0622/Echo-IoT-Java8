package org.echoiot.common.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class CollectionsUtil {
    public static boolean isEmpty(@Nullable Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * Returns new set with elements that are present in set B(new) but absent in set A(old).
     */
    @NotNull
    public static <T> Set<T> diffSets(@NotNull Set<T> a, @NotNull Set<T> b) {
        return b.stream().filter(p -> !a.contains(p)).collect(Collectors.toSet());
    }

    public static <T> boolean contains(@NotNull Collection<T> collection, T element) {
        return isNotEmpty(collection) && collection.contains(element);
    }

    public static <T> int countNonNull(@NotNull T[] array) {
        int count = 0;
        for (@Nullable T t : array) {
            if (t != null) count++;
        }
        return count;
    }

}
