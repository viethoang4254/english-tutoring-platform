package com.englishlearning.features.payment.repository;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.payment.dto.PaymentDto;
import com.englishlearning.features.payment.service.PaymentMoney;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepository {
    private final EntityManager em;
    public PaymentRepository(EntityManager em) { this.em=em; }
    public record Context(long course,long teacher) {}
    public record Price(BigDecimal tuition,String currency,BigDecimal percent,Instant start,Instant end) {}
    public record Destination(long id,String bin,String bank,String number,String holder) {}
    public Optional<Context> context(long student,long enrollment) {
        var rows=em.createNativeQuery("SELECT e.course_id,c.teacher_id FROM enrollments e JOIN courses c ON c.id=e.course_id WHERE e.id=:id AND e.student_id=:student")
            .setParameter("id",enrollment).setParameter("student",student).getResultList();
        if(rows.isEmpty())return Optional.empty();Object[] r=(Object[])rows.get(0);
        return Optional.of(new Context(number(r[0]),number(r[1])));
    }
    public Price price(long course) {
        Object[] r=(Object[])em.createNativeQuery("SELECT tuition,currency,discount_percent,discount_start_at,discount_end_at FROM courses WHERE id=:id")
            .setParameter("id",course).getSingleResult();
        return new Price((BigDecimal)r[0],(String)r[1],(BigDecimal)r[2],instant(r[3]),instant(r[4]));
    }
    public Optional<Destination> destination(long teacher) {
        var rows=em.createNativeQuery("SELECT id,bank_bin,bank_name,account_number,account_holder_name FROM teacher_payment_accounts WHERE teacher_id=:teacher AND review_status='APPROVED' AND status='ACTIVE' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL FOR UPDATE")
            .setParameter("teacher",teacher).getResultList();
        if(rows.isEmpty())return Optional.empty();Object[] r=(Object[])rows.get(0);
        return Optional.of(new Destination(number(r[0]),(String)r[1],(String)r[2],(String)r[3],(String)r[4]));
    }
    private static final String SELECT="SELECT p.id,p.enrollment_id,p.status,p.currency,p.original_amount,p.discount_percent,p.discount_amount,p.rounding_adjustment,p.final_amount,p.payment_code,p.teacher_payment_account_id,p.bank_bin,p.bank_name,p.account_number,p.account_holder_name,p.qr_payload,p.expires_at,p.created_at FROM payments p JOIN enrollments e ON e.id=p.enrollment_id WHERE e.student_id=:student";
    public Optional<PaymentDto> own(long student,long id) { return one(SELECT+" AND p.id=:id",student,id); }
    public Optional<PaymentDto> pending(long student,long enrollment) { return one(SELECT+" AND p.enrollment_id=:id AND p.status='PENDING'",student,enrollment); }
    private Optional<PaymentDto> one(String query,long student,long id) {
        var rows=em.createNativeQuery(query).setParameter("student",student).setParameter("id",id).getResultList();
        return rows.isEmpty()?Optional.empty():Optional.of(dto((Object[])rows.get(0)));
    }
    public long insert(long enrollment,Destination d,PaymentMoney m,String reference,String payload,Instant deadline,Instant now) {
        return number(em.createNativeQuery("""
            INSERT INTO payments(enrollment_id,teacher_payment_account_id,payment_code,provider,original_amount,
              discount_amount,final_amount,currency,status,expires_at,created_at,updated_at,monetary_policy,
              discount_percent,rounding_adjustment,bank_bin,bank_name,account_number,account_holder_name,qr_payload)
            VALUES (:enrollment,:account,:reference,'VIETQR',:original,:discount,:final,'VND','PENDING',:deadline,
              :now,:now,'VND_HALF_UP_V1',:percent,:rounding,:bin,:bank,:number,:holder,:payload) RETURNING id
            """).setParameter("enrollment",enrollment).setParameter("account",d.id()).setParameter("reference",reference)
            .setParameter("original",m.original()).setParameter("discount",m.discount()).setParameter("final",m.payable())
            .setParameter("deadline",deadline).setParameter("now",now).setParameter("percent",m.percent()).setParameter("rounding",m.rounding())
            .setParameter("bin",d.bin()).setParameter("bank",d.bank()).setParameter("number",d.number()).setParameter("holder",d.holder())
            .setParameter("payload",payload).getSingleResult());
    }
    private static PaymentDto dto(Object[] r) {
        return new PaymentDto(new ApiId(number(r[0])),new ApiId(number(r[1])),(String)r[2],(String)r[3],money(r[4]),money(r[5]),money(r[6]),money(r[7]),money(r[8]),
            (String)r[9],new ApiId(number(r[10])),(String)r[11],(String)r[12],(String)r[13],(String)r[14],(String)r[15],instant(r[16]),instant(r[17]));
    }
    private static String money(Object value) {return value==null?null:((BigDecimal)value).stripTrailingZeros().toPlainString();}
    private static long number(Object value) { return ((Number)value).longValue(); }
    private static Instant instant(Object value) { return value==null?null:value instanceof Instant i?i:value instanceof OffsetDateTime o?o.toInstant():((java.sql.Timestamp)value).toInstant(); }
}
