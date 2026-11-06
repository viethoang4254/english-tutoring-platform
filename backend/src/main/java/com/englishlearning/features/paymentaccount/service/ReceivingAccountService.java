package com.englishlearning.features.paymentaccount.service;
import com.englishlearning.features.paymentaccount.dto.ReceivingAccount;
import com.englishlearning.features.paymentaccount.repository.ReceivingAccountRepository;
import com.englishlearning.features.paymentaccount.storage.PrivateQrStorage;
import com.englishlearning.features.teacher.service.*;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.entity.User.Role;
import java.util.*;
import java.time.*;
import java.nio.charset.StandardCharsets;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.englishlearning.features.paymentaccount.service.ReceivingFailure.Reason.*;
@Service
public class ReceivingAccountService {
    private final ReceivingAccountRepository rows;private final TeacherBusinessEligibility eligibility;
    private final CurrentAccountService accounts;
    private final ReceivingAccountTransactions transactions;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReceivingAccountService.class);
    private final BankDirectory directory;private final PrivateQrStorage storage;
    public ReceivingAccountService(ReceivingAccountRepository rows,TeacherBusinessEligibility eligibility,
        CurrentAccountService accounts,BankDirectory directory,PrivateQrStorage storage,ReceivingAccountTransactions transactions) {
        this.rows=rows;this.eligibility=eligibility;this.accounts=accounts;this.directory=directory;this.storage=storage;this.transactions=transactions;
    }
    public record Created(ReceivingAccount account,boolean created) {}
    public record Page(List<ReceivingAccount> items,int page,int size,boolean hasNext) {}
    private void teacher(long actor){try{eligibility.requireEligible(actor);}catch(TeacherReviewFailure e){throw fail(DENIED);}}
    @Transactional(propagation = Propagation.NEVER)
    public Created submit(long actor,String bin,String holder,String number,String key,byte[] input) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) throw fail(UNAVAILABLE);
        teacher(actor);var bank=directory.require(bin);
        holder=text(holder,150);number=text(number,34);
        if(!number.matches("[0-9]{1,34}") || key==null || !key.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))throw fail(INVALID);
        UUID request=UUID.fromString(key);byte[] png=QrImage.sanitize(input);
        String fingerprint=QrImage.hash((bin+"\n"+holder+"\n"+number+"\n"+QrImage.hash(input)).getBytes(StandardCharsets.UTF_8));
        var submission = new ReceivingAccountTransactions.Submission(actor,bank,holder,number,request,fingerprint,QrImage.hash(png),png.length);
        var existing = transactions.preflight(submission);
        if (existing != null) return existing;
        String object = UUID.randomUUID()+".png";
        try { storage.put(object,png); }
        catch (RuntimeException failure) {
            log.warn("receiving_upload_outcome_uncertain");
            throw failure;
        }
        var upload = new ReceivingAccountTransactions.Upload(object);
        try { return transactions.persist(submission,upload); }
        finally {
            if (upload.uncertain()) log.warn("receiving_persistence_outcome_uncertain");
            if (upload.reclaimable()) {
                try { storage.delete(object); }
                catch (RuntimeException failure) { log.warn("receiving_orphan_cleanup_failed"); }
            }
        }
    }

    @Transactional(readOnly=true)
    public Page list(long actor,int page,int size) {
        teacher(actor);if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE)throw fail(INVALID);
        var records=rows.list(actor,page,size);return new Page(records.stream().limit(size).map(r->mask(r.account())).toList(),page,size,records.size()>size);
    }
    @Transactional(readOnly=true)
    public ReceivingAccount detail(long actor,long id){teacher(actor);return own(actor,id).account();}
    @Transactional(readOnly=true)
    public byte[] image(long actor,long id,boolean admin) {
        if(admin) {
            var user=accounts.findById(actor).orElseThrow(()->fail(DENIED));
            if(user.role()!=Role.ADMIN||user.locked()||!user.emailVerified())throw fail(DENIED);
        } else teacher(actor);
        var row=admin?rows.get(id).orElseThrow(()->fail(MISSING)):own(actor,id);
        if(row.object()==null)throw fail(MISSING);
        byte[] png=storage.read(row.object());if(!QrImage.hash(png).equals(row.imageHash()))throw fail(UNAVAILABLE);
        return png;
    }
    private ReceivingAccountRepository.Stored own(long actor,long id){var r=rows.get(id).orElseThrow(()->fail(MISSING));if(r.teacher()!=actor)throw fail(MISSING);return r;}
    private static ReceivingAccount mask(ReceivingAccount r) {
        String n=r.accountNumber();String masked=n.length()<=4?"****":"****"+n.substring(n.length()-4);
        return new ReceivingAccount(r.id(),r.bankBin(),r.bankCode(),r.bankName(),r.holderName(),masked,r.reviewStatus(),r.selectionStatus(),r.hasQrImage(),r.createdAt());
    }
    private static String text(String s,int max){if(s==null||s.isBlank()||s.length()>max||s.codePoints().anyMatch(Character::isISOControl))throw fail(INVALID);return s.strip();}
    private static ReceivingFailure fail(ReceivingFailure.Reason r){return new ReceivingFailure(r);}
}
