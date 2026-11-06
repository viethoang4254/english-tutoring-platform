package com.englishlearning.features.paymentaccount;
import com.englishlearning.features.paymentaccount.service.*;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
public class ReceivingImageTests {
    public static byte[] png() {
        try {var image=new BufferedImage(32,32,BufferedImage.TYPE_INT_RGB);for(int x=0;x<32;x++)for(int y=0;y<32;y++)image.setRGB(x,y,(x+y)%2==0?0:0xffffff);var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();}
        catch(IOException e){throw new AssertionError(e);}
    }
    @Test void losslessPixelsAndTrailingDataRemoved()throws Exception {
        byte[] raw=png(),extra=java.util.Arrays.copyOf(raw,raw.length+20);byte[] result=QrImage.sanitize(extra);
        var before=ImageIO.read(new ByteArrayInputStream(raw));var after=ImageIO.read(new ByteArrayInputStream(result));
        assertEquals(32,after.getWidth());for(int x=0;x<32;x++)for(int y=0;y<32;y++)assertEquals(before.getRGB(x,y),after.getRGB(x,y));
        assertArrayEquals(result,QrImage.sanitize(result));assertTrue(result.length<extra.length);
    }
    @Test void jpegAcceptedWithoutResizing()throws Exception {
        var out=new ByteArrayOutputStream();ImageIO.write(ImageIO.read(new ByteArrayInputStream(png())),"jpeg",out);
        var image=ImageIO.read(new ByteArrayInputStream(QrImage.sanitize(out.toByteArray())));assertEquals(32,image.getWidth());assertEquals(32,image.getHeight());
    }
    @Test void rejectsMalformedOversizeAndDimensions()throws Exception {
        for(byte[] bad:new byte[][]{new byte[0],new byte[QrImage.MAX+1],"<svg/>".getBytes()})assertThrows(ReceivingFailure.class,()->QrImage.sanitize(bad));
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(4097,1,BufferedImage.TYPE_INT_RGB),"png",out);
        assertThrows(ReceivingFailure.class,()->QrImage.sanitize(out.toByteArray()));
    }
    @Test void directoryUsesPinnedStringBinsAndRejectsUnsupported() {
        var d=new BankDirectory(tools.jackson.databind.json.JsonMapper.builder().build());
        assertEquals("VCB",d.require("970436").code());
        for(String bin:new String[]{"000000","970455",null,"970436 "})assertThrows(ReceivingFailure.class,()->d.require(bin));
    }
}
