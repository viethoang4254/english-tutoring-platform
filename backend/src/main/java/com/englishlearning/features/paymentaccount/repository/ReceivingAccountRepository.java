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
}
