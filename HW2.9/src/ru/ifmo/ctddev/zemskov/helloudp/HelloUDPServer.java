package ru.ifmo.ctddev.zemskov.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

/**
 * Server that receives request, processes them and sends responses.
 * Server will reply to every request if the number of threads it is working on allows.
 * The reply will be {@code "Hello, " + received}, where "receive" is the string representation of the request.
 * <p>
 * Server can be created from the command line with two parameters:
 * port and number of threads.
 * <p>
 * Server is started with the method {@code start}.
 * <p>
 * To use method start user has to provide two integers - port and threads' number.
 * <p>
 * Server can be started several times on different ports.
 * <p>
 * To close the server there is the method {@code close}.
 *
 * @author Kirill Zemskov
 * @see #start
 */
public class HelloUDPServer implements HelloServer{
    private static final String USAGE = "Usage: port number_of_threads";
    private final ConcurrentLinkedQueue<DatagramSocket> sockets = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ExecutorService> services = new ConcurrentLinkedQueue<>();

    /**
     * Method to create class and execute from the command line. Usage for parameters to provide:
     * {@code port number_of_threads}
     * port - port to start the server on
     * number_of_threads - number of threads to process request on
     *
     * @param args arguments of the command line
     * @see #start
     */
    public static void main(String[] args) {
        if (args == null || args.length < 2 || Arrays.stream(args).anyMatch(Predicate.isEqual(null))) {
            System.out.println(USAGE);
            return;
        }
        try {
            int port = Integer.parseInt(args[0]);
            int threads = Integer.parseInt(args[1]);
            if (port < 0 || port > 0xFFFF || threads < 1) {
                throw new NumberFormatException();
            }
            new HelloUDPServer().start(port, threads);
        } catch (NumberFormatException e) {
            System.out.println(USAGE);
        }
    }

    /**
     * Method to start server with the given parameters
     *
     * @param port    number of the port to start server on
     * @param threads number of threads to process requests on
     * @see java.util.concurrent.ExecutorService
     * @see java.net.DatagramSocket
     * @see java.net.DatagramPacket
     */
    public void start(int port, int threads) {
        ExecutorService service = Executors.newFixedThreadPool(threads);
        DatagramSocket socket;
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new IllegalStateException("Error during creating socket: " + e.getMessage());
        }

        sockets.add(socket);
        services.add(service);
        Runnable worker = () -> {
            while (!Thread.interrupted() && !socket.isClosed()) {
                try (DatagramSocket sendingSocket = new DatagramSocket()) {
                    sendingSocket.setSoTimeout(300);
                    int bSize = socket.getReceiveBufferSize();
                    DatagramPacket request = new DatagramPacket(new byte[bSize], bSize);
                    try {
                        socket.receive(request);
                    } catch (IOException e) {
                        continue;
                    }
                    InetAddress clientAddress = request.getAddress();
                    int clientPort = request.getPort();
                    String responseString = new String(("Hello, " + new String(request.getData(), 0, request.getLength(), Charset.forName("UTF8"))).getBytes(), "UTF8");
                    DatagramPacket response = new DatagramPacket(responseString.getBytes("UTF8"), responseString.getBytes().length, clientAddress, clientPort);
                    sendingSocket.send(response);
                } catch (IOException e) {
                    //ignore
                }
            }
        };
        for (int thread = 0; thread < threads; thread++) {
            service.execute(worker);
        }
    }

    /**
     * Closes server. {@link #start(int, int)} can't be used after invocation of this method.
     */
    public void close() {
        sockets.forEach(DatagramSocket::close);
        services.forEach(ExecutorService::shutdownNow);
        sockets.clear();
        services.clear();
    }
}
