package com.protoevo.networking;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    private ObjectOutputStream out;
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
            server = new ServerSocket(port); // start listening on port
            status = Status.WAITING.setMessage("Server waiting for connection on port " + port);
            client = server.accept(); // this method is a blocking I/O call, it will not be called unless
            // a connection is established.
            out = new ObjectOutputStream(client.getOutputStream()); // get the output stream of client.
            in = new ObjectInputStream(client.getInputStream());    // get the input stream of client.
            status = Status.CONNECTED;
            opened = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Optional<Object> get() {
        if (!opened) open();

        try {
            status = Status.WAITING.setMessage("Connection Established. Waiting to receive from client");
            System.out.println(status);
            Object obj = in.readObject();
            return Optional.of(obj);

        } catch (Exception e) {
            e.printStackTrace();
            status = Status.FAILED.setMessage("Failed to receive from client: " + e.getMessage());
        }
        return Optional.empty();
    }

    public void close() {
        try {
            out.close();
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
        Server server = new Server(8888);
        Optional<Object> obj = server.get();
        System.out.println("Received:" + obj);
    }
}
