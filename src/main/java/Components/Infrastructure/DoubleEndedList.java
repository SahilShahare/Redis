package Components.Infrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoubleEndedList<E> {
    // Note: get(i) and get(start, end) take ordinary 0-based positions
    // (0 = current leftmost element). low/high are internal bookkeeping
    // only and are never exposed through the public API.

    private static class Node<E> {
        E item;
        Node<E> prev;
        Node<E> next;

        Node(E item) {
            this.item = item;
        }
    }

    private final Map<Integer, Node<E>> index = new HashMap<>();
    private Node<E> head;
    private Node<E> tail;

    // Valid virtual indices are [low, high] inclusive. Empty when high < low.
    private int low = 0;
    private int high = -1;

    public int addLeft(E item) {
        Node<E> node = new Node<>(item);
        if (head == null) {
            head = node;
            tail = node;
        } else {
            node.next = head;
            head.prev = node;
            head = node;
        }
        low--;
        index.put(low, node);
        return size();
    }

    public int addRight(E item) {
        Node<E> node = new Node<>(item);
        if (tail == null) {
            head = node;
            tail = node;
        } else {
            node.prev = tail;
            tail.next = node;
            tail = node;
        }
        high++;
        index.put(high, node);
        return size();
    }

    public int addAllLeft(List<? extends E> items) {
        if (items == null) {
            throw new NullPointerException("items must not be null");
        }
        if (items.isEmpty()) {
            return size();
        }

        Node<E> segHead = null;
        Node<E> segTail = null;
        for (int i = items.size() - 1; i >= 0; i--) {
            Node<E> node = new Node<>(items.get(i));
            if (segHead == null) {
                segHead = node;
                segTail = node;
            } else {
                segTail.next = node;
                node.prev = segTail;
                segTail = node;
            }
        }

        if (head == null) {
            head = segHead;
            tail = segTail;
        } else {
            segTail.next = head;
            head.prev = segTail;
            head = segHead;
        }

        int key = low - items.size();
        low = key;
        Node<E> n = segHead;
        while (n != null) {
            index.put(key, n);
            key++;
            n = n.next;
        }

        return size();
    }

    public int addAllRight(List<? extends E> items) {
        if (items == null) {
            throw new NullPointerException("items must not be null");
        }
        if (items.isEmpty()) {
            return size();
        }

        Node<E> segHead = null;
        Node<E> segTail = null;
        for (E item : items) {
            Node<E> node = new Node<>(item);
            if (segHead == null) {
                segHead = node;
                segTail = node;
            } else {
                segTail.next = node;
                node.prev = segTail;
                segTail = node;
            }
        }

        if (tail == null) {
            head = segHead;
            tail = segTail;
        } else {
            tail.next = segHead;
            segHead.prev = tail;
            tail = segTail;
        }

        int key = high + 1;
        Node<E> n = segHead;
        while (n != null) {
            index.put(key, n);
            key++;
            n = n.next;
        }
        high = key - 1;
        return size();
    }

    public E removeLeft() {
        if (high < low) {
            return null;
        }
        Node<E> node = head;
        index.remove(low);
        head = head.next;
        if (head != null) {
            head.prev = null;
        } else {
            tail = null; // list just became empty
        }
        low++;
        return node.item;
    }

    public List<E> removeLeft(int n) {
        List<E> removed = new ArrayList<>(Math.max(0, n));
        if (n <= 0) {
            return removed;
        }
        for (int i = 0; i < n; i++) {
            if (high < low) {
                break; // nothing left; stop quietly rather than throwing
            }
            removed.add(removeLeft());
        }
        return removed;
    }

    public E removeRight() {
        if (high < low) {
            return null;
        }
        Node<E> node = tail;
        index.remove(high);
        tail = tail.prev;
        if (tail != null) {
            tail.next = null;
        } else {
            head = null; // list just became empty
        }
        high--;
        return node.item;
    }

    public List<E> removeRight(int n) {
        List<E> removed = new ArrayList<>(Math.max(0, n));
        if (n <= 0) {
            return removed;
        }
        for (int i = 0; i < n; i++) {
            if (high < low) {
                break; // nothing left; stop quietly rather than throwing
            }
            removed.add(removeRight());
        }
        return removed;
    }

    public E getLeft() {
        if (isEmpty()) {
            return null;
        }
        return index.get(low).item;
    }

    public List<E> get(int start, int end) {
        int size = size();
        start = (start >= 0) ? start : (size + start);
        start = Math.max(0, start);
        end = (end >= 0) ? end : (size + end);
        end = Math.min(end, size - 1);
        int count = Math.max(0, end - start + 1);

        List<E> result = new ArrayList<>(count);
        if (count > 0 && start < size) {
            Node<E> node = index.get(low + start); // single hash lookup
            for (int c = 0; c < count; c++) {
                result.add(node.item);
                node = node.next; // O(1) walk, no further hashing
                if (node == null) break;
            }
        }
        return result;
    }

    public int size() {
        return Math.max(0, high - low + 1);
    }

    public List<E> toList() {
        int size = Math.max(0, high - low + 1);
        List<E> result = new ArrayList<>(size);
        if (size > 0) {
            Node<E> node = index.get(low); // single hash lookup
            for (int c = 0; c < size; c++) {
                result.add(node.item);
                node = node.next;
            }
        }
        return result;
    }

    public boolean isEmpty() {
        return high < low;
    }
}