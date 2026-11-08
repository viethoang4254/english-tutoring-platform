package com.englishlearning.features.paymentaccount.repository;
import com.englishlearning.features.paymentaccount.dto.ReceivingAccount;
import com.englishlearning.common.api.ApiId;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.time.Instant;
import org.springframework.stereotype.Repository;
@Repository
public class ReceivingAccountRepository {
    private final EntityManager em;
    public ReceivingAccountRepository(EntityManager em){this.em=em;}
    public record Stored(long teacher,ReceivingAccount account,String object,String imageHash,String requestHash) {}
    private static final String SELECT="SELECT id,teacher_id,bank_bin,bank_code,bank_name,account_holder_name,account_number,review_status,status,qr_object,qr_sha256,request_sha256,created_at FROM public.teacher_payment_accounts";
    private Stored row(Object value) {
        Object[] r=(Object[])value;var time=r[12] instanceof Instant i?i:r[12] instanceof java.time.OffsetDateTime o?o.toInstant():((java.sql.Timestamp)r[12]).toInstant();
        return new Stored(((Number)r[1]).longValue(),new ReceivingAccount(new ApiId(((Number)r[0]).longValue()),(String)r[2],(String)r[3],(String)r[4],(String)r[5],(String)r[6],(String)r[7],(String)r[8],r[9]!=null,time),(String)r[9],(String)r[10],(String)r[11]);
    }
    public Optional<Stored> get(long id){return ((List<?>)em.createNativeQuery(SELECT+" WHERE id=:id").setParameter("id",id).getResultList()).stream().map(this::row).findFirst();}
    public Optional<Stored> byKey(long actor,UUID key){return ((List<?>)em.createNativeQuery(SELECT+" WHERE teacher_id=:actor AND submission_key=:key").setParameter("actor",actor).setParameter("key",key).getResultList()).stream().map(this::row).findFirst();}
    public boolean pending(long actor){return (Boolean)em.createNativeQuery("SELECT EXISTS(SELECT 1 FROM public.teacher_payment_accounts WHERE teacher_id=:actor AND review_status='PENDING')").setParameter("actor",actor).getSingleResult();}
    public List<Stored> list(long actor,int page,int size){return ((List<?>)em.createNativeQuery(SELECT+" WHERE teacher_id=:actor ORDER BY created_at DESC,id DESC").setParameter("actor",actor).setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size+1).getResultList()).stream().map(this::row).toList();}
    public long insert(long actor,String bin,String code,String bankName,String holder,String number,String object,String imageHash,int bytes,UUID key,String fingerprint,Instant now) {
        return ((Number)em.createNativeQuery("INSERT INTO public.teacher_payment_accounts(teacher_id,bank_bin,bank_code,bank_name,account_holder_name,account_number,provider,status,review_status,qr_object,qr_sha256,qr_size,submission_key,request_sha256,created_at,updated_at) VALUES (:actor,:bin,:code,:name,:holder,:number,'VIETQR','INACTIVE','PENDING',:object,:hash,:bytes,:key,:fingerprint,:now,:now) RETURNING id")
            .setParameter("actor",actor).setParameter("bin",bin).setParameter("code",code).setParameter("name",bankName).setParameter("holder",holder).setParameter("number",number).setParameter("object",object).setParameter("hash",imageHash).setParameter("bytes",bytes).setParameter("key",key).setParameter("fingerprint",fingerprint).setParameter("now",now).getSingleResult()).longValue();
    }
    public Optional<Stored> lock(long id) {
        return ((List<?>)em.createNativeQuery(SELECT+" WHERE id=:id FOR UPDATE").setParameter("id",id).getResultList()).stream().map(this::row).findFirst();
    }
    public List<Stored> pendingPage(int page,int size) {
        return ((List<?>)em.createNativeQuery(SELECT+" WHERE review_status='PENDING' ORDER BY created_at,id")
            .setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size+1).getResultList()).stream().map(this::row).toList();
    }
    public record Audit(ApiId reviewer,Instant at,String reason) {}
    public Audit audit(long id) {
        Object[] r=(Object[])em.createNativeQuery("SELECT reviewed_by,reviewed_at,rejection_reason FROM public.teacher_payment_accounts WHERE id=:id").setParameter("id",id).getSingleResult();
        Instant at=r[1]==null?null:r[1] instanceof Instant t?t:r[1] instanceof java.time.OffsetDateTime o?o.toInstant():((java.sql.Timestamp)r[1]).toInstant();
        return new Audit(r[0]==null?null:new ApiId(((Number)r[0]).longValue()),at,(String)r[2]);
    }
    public void deactivateSelected(long teacher,Instant now) {
        em.createNativeQuery("UPDATE public.teacher_payment_accounts SET status='INACTIVE',updated_at=:now WHERE teacher_id=:teacher AND status='ACTIVE'")
            .setParameter("teacher",teacher).setParameter("now",now).executeUpdate();
    }
    public int decide(long id,long reviewer,boolean approve,String reason,Instant now) {
        return em.createNativeQuery("UPDATE public.teacher_payment_accounts SET review_status=:review,status=:selection,reviewed_by=:reviewer,reviewed_at=:now,rejection_reason=:reason,updated_at=:now WHERE id=:id AND review_status='PENDING'")
            .setParameter("review",approve?"APPROVED":"REJECTED").setParameter("selection",approve?"ACTIVE":"INACTIVE")
            .setParameter("reviewer",reviewer).setParameter("now",now).setParameter("reason",reason).setParameter("id",id).executeUpdate();
    }
}
