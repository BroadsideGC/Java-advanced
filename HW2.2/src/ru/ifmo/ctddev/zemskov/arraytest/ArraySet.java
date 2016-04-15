package ru.ifmo.ctddev.zemskov.arraytest;

import java.util.*;

/**
 * Created by BigZ on 27.02.16.
 */
public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {

    private final List<E> array;
    private final Comparator<? super E> comp;

    public ArraySet() {
        this(Collections.emptyList(), null, true);
    }

    public ArraySet(ArraySet<E> other) {
        this(other.array, other.comp, true);
    }

    public ArraySet(Collection<E> collection, Comparator<? super E> comparator) {
        this.comp = comparator;
        this.array = sortCollection(collection, comparator);
    }

    public ArraySet(List<E> list, Comparator<? super E> comparator, boolean sorted) {
        this.comp = comparator;
        if (!sorted && list != null) {
            this.array = sortCollection(list, comparator);
        } else {
            this.array = list;
        }
    }

    private ArrayList<E> sortCollection(Collection<E> collection, Comparator<? super E> comparator) {
        ArrayList<E> ret = new ArrayList<>();
        Set<E> s = new TreeSet<>(comparator);
        s.addAll(collection);
        ret.addAll(s);
        return ret;
    }

    public ArraySet(Collection<E> collection) {
        this(collection, null);
    }

    @Override
    public Object[] toArray() {
        return array.toArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(array, (E) o, comp) >= 0;
    }

    @Override
    public E lower(E e) {
        int ind = Collections.binarySearch(array, e, comp);
        return ((ind == 0 || ind == -1) ? null : array.get((ind < 0 ? -ind - 1 : ind) - 1));
    }

    @Override
    public E floor(E e) {
        int ind = Collections.binarySearch(array, e, comp);
        return ((ind == -1) ? null : array.get(ind < 0 ? -ind - 2 : ind));
    }

    @Override
    public E ceiling(E e) {
        int ind = Collections.binarySearch(array, e, comp);
        return ((ind < -size() || ind > size() - 1) ? null : array.get((ind < 0 ? -ind - 1 : ind)));
    }

    @Override
    public E higher(E e) {
        int ind = Collections.binarySearch(array, e, comp);
        return ((ind < -size() || ind >= size() - 1) ? null : array.get((ind < 0 ? -ind - 1 : ind + 1)));
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException("Can`t be modified");
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException("Can`t be modified");
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(array).iterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(new ReversedArrayList<>(array), comp.reversed(), true);
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new ReversedArrayList<>(array).iterator();
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return tailSet(fromElement, fromInclusive).headSet(toElement, toInclusive);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        int ind = Collections.binarySearch(array, toElement, comp);
        int add = (inclusive && ind >= 0) ? 1 : 0;
        return subSet(0, (ind < 0 ? -ind - 1 : ind) + add);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        int ind = Collections.binarySearch(array, fromElement, comp);
        int add = (!inclusive && ind >= 0) ? 1 : 0;
        return subSet((ind < 0 ? -ind - 1 : ind) + add, size());
    }

    public NavigableSet<E> subSet(int first, int last) {
        return new ArraySet<>(array.subList(first, last), comp, true);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comp;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return tailSet(fromElement).headSet(toElement);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return array.get(0);
    }

    @Override
    public E last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return array.get(size() - 1);
    }

    @Override
    public int size() {
        return array.size();
    }

    private class ReversedArrayList<T> extends AbstractList<T> implements RandomAccess {

        private final List<T> substrate;
        private final boolean direct;

        ReversedArrayList(List<T> substrate) {
            if (substrate instanceof ReversedArrayList) {
                ReversedArrayList<T> tmp = (ReversedArrayList<T>) substrate;
                this.substrate = tmp.substrate;
                direct = !tmp.direct;
                return;
            }
            if (!(substrate instanceof RandomAccess)) {
                throw new IllegalArgumentException("Substrate must be random access");
            }
            this.substrate = substrate;
            direct = false;
        }

        @Override
        public int size() {
            return substrate.size();
        }

        @Override
        public T get(int index) {
            return direct ? substrate.get(index) : substrate.get(size() - index - 1);
        }

    }
}
