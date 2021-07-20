package me.rainma22.WebTransferServer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server extends Thread{
    private byte key[]=new byte[4];
    private byte opcode;
    private boolean FIN;
    private long length=0;
    private ServerSocket s;
    public String workType="";
    public SocketAddress socketAddress;
    public Server() throws IOException {
        s=Main.serverSocket;
        socketAddress=null;
        this.start();
    }

    @Override
    public void run(){
        try {
            listen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void parseRequest(String request){
        String[] r=request.split(" ");
        switch (r[0]){
            case "get":
                workType="get";
                break;
            case "set":
                workType="set";
                break;
        }
    }
    private void listen() throws Exception{
        Socket client= s.accept();
        new Server();
        handshake(client);

        DataOutputStream out=new DataOutputStream(client.getOutputStream());
        socketAddress=client.getRemoteSocketAddress();
        System.out.println(client.getInetAddress());
        //Scanner in=new Scanner(new InputStreamReader(client.getInputStream()));
        ArrayList<Integer> encoded=parseData(client);
        byte[] decoded=decode(encoded);
        if (opcode==1){
            String request=new String(decoded, StandardCharsets.UTF_8);
            parseRequest(request);
        }
        byte[] data=ByteEncoder.encodeString("file not found!");
        out.write(data,0,data.length);
        Thread.sleep(1000);
        client.close();
    }
    private void handshake(Socket client) throws IOException, NoSuchAlgorithmException {
        DataOutputStream out=new DataOutputStream(client.getOutputStream());
        Scanner in=new Scanner(new InputStreamReader(client.getInputStream()));
        String data=in.useDelimiter("\\r\\n\\r\\n").next();
        Matcher get = Pattern.compile("^GET").matcher(data);

        if (get.find()) {
            Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
            match.find();
            byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Sec-WebSocket-Accept: "
                    + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")))
                    + "\r\n\r\n").getBytes("UTF-8");
            //key=match.group(1).getBytes();

            out.write(response, 0, response.length);
        }
    }
    private ArrayList<Integer> parseData(Socket client)throws IOException{
        ArrayList<Integer> encoded=new ArrayList<>(10);
        int i,counter=0;boolean sixteenBitLength=false,sixtyFourBitLength=false;
        while ((i=client.getInputStream().read())!=(byte)'\n'){
            switch (counter){
                case 0:
                    int tmp=i;
                    FIN=tmp >=128;
                    opcode=(byte)(i-128);
                    break;
                case 1:
                    tmp= (byte) (i-128);
                    sixteenBitLength=tmp==126;sixtyFourBitLength=tmp==127;
                    if (!sixteenBitLength&&!sixtyFourBitLength){
                        length=tmp+128-125;
                    }
                    break;
                case 2:
                case 3:
                    if (sixteenBitLength||sixtyFourBitLength){
                        int it=i*(int)(Math.pow(256,counter-2));
                        length+=it;
                    }else{
                        key[counter-2]=(byte)i;
                    }
                    break;
                case 4:
                case 5:
                    if (sixteenBitLength){
                        key[counter-4]=(byte)i;
                    }else if (sixtyFourBitLength){
                        int it=i*(int)(Math.pow(256,counter-2));
                        length+=it;
                    }else{
                        key[counter-2]=(byte)i;
                    }
                    break;
                case 6:
                case 7:
                    if (sixteenBitLength){
                        key[counter-4]=(byte)i;
                    }else if (sixtyFourBitLength){
                        int it=i*(int)(Math.pow(256,counter-2));
                        length+=it;
                    }else{
                        encoded.add(i);
                    }
                    break;
                case 8:
                case 9:
                    if (sixtyFourBitLength){
                        int it=i*(int)(Math.pow(256,counter-2));
                        length+=it;
                        break;
                    }
                default:
                    encoded.add(i);


            }

            //System.out.println((char)(i));
            //^ key[i & 0x3]
            counter++;
            if (length!=0&&counter>=length) break;
        }
        return encoded;
    }
    private byte[] decode(ArrayList<Integer> encoded){
        byte[] decoded=new byte[encoded.size()];
        for (int i = 0; i < encoded.size(); i++) {
            decoded[i] = (byte)(encoded.get(i)^key[i %4]);
        }
        return decoded;
    }
}
