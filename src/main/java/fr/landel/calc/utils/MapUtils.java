package fr.landel.calc.utils;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class MapUtils {

    // private static final Logger LOGGER = new Logger(MapUtils.class);

    private static final String ERROR_MAP_NULL = "map cannot be null";
    private static final String ERROR_VALUE_SUPPLIER_NULL = "value's supplier cannot be null";

    private MapUtils() {
        throw new UnsupportedOperationException();
    }

    public static <K, V> V getOrPutIfAbsent(final Map<K, V> map, final K key, final Supplier<V> supplier) {
        V value = Objects.requireNonNull(map, ERROR_MAP_NULL).get(key);
        if (value == null) {
            map.put(key, value = Objects.requireNonNull(supplier, ERROR_VALUE_SUPPLIER_NULL).get());
        }
        return value;
    }
}
