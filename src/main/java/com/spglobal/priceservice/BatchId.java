// This class represents a Batch ID in the Price Service.

package com.spglobal.priceservice;

public class BatchId {
    private String id;

    public BatchId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "BatchId{" +
                "id='" + id + '\'' +
                '}';
    }
}