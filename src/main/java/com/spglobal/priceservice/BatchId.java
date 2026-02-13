package com.spglobal.priceservice;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a unique identifier for a price batch.
 * 
 * Each batch is uniquely identified by a UUID, generated at batch creation time.
 */
public class BatchId {
    private final String id;

    /**
     * Creates a new BatchId with a randomly generated UUID.
     */
    public BatchId() {
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Creates a BatchId from an existing string identifier.
     *
     * @param id the batch identifier (non-null)
     */
    public BatchId(String id) {
        this.id = Objects.requireNonNull(id, "Batch ID cannot be null");
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatchId batchId = (BatchId) o;
        return Objects.equals(id, batchId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BatchId{" + id + '}';
    }
}
