package com.chatapp.util;

public class CustomMap {
    private static class Entry {
        String key;
        Object value;
        Entry next;

        Entry(String key, Object value, Entry next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private Entry[] buckets;
    private int size;

    public CustomMap() {
        this.buckets = new Entry[16];
        this.size = 0;
    }

    private int getBucketIndex(String key) {
        if (key == null) return 0;
        int h = 0;
        int len = key.length();
        for (int i = 0; i < len; i++) {
            h = 31 * h + key.charAt(i);
        }
        return (h & 0x7FFFFFFF) % buckets.length;
    }

    public synchronized void put(String key, Object value) {
        if (key == null) return;
        int idx = getBucketIndex(key);
        Entry current = buckets[idx];
        while (current != null) {
            if (CustomStringUtil.customEqualsIgnoreCase(current.key, key)) {
                current.value = value;
                return;
            }
            current = current.next;
        }
        
        // Add new entry at the head of the bucket
        buckets[idx] = new Entry(key, value, buckets[idx]);
        size++;
    }

    public synchronized Object get(String key) {
        if (key == null) return null;
        int idx = getBucketIndex(key);
        Entry current = buckets[idx];
        while (current != null) {
            if (CustomStringUtil.customEqualsIgnoreCase(current.key, key)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    public synchronized Object remove(String key) {
        if (key == null) return null;
        int idx = getBucketIndex(key);
        Entry prev = null;
        Entry current = buckets[idx];
        while (current != null) {
            if (CustomStringUtil.customEqualsIgnoreCase(current.key, key)) {
                if (prev == null) {
                    buckets[idx] = current.next;
                } else {
                    prev.next = current.next;
                }
                size--;
                return current.value;
            }
            prev = current;
            current = current.next;
        }
        return null;
    }

    public synchronized boolean containsKey(String key) {
        return get(key) != null;
    }

    public synchronized int size() {
        return size;
    }

    public synchronized String[] keys() {
        String[] allKeys = new String[size];
        int idx = 0;
        for (int i = 0; i < buckets.length; i++) {
            Entry current = buckets[i];
            while (current != null) {
                allKeys[idx++] = current.key;
                current = current.next;
            }
        }
        return allKeys;
    }

    public synchronized Object[] values() {
        Object[] allValues = new Object[size];
        int idx = 0;
        for (int i = 0; i < buckets.length; i++) {
            Entry current = buckets[i];
            while (current != null) {
                allValues[idx++] = current.value;
                current = current.next;
            }
        }
        return allValues;
    }
}
