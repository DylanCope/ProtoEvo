package com.protoevo.networking;

import com.protoevo.env.Environment;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

public class Server {

    public enum Status {
        CLOSED("Server not opened"),
        WAITING("Waiting for client on port"),
        CONNECTED("Established connection with client"),
        RECEIVING("Waiting for environment from client"),
        FAILED("Failed to receive from client");

        private String message;

        Status(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public Status setMessage(String message) {
            this.message = message;
            return this;
        }

        public String toString() {
            return message;
        }
    }

    private final int port;
    private Socket client;
    private ObjectInputStream in;
    private ServerSocket server;
    private boolean opened = false;
    private Status status;

    public Server(int port) {
        this.port = port;
        status = Status.CLOSED;
    }

    private void open() {
        try {
//            TCPObjectServer server = new TCPObjectServer(port);
            server = new ServerSocket(port); // start listening on port
            status = Status.WAITING.setMessage("Server waiting for connection on port " + port);
            client = server.accept(); // this method is a blocking I/O call, it will not be called unless
            // a connection is established.

            client.setTcpNoDelay(true);
//            in = new FSTObjectInput(client.getInputStream(), EnvFileIO.getFSTConfig());
            in = new ObjectInputStream(client.getInputStream());    // get the input stream of client.
            status = Status.CONNECTED;
            opened = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T> Optional<T> get(Class<T> clazz) {
        if (!opened) open();

        try {
            status = Status.WAITING.setMessage("Connection Established. Waiting to receive from client");
            System.out.println(status);
//            T obj = (T) in.readObject(clazz);
            Object obj = in.readObject();
            if (clazz.isInstance(obj))
                return Optional.of(clazz.cast(obj));
            status = Status.FAILED.setMessage("Received object is not of type " + clazz.getSimpleName());
        } catch (Exception e) {
            e.printStackTrace();
            status = Status.FAILED.setMessage("Failed to receive from client: " + e.getMessage());
        }
        return Optional.empty();
    }

    public void close() {
        try {
            in.close();
            client.close();
            server.close();
            opened = false;
            status = Status.CLOSED;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Status getStatus() {
        return status;
    }

    public static void main(String[] args) {
        Server server = new Server(1212);
        Optional<Environment> obj = server.get(Environment.class);
        System.out.println("Received:" + obj);
        obj.ifPresent(env -> {
            env.createTransientObjects();
            System.out.println(env.getStats());
        });

        obj = server.get(Environment.class);
        System.out.println("Received:" + obj);
        obj.ifPresent(env -> {
            env.createTransientObjects();
            System.out.println(env.getStats());
        });
    }
}
