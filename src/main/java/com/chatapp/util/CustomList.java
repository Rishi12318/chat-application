package com.chatapp.util;

public class CustomList {
    private Object[] elements;
    private int size;

    public CustomList() {
        this.elements = new Object[10];
        this.size = 0;
    }

    public synchronized void add(Object element) {
        ensureCapacity(size + 1);
        elements[size++] = element;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > elements.length) {
            int newCapacity = elements.length * 2;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            Object[] newElements = new Object[newCapacity];
            for (int i = 0; i < size; i++) {
                newElements[i] = elements[i];
            }
            elements = newElements;
        }
    }

    public synchronized Object get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return elements[index];
    }

    public synchronized int size() {
        return size;
    }

    public synchronized Object remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        Object removed = elements[index];
        int numMoved = size - index - 1;
        if (numMoved > 0) {
            for (int i = 0; i < numMoved; i++) {
                elements[index + i] = elements[index + i + 1];
            }
        }
        elements[--size] = null; // Clear reference
        return removed;
    }

    public synchronized void clear() {
        for (int i = 0; i < size; i++) {
            elements[i] = null;
        }
        size = 0;
    }
}
