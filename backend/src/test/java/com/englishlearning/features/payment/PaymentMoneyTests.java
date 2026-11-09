package com.englishlearning.features.payment;

import com.englishlearning.features.payment.service.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaymentMoneyTests {
    PaymentMoney money(String tuition,String percent){return PaymentMoney.calculate(new BigDecimal(tuition),new BigDecimal(percent));}
    void amount(String expected,BigDecimal actual){assertEquals(0,new BigDecimal(expected).compareTo(actual));}
    @Test void halfUpWithoutDiscountPreservesTuition() {
        var up=money("100.50","0");amount("101",up.payable());amount("0.50",up.rounding());amount("100.50",up.original());amount("0",up.discount());
        var down=money("100.49","0");amount("100",down.payable());amount("-0.49",down.rounding());
    }
    @Test void exactPercentageIsNotRoundedEarly() {
        var m=money("123.45","12.34");amount("15.233730",m.discount());amount("108",m.payable());amount("-0.216270",m.rounding());
        amount("12.34",m.percent());amount("123.45",m.original());
    }
    @Test void halfBoundaryAndTinyAmounts() {
        amount("1",money("1","50").payable());
        for(String tuition:new String[]{"0","-1","0.49"})assertThrows(PaymentFailure.class,()->money(tuition,"0"));
        assertThrows(PaymentFailure.class,()->money("1","99.99"));
    }
    @Test void invalidPrecisionAndAmountsRejected() {
        for(String tuition:new String[]{"1.001","9999999999999.99"})assertThrows(PaymentFailure.class,()->money(tuition,"0"));
        for(String percent:new String[]{"-1","100","1.001"})assertThrows(PaymentFailure.class,()->money("100",percent));
    }
    @Test void immutableSnapshotDoesNotFollowLaterPrices() {
        var old=money("100.50","0");var later=money("200","10");
        amount("101",old.payable());amount("100.50",old.original());amount("180",later.payable());
    }
}
