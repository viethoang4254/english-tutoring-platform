package com.englishlearning.features.payment;

import com.englishlearning.features.payment.service.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import java.io.ByteArrayInputStream;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VietQrTests {
    final String reference="ELP0123456789ABCDEF012345";
    @Test void documentedVietQrExampleChecksum() {
        // Published response example: https://www.vietqr.io/generate (not a confirmation API).
        String sample="00020101021238560010A0000007270126000697041501121133666688880208QRIBFTTA53037045405790005802VN62220818Ung Ho Quy Vac Xin6304";
        assertEquals("3ACF",VietQr.crc(sample));
        assertEquals("29B1",VietQr.crc("123456789"));
    }
    @Test void independentTlvParsingChecksExactDestinationAndAmount() {
        var tags=parse(VietQr.payload("970415","113366668888","79000",reference));
        assertEquals("01",tags.get("00"));assertEquals("12",tags.get("01"));assertEquals("704",tags.get("53"));assertEquals("VN",tags.get("58"));
        assertEquals("79000",tags.get("54"));var merchant=parse(tags.get("38"));
        assertEquals("A000000727",merchant.get("00"));assertEquals("QRIBFTTA",merchant.get("02"));
        assertEquals(Map.of("00","970415","01","113366668888"),parse(merchant.get("01")));
        assertEquals(reference,parse(tags.get("62")).get("08"));assertEquals(25,reference.length());
    }
    @Test void pngDecodesIndependentlyToStoredPayload()throws Exception {
        String payload=VietQr.payload("970415","113366668888","79000",reference);
        var image=ImageIO.read(new ByteArrayInputStream(VietQr.png(payload)));
        var pixels=image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());
        var decoded=new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(image.getWidth(),image.getHeight(),pixels))));
        assertEquals(payload,decoded.getText());
    }
    @Test void invalidFieldsAndCorruptedPayloadRejected() {
        assertThrows(PaymentFailure.class,()->VietQr.payload("970415","12345678901234567890","1",reference));
        assertThrows(PaymentFailure.class,()->VietQr.payload("970415","123456","0",reference));
        assertThrows(PaymentFailure.class,()->VietQr.png("invalid"));
    }
    private Map<String,String> parse(String value) {
        var fields=new LinkedHashMap<String,String>();
        for(int at=0;at<value.length();) {
            String tag=value.substring(at,at+2);int length=Integer.parseInt(value.substring(at+2,at+4));
            assertNull(fields.put(tag,value.substring(at+4,at+4+length)));at+=4+length;
        }
        return fields;
    }
}
