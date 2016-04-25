package ru.ifmo.ctddev.zemskov.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Parallel mapper implementation
 *
 * @author Kirill Zemskov
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final Queue<Order> queue = new ArrayDeque<>();
    private final List<Thread> threads = new ArrayList<>();

    private enum Status {NOT_STARTED, PROCESSING, READY, ABORTED}

    /**
     * Constructor create threads that ready fo execute orders
     *
     * @param number number of orders
     */
    public ParallelMapperImpl(int number) {
        Runnable runner = () -> {
            try {
                while (!Thread.interrupted()) {
                    Order order;
                    synchronized (queue) {
                        while (queue.isEmpty()) {
                            queue.wait();
                        }
                        order = queue.poll();
                    }
                    order.execute();
                }
            } catch (InterruptedException e) {
                //interrupted
            } finally {
                Thread.currentThread().interrupt();
            }
        };
        for (int i = 0; i < number; i++) {
            threads.add(new Thread(runner));
        }
        threads.stream().forEach(Thread::start);
    }

    /**
     * Returns a {@code List} consisting of the results of applying the given
     * {@link java.util.function.Function} to the elements of this {@code List}.
     *
     * @param function function to apply to elements
     * @param list     list to process
     * @param <T>      Generic type of list data
     * @param <R>      Generic type of return
     * @return the new {@code List}
     * @throws InterruptedException if some of created threads was interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list) throws InterruptedException {
        Function<? super T, Order<T, R>> pushOrder = (data -> {
            Order<T, R> order = new Order<>(function, data);
            synchronized (queue) {
                queue.add(order);
                queue.notify();
            }
            return order;
        });
        return list.stream().map(pushOrder).collect(Collectors.toList()).stream().map(Order::get).collect(Collectors.toList());
    }

    /**
     * Shutdowns all threads, used for mapping.
     *
     * @throws InterruptedException if some of created threads was interrupted
     */
    @Override
    public void close() throws InterruptedException {
        synchronized (queue) {
            queue.forEach(Order::cancel);
            queue.clear();
        }
        threads.forEach(Thread::interrupt);
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private class Order<T, R> {
        private final Function<? super T, ? extends R> function;
        private final T argument;
        private volatile R answer;
        private volatile Status status;
        private volatile Thread runner = null;

        private Order(Function<? super T, ? extends R> function, T argument) {
            this.function = function;
            this.argument = argument;
            status = Status.NOT_STARTED;
        }

        private synchronized R get() {
            while (status != Status.ABORTED && status != Status.READY) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }
            return answer;
        }

        private synchronized void execute() {
            if (status != Status.NOT_STARTED) {
                return;
            }
            status = Status.PROCESSING;
            runner = Thread.currentThread();
            try {
                answer = function.apply(argument);
                status = Status.READY;
                notify();
            } catch (Exception e) {
                status = Status.ABORTED;
                notify();
                throw e;
            } finally {
                runner = null;
            }
        }

        private synchronized void cancel() {
            if (status != Status.READY) {
                status = Status.ABORTED;
            }
            if (runner != null) {
                runner.interrupt();
            }
        }
    }
}
