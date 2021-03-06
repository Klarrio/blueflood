package com.rackspacecloud.blueflood.cache;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.rackspacecloud.blueflood.types.Locator;
import com.rackspacecloud.blueflood.utils.Metrics;

import java.util.concurrent.TimeUnit;

public class LocatorCache {

    // this collection is used to reduce the number of locators that get written.
    // Simply, if a locator has been seen within the last 10 minutes, don't bother.
    private final Cache<String, LocatorCacheEntry> insertedLocators;

    // this collection is used to reduce the number of delayed locators that get
    // written per slot. Simply, if a locator has been seen for a slot, don't bother.
    private final Cache<String, Boolean> insertedDelayedLocators;

    private static LocatorCache instance = new LocatorCache(10, TimeUnit.MINUTES,
                                                            3, TimeUnit.DAYS);


    static {
        Metrics.getRegistry().register(MetricRegistry.name(LocatorCache.class, "Current Locators Count"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return instance.getCurrentLocatorCount();
                    }
                });

        Metrics.getRegistry().register(MetricRegistry.name(LocatorCache.class, "Current Delayed Locators Count"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return instance.getCurrentDelayedLocatorCount();
                    }
                });
    }


    public static LocatorCache getInstance() {
        return instance;
    }

    protected LocatorCache(long expireAfterAccessDuration, TimeUnit expireAfterAccessTimeUnit,
                           long expireAfterWriteDuration, TimeUnit expireAfterWriteTimeUnit) {

        insertedLocators =
                CacheBuilder.newBuilder()
                        .expireAfterAccess(expireAfterAccessDuration, expireAfterAccessTimeUnit)
                        .expireAfterWrite(expireAfterWriteDuration, expireAfterWriteTimeUnit)
                        .concurrencyLevel(16)
                        .build();

        insertedDelayedLocators =
                CacheBuilder.newBuilder()
                        .expireAfterAccess(expireAfterAccessDuration, expireAfterAccessTimeUnit)
                        .concurrencyLevel(16)
                        .build();
    }

    @VisibleForTesting
    public static LocatorCache getInstance(long expireAfterAccessDuration, TimeUnit expireAfterAccessTimeUnit,
                                           long expireAfterWriteDuration, TimeUnit expireAfterWriteTimeUnit) {

        return new LocatorCache(expireAfterAccessDuration, expireAfterAccessTimeUnit,
                                expireAfterWriteDuration, expireAfterWriteTimeUnit);
    }

    public long getCurrentLocatorCount() {
        return insertedLocators.size();
    }

    public long getCurrentDelayedLocatorCount() {
        return insertedDelayedLocators.size();
    }

    /**
     * Checks if Locator is recently inserted in the batch layer
     *
     * @param loc
     * @return
     */
    public synchronized boolean isLocatorCurrentInBatchLayer(Locator loc) {
        LocatorCacheEntry entry = insertedLocators.getIfPresent(loc.toString());
        return entry != null && entry.isBatchCurrent();
    }

    /**
     * Checks if Locator is recently inserted in the discovery layer
     *
     * @param loc
     * @return
     */
    public synchronized boolean isLocatorCurrentInDiscoveryLayer(Locator loc) {
        LocatorCacheEntry entry = insertedLocators.getIfPresent(loc.toString());
        return entry != null && entry.isDiscoveryCurrent();
    }

    /**
     * Check if the delayed locator is recently inserted for a given slot
     *
     * @param slot
     * @param locator
     * @return
     */
    public synchronized boolean isDelayedLocatorForASlotCurrent(int slot, Locator locator) {
        return insertedDelayedLocators.getIfPresent(getLocatorSlotKey(slot, locator)) != null;
    }

    private String getLocatorSlotKey(int slot, Locator locator) {
        return slot + "," + locator.toString();
    }

    private LocatorCacheEntry getOrCreateInsertedLocatorEntry(Locator loc) {
        LocatorCacheEntry entry = insertedLocators.getIfPresent(loc.toString());

        if(entry == null) {
            entry = new LocatorCacheEntry();
            insertedLocators.put(loc.toString(), entry);
        }

        return entry;
    }

    /**
     * Marks the Locator as recently inserted in the batch layer
     * @param loc
     */
    public synchronized void setLocatorCurrentInBatchLayer(Locator loc) {
        getOrCreateInsertedLocatorEntry(loc).setBatchCurrent();
    }

    /**
     * Marks the Locator as recently inserted in the discovery layer
     * @param loc
     */
    public synchronized void setLocatorCurrentInDiscoveryLayer(Locator loc) {
        getOrCreateInsertedLocatorEntry(loc).setDiscoveryCurrent();
    }

    /**
     * Marks the delayed locator as recently inserted for a given slot
     * @param slot
     * @param locator
     */
    public synchronized void setDelayedLocatorForASlotCurrent(int slot, Locator locator) {
        insertedDelayedLocators.put(getLocatorSlotKey(slot, locator), Boolean.TRUE);
    }

    @VisibleForTesting
    public synchronized void resetCache() {
        insertedLocators.invalidateAll();
        insertedDelayedLocators.invalidateAll();
    }

    @VisibleForTesting
    public synchronized void resetInsertedLocatorsCache() {
        insertedLocators.invalidateAll();
    }

    /**
     * Cache entry which defines where the locator has been inserted into during the caching period.
     */
    private class LocatorCacheEntry {
        private boolean discoveryCurrent = false;
        private boolean batchCurrent = false;

        void setDiscoveryCurrent() {
            this.discoveryCurrent = true;
        }

        void setBatchCurrent() {
            this.batchCurrent = true;
        }

        boolean isDiscoveryCurrent() {
            return discoveryCurrent;
        }

        boolean isBatchCurrent() {
            return batchCurrent;
        }
    }

}
