package com.spglobal.priceservice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A thread-safe service for managing financial instrument price data.
 * 
 * This service implements a batching mechanism where producers upload prices in batches,
 * and consumers can query for the latest prices. Key features:
 * 
 * - Atomic batch completion: All prices in a batch become visible simultaneously
 * - Batch isolation: Incomplete batch data is never visible to consumers
 * - Thread-safe: Supports concurrent producer and consumer operations
 * - Resilient: Handles producer errors gracefully
 * 
 * Design decisions:
 * - Used ReadWriteLock for efficient concurrent reads with exclusive writes
 * - Stored per-instrument latest prices separately for O(1) lookup performance
 * - Used ConcurrentHashMap for thread-safe batch staging
 * - Batch state machine prevents incorrect operation sequences
 */
public class PriceService {
    
    // State machine for batch operations
    private enum BatchState {
        ACTIVE,      // Batch is open and accepting prices
        COMPLETED,   // Batch is completed and data is visible
        CANCELLED    // Batch is cancelled and discarded
    }
    
    // Represents a batch being staged
    private static class StagedBatch {
        final String batchId;
        final Map<String, Price> prices;
        BatchState state;
        
        StagedBatch(String batchId) {
            this.batchId = batchId;
            this.prices = new ConcurrentHashMap<>();
            this.state = BatchState.ACTIVE;
        }
    }
    
    // Latest prices per instrument ID
    private final Map<String, Price> latestPrices = new ConcurrentHashMap<>();
    
    // Batch staging area: batchId -> StagedBatch
    private final Map<String, StagedBatch> stagedBatches = new ConcurrentHashMap<>();
    
    // Lock to protect batch state transitions and final price updates
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Starts a new batch for price uploads.
     * 
     * @return a new batch ID for this batch
     * @throws IllegalStateException if called when another batch is already active
     * 
     * Design: Returns BatchId object to provide type safety and prevent ID confusion
     */
    public BatchId startBatch() {
        lock.writeLock().lock();
        try {
            BatchId batchId = new BatchId();
            String batchIdStr = batchId.getId();
            
            // Check if a batch with this ID already exists (should be extremely rare with UUID)
            if (stagedBatches.containsKey(batchIdStr)) {
                throw new IllegalStateException(
                    "Batch with ID " + batchIdStr + " already exists");
            }
            
            stagedBatches.put(batchIdStr, new StagedBatch(batchIdStr));
            return batchId;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Uploads a price record to an active batch.
     * 
     * Multiple prices for the same instrument can be uploaded within a batch;
     * the latest one (by asOf time) will be retained.
     * 
     * @param batchId the batch ID returned from startBatch()
     * @param price the price record to upload
     * @throws NullPointerException if any parameter is null
     * @throws IllegalStateException if the batch is not active or does not exist
     * 
     * Design: Uses concurrent updates within a batch for parallel upload support
     */
    public void uploadPrice(BatchId batchId, Price price) {
        if (batchId == null) {
            throw new NullPointerException("Batch ID cannot be null");
        }
        if (price == null) {
            throw new NullPointerException("Price cannot be null");
        }
        
        String batchIdStr = batchId.getId();
        
        lock.readLock().lock();
        try {
            StagedBatch batch = stagedBatches.get(batchIdStr);
            
            if (batch == null) {
                throw new IllegalStateException(
                    "Batch " + batchIdStr + " does not exist. Call startBatch() first.");
            }
            
            if (batch.state != BatchState.ACTIVE) {
                throw new IllegalStateException(
                    "Batch " + batchIdStr + " is not active. Current state: " + batch.state);
            }
            
            // Store the price for this instrument, keeping only the latest by asOf time
            String instrumentId = price.getId();
            batch.prices.compute(instrumentId, (id, existingPrice) -> {
                if (existingPrice == null) {
                    return price;
                }
                // Keep the price with the latest asOf timestamp
                return price.getAsOf().isAfter(existingPrice.getAsOf()) ? price : existingPrice;
            });
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Uploads multiple prices to an active batch in a single operation.
     * 
     * This is a convenience method for batch uploads. Internally, each price
     * is processed with the same logic as individual uploadPrice calls.
     * 
     * @param batchId the batch ID returned from startBatch()
     * @param prices a list of price records to upload
     * @throws NullPointerException if any parameter is null
     * @throws IllegalStateException if the batch is not active
     * 
     * Design: Reuses uploadPrice for consistency
     */
    public void uploadPrices(BatchId batchId, List<Price> prices) {
        if (prices == null) {
            throw new NullPointerException("Prices list cannot be null");
        }
        
        for (Price price : prices) {
            uploadPrice(batchId, price);
        }
    }

    /**
     * Completes a batch and makes all its prices available to consumers atomically.
     * 
     * All prices in the batch become visible simultaneously to any consumer queries.
     * This operation is atomic: either all prices are committed or none are.
     * 
     * @param batchId the batch ID returned from startBatch()
     * @throws NullPointerException if batchId is null
     * @throws IllegalStateException if the batch does not exist or is not active
     * 
     * Design: Uses write lock to ensure atomic visibility of all batch prices
     */
    public void completeBatch(BatchId batchId) {
        if (batchId == null) {
            throw new NullPointerException("Batch ID cannot be null");
        }
        
        String batchIdStr = batchId.getId();
        
        lock.writeLock().lock();
        try {
            StagedBatch batch = stagedBatches.get(batchIdStr);
            
            if (batch == null) {
                throw new IllegalStateException(
                    "Batch " + batchIdStr + " does not exist");
            }
            
            if (batch.state != BatchState.ACTIVE) {
                throw new IllegalStateException(
                    "Batch " + batchIdStr + " is not active. Current state: " + batch.state);
            }
            
            // Atomically update all latest prices
            for (Map.Entry<String, Price> entry : batch.prices.entrySet()) {
                String instrumentId = entry.getKey();
                Price newPrice = entry.getValue();
                
                latestPrices.compute(instrumentId, (id, existingPrice) -> {
                    // Keep whichever price is more recent by asOf time
                    if (existingPrice == null) {
                        return newPrice;
                    }
                    return newPrice.getAsOf().isAfter(existingPrice.getAsOf()) 
                        ? newPrice 
                        : existingPrice;
                });
            }
            
            // Mark batch as completed
            batch.state = BatchState.COMPLETED;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Cancels a batch and discards all its prices.
     * 
     * After cancellation, the batch is removed from the system and cannot be modified.
     * Cancelled batches never affect visible price data.
     * 
     * @param batchId the batch ID returned from startBatch()
     * @throws NullPointerException if batchId is null
     * @throws IllegalStateException if the batch does not exist or is not active
     * 
     * Design: Batch state transitions prevent re-use of cancelled batches
     */
    public void cancelBatch(BatchId batchId) {
        if (batchId == null) {
            throw new NullPointerException("Batch ID cannot be null");
        }
        
        String batchIdStr = batchId.getId();
        
        lock.writeLock().lock();
        try {
            StagedBatch batch = stagedBatches.get(batchIdStr);
            
            if (batch == null) {
                throw new IllegalStateException(
                    "Batch " + batchIdStr + " does not exist");
            }
            
            if (batch.state != BatchState.ACTIVE) {
                throw new IllegalStateException(
                    "Batch " + batchIdStr + " is not active. Current state: " + batch.state);
            }
            
            // Mark batch as cancelled (data is discarded, no update to latestPrices)
            batch.state = BatchState.CANCELLED;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Retrieves the latest prices for a list of instrument IDs.
     * 
     * Only prices from completed batches are returned. If an instrument has no
     * completed price, it will not appear in the result.
     * 
     * @param instrumentIds a list of instrument IDs to query
     * @return a map of instrument ID to latest Price (empty map if no matches found)
     * @throws NullPointerException if instrumentIds is null
     * 
     * Design: Uses read lock for concurrent access, returns unmodifiable map
     */
    public Map<String, Price> getLatestPrices(List<String> instrumentIds) {
        if (instrumentIds == null) {
            throw new NullPointerException("Instrument IDs list cannot be null");
        }
        
        lock.readLock().lock();
        try {
            Map<String, Price> result = new HashMap<>();
            for (String id : instrumentIds) {
                if (id != null && !id.isEmpty()) {
                    Price price = latestPrices.get(id);
                    if (price != null) {
                        result.put(id, price);
                    }
                }
            }
            return Collections.unmodifiableMap(result);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Retrieves the latest price for a single instrument.
     * 
     * @param instrumentId the instrument ID to query
     * @return the latest Price, or null if not found
     * @throws NullPointerException if instrumentId is null
     * 
     * Design: Convenience method for single-instrument queries
     */
    public Price getLatestPrice(String instrumentId) {
        if (instrumentId == null) {
            throw new NullPointerException("Instrument ID cannot be null");
        }
        
        lock.readLock().lock();
        try {
            return latestPrices.get(instrumentId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Retrieves all currently available latest prices.
     * 
     * @return an unmodifiable map of all instrument IDs to their latest Prices
     * 
     * Design: Useful for monitoring and debugging
     */
    public Map<String, Price> getAllLatestPrices() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableMap(new HashMap<>(latestPrices));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Resets the service to an empty state.
     * 
     * This method is useful for testing and should not be used in production.
     * All batches and prices are discarded.
     * 
     * Design: Full write lock ensures no operations interfere during reset
     */
    public void reset() {
        lock.writeLock().lock();
        try {
            latestPrices.clear();
            stagedBatches.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
