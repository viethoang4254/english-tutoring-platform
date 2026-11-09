package com.englishlearning.features.payment.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.imageio.ImageIO;

public final class VietQr {
    private VietQr() {}
    public static String payload(String bin,String account,String amount,String reference) {
        if(bin==null || !bin.matches("[0-9]{6}") || account==null || !account.matches("[0-9]{6,19}")
            || amount==null || !amount.matches("[1-9][0-9]{0,12}") || reference==null || !reference.matches("ELP[0-9A-F]{22}"))
            throw new PaymentFailure(PaymentFailure.Reason.RECEIVING_ACCOUNT_UNAVAILABLE);
        String receiver=tlv("00",bin)+tlv("01",account);
        String merchant=tlv("00","A000000727")+tlv("01",receiver)+tlv("02","QRIBFTTA");
        String data=tlv("00","01")+tlv("01","12")+tlv("38",merchant)+tlv("53","704")
            +tlv("54",amount)+tlv("58","VN")+tlv("62",tlv("08",reference))+"6304";
        return data+crc(data);
    }
    private static String tlv(String tag,String value) {
        if(value.length()>99) throw new IllegalArgumentException("QR field too long");
        return tag+String.format(java.util.Locale.ROOT,"%02d",value.length())+value;
    }
    public static String crc(String value) {
        int crc=0xffff;
        for(byte b:value.getBytes(StandardCharsets.US_ASCII)) {
            crc^=(b&255)<<8;
            for(int i=0;i<8;i++) crc=((crc&0x8000)!=0?(crc<<1)^0x1021:crc<<1)&0xffff;
        }
        return String.format(java.util.Locale.ROOT,"%04X",crc);
    }
    public static byte[] png(String payload) {
        if(payload==null || payload.length()<8 || payload.length()>512 || !payload.substring(payload.length()-8,payload.length()-4).equals("6304")
            || !crc(payload.substring(0,payload.length()-4)).equals(payload.substring(payload.length()-4)))
            throw new PaymentFailure(PaymentFailure.Reason.PAYMENT_LEGACY_UNAVAILABLE);
        try {
            var matrix=new QRCodeWriter().encode(payload,BarcodeFormat.QR_CODE,480,480,
                Map.of(EncodeHintType.ERROR_CORRECTION,ErrorCorrectionLevel.M,EncodeHintType.MARGIN,4));
            var image=new BufferedImage(480,480,BufferedImage.TYPE_INT_RGB);
            for(int y=0;y<480;y++) for(int x=0;x<480;x++) image.setRGB(x,y,matrix.get(x,y)?0x000000:0xffffff);
            var out=new ByteArrayOutputStream();
            if(!ImageIO.write(image,"png",out)) throw new IllegalStateException("PNG encoder unavailable");
            return out.toByteArray();
        } catch(com.google.zxing.WriterException | java.io.IOException e) { throw new IllegalStateException("QR generation unavailable"); }
    }
}
