package com.protoevo.networking;

import org.nustaq.serialization.FSTObjectOutput;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {

    public enum Status {
        CLOSED("Client not opened"),
        OPEN("Client open"),
        SENDING("Sending to server"),
        SENT_SUCCESSFUL("Sent to server"),
        FAILED("Failed to send to server");

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

    private final String address;
    private final int port;
    private Socket client;
    private FSTObjectOutput out;
    private boolean opened = false;
    private Status status;

    public Client(String address, int port) {
        this.address = address;
        this.port = port;
        status = Status.CLOSED;
    }

    public void open() {
        try {
            client = new Socket(address, port);
            out = new FSTObjectOutput(client.getOutputStream());
            opened = true;
            status = Status.OPEN;
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + address);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + address);
            System.exit(1);
        }
    }

    public Status getStatus() {
        return status;
    }

    public void send(Object obj, Class<?> clazz) {
        if (!opened) open();

        try {
//            out.writeUnshared(obj);
//            out.flush();
//            out.reset();

            status = Status.SENDING;
            out.writeObject(obj, clazz);
            out.flush();
            out.resetForReUse();
            status = Status.SENT_SUCCESSFUL;

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void close() {
        try {
            out.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {}

}
