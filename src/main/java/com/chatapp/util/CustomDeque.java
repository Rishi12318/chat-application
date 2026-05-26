package com.chatapp.util;

public class CustomDeque {
    private static class Node {
        Object item;
        Node next;
        Node prev;

        Node(Object item, Node next, Node prev) {
            this.item = item;
            this.next = next;
            this.prev = prev;
        }
    }

    private Node first;
    private Node last;
    private int size;

    public CustomDeque() {
        this.first = null;
        this.last = null;
        this.size = 0;
    }

    public synchronized void addLast(Object item) {
        Node l = last;
        Node newNode = new Node(item, null, l);
        last = newNode;
        if (l == null) {
            first = newNode;
        } else {
            l.next = newNode;
        }
        size++;
    }

    public synchronized Object removeFirst() {
        if (first == null) return null;
        Object element = first.item;
        Node nextNode = first.next;
        first.item = null; // Clear reference
        first = nextNode;
        if (nextNode == null) {
            last = null;
        } else {
            nextNode.prev = null;
        }
        size--;
        return element;
    }

    public synchronized int size() {
        return size;
    }

    public synchronized Object[] toSnapshot() {
        Object[] snapshot = new Object[size];
        int idx = 0;
        Node current = first;
        while (current != null) {
            snapshot[idx++] = current.item;
            current = current.next;
        }
        return snapshot;
    }
}
