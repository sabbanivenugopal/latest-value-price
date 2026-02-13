package com.spglobal.priceservice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Wrapper for flexible price payload data.
 * 
 * This class encapsulates price data as a flexible key-value map,
 * allowing various price attributes to be stored without strict schema.
 */
public class PricePayload {
    private final Map<String, Object> data;

    /**
     * Constructs a new PricePayload with the provided data.
     *
     * @param data the price data map (non-null, can be empty)
     * @throws NullPointerException if data is null
     */
    public PricePayload(Map<String, Object> data) {
        this.data = new HashMap<>(Objects.requireNonNull(data, "Data cannot be null"));
    }

    /**
     * Constructs an empty PricePayload.
     */
    public PricePayload() {
        this.data = new HashMap<>();
    }

    /**
     * Gets the underlying data as an unmodifiable map.
     *
     * @return an unmodifiable view of the payload data
     */
    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    /**
     * Gets a specific value from the payload by key.
     *
     * @param key the data key
     * @return the value associated with the key, or null if not present
     */
    public Object get(String key) {
        return data.get(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PricePayload that = (PricePayload) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    @Override
    public String toString() {
        return "PricePayload{" + data + '}';
    }
}
