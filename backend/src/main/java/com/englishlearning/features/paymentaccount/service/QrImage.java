package com.englishlearning.features.paymentaccount.service;
import java.io.*;
import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
public final class QrImage {
    public static final int MAX=5*1024*1024;
    private QrImage() {}
    public static byte[] sanitize(byte[] input) {
        if(input==null || input.length==0 || input.length>MAX) throw invalid();
        try(var stream=new MemoryCacheImageInputStream(new ByteArrayInputStream(input))) {
            var readers=ImageIO.getImageReaders(stream); if(!readers.hasNext()) throw invalid();
            var reader=readers.next();
            try {
                String format=reader.getFormatName();
                if(!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg")) throw invalid();
                reader.setInput(stream,true,true);
                int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w<1 || h<1 || w>4096 || h>4096 || (long)w*h>16_000_000) throw invalid();
                var decoded=reader.read(0); if(decoded==null) throw invalid();
                // Lossless PNG, unchanged dimensions, white alpha background; no metadata or resampling.
                var clean=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
                var graphics=clean.createGraphics();
                try { graphics.setColor(java.awt.Color.WHITE);graphics.fillRect(0,0,w,h);graphics.drawImage(decoded,0,0,null); }
                finally { graphics.dispose(); }
                var output=new ByteArrayOutputStream();
                var bounded=new FilterOutputStream(output) {
                    int size;
                    @Override public void write(int b)throws IOException { if(++size>MAX)throw new IOException();out.write(b); }
                    @Override public void write(byte[] b,int off,int len)throws IOException { if((long)size+len>MAX)throw new IOException();size+=len;out.write(b,off,len); }
                };
                try(var imageOut=new javax.imageio.stream.MemoryCacheImageOutputStream(bounded)) {
                    if(!ImageIO.write(clean,"png",imageOut))throw invalid();
                }
                return output.toByteArray();
            } finally { reader.dispose(); }
        } catch(Exception e) { throw invalid(); }
    }
    public static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable."); }
    }
    private static ReceivingFailure invalid(){return new ReceivingFailure(ReceivingFailure.Reason.INVALID);}
}
