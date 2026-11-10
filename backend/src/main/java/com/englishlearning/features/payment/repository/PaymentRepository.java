package com.englishlearning.features.payment.repository;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.payment.dto.PaymentDto;
import com.englishlearning.features.payment.dto.ReconciliationEvidence;
import com.englishlearning.features.payment.dto.ReconciliationAttempt;
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
    public record ReconciliationContext(long student,long enrollment) {}
    public Optional<ReconciliationContext> teacherContext(long teacher,long course,long payment) {
        var found=em.createNativeQuery("SELECT e.student_id,e.id FROM payments p JOIN enrollments e ON e.id=p.enrollment_id JOIN courses c ON c.id=e.course_id WHERE p.id=:id AND c.id=:course AND c.teacher_id=:teacher")
            .setParameter("id",payment).setParameter("course",course).setParameter("teacher",teacher).getResultList();
        if(found.isEmpty())return Optional.empty();Object[] r=(Object[])found.get(0);
        return Optional.of(new ReconciliationContext(number(r[0]),number(r[1])));
    }
    public List<PaymentDto> teacherPage(long teacher,long course,int page,int size) {
        String query=SELECT.replace("WHERE e.student_id=:student","JOIN courses c ON c.id=e.course_id WHERE c.teacher_id=:teacher AND c.id=:course");
        return ((List<?>)em.createNativeQuery(query+" ORDER BY p.id DESC").setParameter("teacher",teacher).setParameter("course",course)
            .setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size+1).getResultList()).stream().map(r->dto((Object[])r)).toList();
    }
    public PaymentDto lockPayment(long student,long payment) {
        return one(SELECT+" AND p.id=:id FOR UPDATE OF p",student,payment).orElseThrow();
    }
    public boolean conflictingObligation(long enrollment,long payment) {
        return (Boolean)em.createNativeQuery("SELECT EXISTS(SELECT 1 FROM payments WHERE enrollment_id=:e AND id<>:p AND status IN ('PENDING','CONFIRMED'))")
            .setParameter("e",enrollment).setParameter("p",payment).getSingleResult();
    }
    public void lockBankTransaction(PaymentDto payment,String bankReference) {
        // Serialize even when two Teachers registered the same physical receiving account.
        em.createNativeQuery("SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(:identity,0))")
            .setParameter("identity",payment.bankBin()+":"+payment.accountNumber()+":"+bankReference).getSingleResult();
    }
    public Optional<ReconciliationAttempt> allocation(PaymentDto payment,String bankReference) {
        var found=em.createNativeQuery(AUDIT+" WHERE bank_bin=:b AND account_number=:n AND bank_transaction_reference=:r AND allocated")
            .setParameter("b",payment.bankBin()).setParameter("n",payment.accountNumber()).setParameter("r",bankReference).getResultList();
        return found.isEmpty()?Optional.empty():Optional.of(attempt((Object[])found.get(0)));
    }
    public void activate(long payment,long enrollment,Instant now) {
        int p=em.createNativeQuery("UPDATE payments SET status='CONFIRMED',confirmed_at=:now,updated_at=:now WHERE id=:id AND status IN ('PENDING','EXPIRED')")
            .setParameter("now",now).setParameter("id",payment).executeUpdate();
        int e=em.createNativeQuery("UPDATE enrollments SET status='ACTIVE',enrolled_at=:now,seat_reserved_until=NULL,updated_at=:now WHERE id=:id AND status='PENDING'")
            .setParameter("now",now).setParameter("id",enrollment).executeUpdate();
        if(p!=1 || e!=1)throw new IllegalStateException("Locked reconciliation transition failed.");
    }
    public ReconciliationAttempt recordAttempt(long payment,long teacher,ReconciliationEvidence e,boolean late,
            Instant now,String failure,Long replay) {
        long id=number(em.createNativeQuery("""
            INSERT INTO payment_reconciliation_attempts(payment_id,teacher_id,bank_bin,account_number,
              bank_transaction_reference,received_at,observed_amount,currency,transfer_description,transfer_reference,
              attested,late,attempted_at,outcome,failure_reason,allocated,replay_of)
            VALUES (:p,:t,:b,:n,:r,:received,:amount,:currency,:description,:reference,:attested,:late,:now,
              :outcome,:failure,:allocated,:replay) RETURNING id
            """).setParameter("p",payment).setParameter("t",teacher).setParameter("b",e.bankBin()).setParameter("n",e.accountNumber())
            .setParameter("r",e.bankTransactionReference()).setParameter("received",e.receivedAt()).setParameter("amount",e.observedAmount())
            .setParameter("currency",e.currency()).setParameter("description",e.transferDescription()).setParameter("reference",e.transferReference())
            .setParameter("attested",e.attested()).setParameter("late",late).setParameter("now",now).setParameter("outcome",failure==null?"SUCCESS":"FAILED")
            .setParameter("failure",failure).setParameter("allocated",failure==null && replay==null).setParameter("replay",replay).getSingleResult());
        return new ReconciliationAttempt(new ApiId(id),new ApiId(payment),new ApiId(teacher),e,late,now,
            failure==null?"SUCCESS":"FAILED",failure,failure==null&&replay==null,replay==null?null:new ApiId(replay));
    }
    private static final String AUDIT="SELECT id,payment_id,teacher_id,bank_bin,account_number,bank_transaction_reference,received_at,observed_amount,currency,transfer_description,transfer_reference,attested,late,attempted_at,outcome,failure_reason,allocated,replay_of FROM payment_reconciliation_attempts";
    public List<ReconciliationAttempt> history(long payment,int page,int size) {
        return ((List<?>)em.createNativeQuery(AUDIT+" WHERE payment_id=:p ORDER BY id DESC").setParameter("p",payment)
            .setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size+1).getResultList()).stream().map(r->attempt((Object[])r)).toList();
    }
    private static ReconciliationAttempt attempt(Object[] r) {
        return new ReconciliationAttempt(new ApiId(number(r[0])),new ApiId(number(r[1])),new ApiId(number(r[2])),
            new ReconciliationEvidence((String)r[3],(String)r[4],(String)r[5],instant(r[6]),(BigDecimal)r[7],(String)r[8],(String)r[9],(String)r[10],(Boolean)r[11]),
            (Boolean)r[12],instant(r[13]),(String)r[14],(String)r[15],(Boolean)r[16],r[17]==null?null:new ApiId(number(r[17])));
    }
    private static long number(Object value) { return ((Number)value).longValue(); }
    private static Instant instant(Object value) { return value==null?null:value instanceof Instant i?i:value instanceof OffsetDateTime o?o.toInstant():((java.sql.Timestamp)value).toInstant(); }
}
