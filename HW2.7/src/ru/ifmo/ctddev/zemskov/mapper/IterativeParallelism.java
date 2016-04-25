package ru.ifmo.ctddev.zemskov.mapper;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Allows to make some operations on list in few threads.
 *
 * @author Kirill Zemskov
 */
public class IterativeParallelism implements ListIP {

    private ParallelMapper parallelMapper;

    /**
     * default constructor
     */
    public IterativeParallelism() {
        this.parallelMapper = null;
    }

    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }


    /**
     * Concat string in parallel threads
     *
     * @param i    number of threads
     * @param list list with data
     * @return return concate of strings
     * @throws InterruptedException when something went wrong in some thread
     */
    @Override
    public String join(int i, List<?> list) throws InterruptedException {
        return executeParalleled(i, list, data -> data.stream().map(Object::toString).collect(Collectors.joining())).stream().collect(Collectors.joining());

    }

    /**
     * Apply filter in parallel threads
     *
     * @param i         number of threads
     * @param list      list with data
     * @param predicate filter
     * @param <T>       used generic
     * @return return filtred list
     * @throws InterruptedException when something went wrong in some thread
     */
    @Override
    public <T> List<T> filter(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return executeParalleled(i, list, data -> data.stream().filter(predicate).collect(toList())).stream().flatMap(Collection::stream).collect(toList());
    }


    /**
     * Apply function in parallel threads
     *
     * @param i        number of threads
     * @param list     list with data
     * @param function function
     * @param <T>      used generic
     * @return return list with applied function
     * @throws InterruptedException when something went wrong in some thread
     */
    @Override
    public <T, U> List<U> map(int i, List<? extends T> list, Function<? super T, ? extends U> function) throws InterruptedException {
        return executeParalleled(i, list, data -> data.stream().map(function).collect(toList())).stream().flatMap(Collection::stream).collect(toList());
    }

    /**
     * Find maximum in parallel threads
     *
     * @param i          number of threads
     * @param list       list with data
     * @param comparator used comparator
     * @param <T>        used generic
     * @return return maximum in list
     * @throws InterruptedException when something went wrong in some thread
     */
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public <T> T maximum(int i, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return executeParalleled(i, list, data -> data.stream().max(comparator).get()).stream().max(comparator).get();
    }

    /**
     * Find minimum in parallel threads
     *
     * @param i          number of threads
     * @param list       list with data
     * @param comparator used comparator
     * @param <T>        used generic
     * @return return minimum in list
     * @throws InterruptedException when something went wrong in some thread
     * @see #maximum(int, List, Comparator)
     */
    @Override
    public <T> T minimum(int i, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(i, list, comparator.reversed());
    }

    /**
     * Check that all elements accepted by predicate
     *
     * @param i         number of threads
     * @param list      list with data
     * @param predicate predicate
     * @param <T>       used generic
     * @return return true if all accepted, false if not
     * @throws InterruptedException when something went wrong in some thread
     */
    @Override
    public <T> boolean all(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return executeParalleled(i, list, data -> data.stream().allMatch(predicate)).stream().allMatch(Predicate.isEqual(true));
    }

    /**
     * Check that any elements accepted by predicate
     *
     * @param i         number of threads
     * @param list      list with data
     * @param predicate predicate
     * @param <T>       used generic
     * @return return true if any accepted, false if not
     * @throws InterruptedException when something went wrong in some thread
     */
    @Override
    public <T> boolean any(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return executeParalleled(i, list, data -> data.stream().anyMatch(predicate)).stream().anyMatch(Predicate.isEqual(true));
    }

    private class Runner<T, R> implements Runnable {
        private T list;
        private Function<T, R> function;
        private R result;

        Runner(T list, Function<T, R> function) {
            this.list = list;
            this.function = function;
        }

        @Override
        public void run() {
            result = function.apply(list);
        }

        R getResult() {
            return result;
        }
    }

    private <T, R> List<R> executeParalleled(int n, List<? extends T> list, Function<List<? extends T>, R> function) throws InterruptedException {
        List<List<? extends T>> split = new ArrayList<>();
        int mod = list.size() % n;
        int l = 0;
        int r = list.size() / n;
        for (int i = 0; i < Math.min(n, list.size()); ++i) {
            if (mod > 0) {
                r++;
                mod--;
            }
            split.add(list.subList(l, r));
            l = r;
            r += list.size() / n;
        }
        if (parallelMapper != null) {
            return (parallelMapper.map(function, split));
        }
        List<Runner<?, R>> runners = split.stream().map(runner -> new Runner<>(runner, function)).collect(toList());
        List<Thread> threads = runners.stream().map(Thread::new).collect(toList());
        threads.forEach(Thread::start);
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            throw new InterruptedException(e.getMessage());
        }
        return runners.stream().map(Runner::getResult).collect(toList());
    }

}
