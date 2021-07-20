package me.rainma22.WebTransferServer;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;

public class Main {
    public static ServerSocket serverSocket;
    public static ArrayList<Server> serverPool=new ArrayList<>();
    public static void main(String[] args) throws IOException, InterruptedException {
        serverSocket=new ServerSocket(2020);
        new Server();
        while(true){
            Thread.sleep(1000);
        }
    }
    public synchronized void addToList(Server s){
        serverPool.add(s);
    }
    public synchronized static void requestClient(InetAddress inetAddress){
        for (int i = 0; i < serverPool.size(); i++) {
            Server s= serverPool.get(i);
            InetSocketAddress isa=(InetSocketAddress) (s.socketAddress);
            if (inetAddress.equals(isa.getAddress())){

            }
        }
    }
}
