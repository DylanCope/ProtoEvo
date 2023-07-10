package com.protoevo.networking;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {

    public static class Student implements Serializable {
        private int studentAvg;
        private String studentName;
        public Student(int studentAvg, String studentName) {
            this.studentAvg = studentAvg;
            this.studentName = studentName;
        }

        public int getStudentAvg() {
            return studentAvg;
        }

        public String getStudentName() {
            return studentName;
        }
    }

    public static void main(String[] args) {
        Socket client = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            client = new Socket("saintsaviourslodge.ddns.net", 8888);
            out = new ObjectOutputStream(client.getOutputStream());
            in = new ObjectInputStream(client.getInputStream());
            Student student = new Student(50, "Dylan");
            out.writeObject(student);
            out.flush();

//close resources

            out.close();
            in.close();
            client.close();

        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
