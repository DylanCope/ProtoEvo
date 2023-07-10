package com.protoevo.networking;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) {
        Socket client = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            ServerSocket server = new ServerSocket(8888); // start listening on port 8888
            client = server.accept(); // this method is a blocking I/O call, it will not be called unless
//a connection is established.
            out = new ObjectOutputStream(client.getOutputStream()); // get the output stream of client.
            in = new ObjectInputStream(client.getInputStream());    // get the input stream of client.

            Client.Student student = (Client.Student) in.readObject(); // cast a Student class from reading the object
            // that was sent to the server by the client.
            System.out.println("Average: " + student.getStudentAvg() + " Name: " + student.getStudentName());

// close resources
            out.close();
            in.close();
            client.close();
            server.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
