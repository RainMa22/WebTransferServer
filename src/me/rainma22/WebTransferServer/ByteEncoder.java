package me.rainma22.WebTransferServer;

import java.nio.charset.StandardCharsets;

public class ByteEncoder {
    protected static byte[] encodeString(String s){
        byte OPCODE=0x1;
        byte[] data=s.getBytes(StandardCharsets.UTF_8);
        int total=s.length()+14;
        int length=s.length();
        byte[] bytes=new byte[total];
        bytes[0]=(byte)(128+OPCODE);
        bytes[1]= (byte) (length);
        for (int i = 0; i < s.length(); i++) {
            bytes[2+i]= data[i];
        }
        return bytes;
    }
}
