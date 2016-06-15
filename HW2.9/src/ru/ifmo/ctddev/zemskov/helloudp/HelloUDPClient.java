package ru.ifmo.ctddev.zemskov.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Basic implementation of {@link HelloClient}. Provides way to send simple messages to some server.
 *
 * @author Kirill Zemskov
 */
public class HelloUDPClient implements HelloClient {
    private static final String USAGE = "Usage: java HelloUDPClient <hostname> <port> <prefix> <requests> <threads>";
    private static final int TIMEOUT = 300;

    /**
     * Creates {@link HelloUDPClient} and uses it's {@link #start(String, int, String, int, int)} method with arguments
     * from {@code args}.
     *
     * @param args arguments, which will be passed to created client
     */
    public static void main(String[] args) {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Predicate.isEqual(null))) {
            System.err.println(USAGE);
            return;
        }
        int port, requests, threads;
        try {
            port = Integer.parseInt(args[1]);
            requests = Integer.parseInt(args[3]);
            threads = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.err.println(USAGE);
            return;
        }
        if (port < 0 || port > 0xFFFF || threads < 1 || requests < 1) {
            throw new NumberFormatException();
        }
        try {
            new HelloUDPClient().start(args[0], port, args[2], requests, threads);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Method to start sending requests.
     *
     * @param host     address to send requests to
     * @param port     port to connect to
     * @param prefix   the prefix of the request
     * @param requests number of the requests per thread
     * @param threads  number of the threads to send requests from
     * @see java.util.concurrent.ExecutorService
     * @see java.net.InetAddress
     * @see java.net.DatagramSocket
     * @see java.net.DatagramPacket
     */
    public void start(String host, int port, String prefix, int requests, int threads) {
        ExecutorService threadPool = Executors.newFixedThreadPool(threads);
        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Unknown host: " + e.getMessage());
        }
        try {
            for (int thread = 0; thread < threads; thread++) {
                final int threadId = thread;
                threadPool.submit(() -> {
                    try (DatagramSocket socket = new DatagramSocket()) {
                        socket.setSoTimeout(TIMEOUT);
                        for (int req = 0; req < requests; req++) {
                            String request = new String((prefix + threadId + "_" + req).getBytes("UTF8"), "UTF8");
                            int len = request.getBytes("UTF8").length;
                            DatagramPacket sendingPacket = new DatagramPacket(request.getBytes("UTF8"), len, address, port);
                            DatagramPacket receivedPacket = new DatagramPacket(new byte[len + 8], len + 8);
                            String required = new String(("Hello, " + request).getBytes("UTF8"), "UTF8");
                            String received = "";
                            while (!required.equals(received)) {
                                try {
                                    socket.send(sendingPacket);
                                    socket.receive(receivedPacket);
                                    received = new String(receivedPacket.getData(), 0, receivedPacket.getLength(), Charset.forName("UTF8"));
                                } catch (IOException e) {
                                    System.err.println("Error during sending packet");
                                }
                            }
                            System.out.println(received);
                        }

                    } catch (SocketException e) {
                        throw new IllegalStateException("Error during creating socket");
                    } catch (UnsupportedEncodingException e) {
                        System.err.println("Encoding not supported");
                    }
                });
            }
            threadPool.shutdownNow();
            threadPool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            ///ignore
        }
    }
}
