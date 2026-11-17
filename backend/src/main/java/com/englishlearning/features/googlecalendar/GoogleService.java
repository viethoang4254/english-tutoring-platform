package com.englishlearning.features.googlecalendar;

import java.util.*;
import org.springframework.stereotype.Service;
import com.englishlearning.features.user.service.CurrentAccountService;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.teacher.service.TeacherBusinessEligibility;

@Service
public class GoogleService {
    private final GoogleSettings settings;private final GoogleCrypto crypto;private final GoogleProvider provider;private final GoogleStore store;
    private final CurrentAccountService accounts;private final TeacherBusinessEligibility teachers;
    public GoogleService(GoogleSettings settings,GoogleCrypto crypto,GoogleProvider provider,GoogleStore store,CurrentAccountService accounts,TeacherBusinessEligibility teachers){
        this.settings=settings;this.crypto=crypto;this.provider=provider;this.store=store;this.accounts=accounts;this.teachers=teachers;
    }
    public void eligible(long user){
        var a=accounts.findById(user).orElseThrow(()->new GoogleFailure("DENIED"));
        if(a.locked()||!a.loginEligible()||!(a.role()==Role.STUDENT||a.role()==Role.TEACHER))throw new GoogleFailure("DENIED");
        if(a.role()==Role.TEACHER)try{teachers.requireEligible(user);}catch(com.englishlearning.features.teacher.service.TeacherReviewFailure e){throw new GoogleFailure("DENIED");}
    }
    public record Status(String status,String email,boolean reconnectRequired,java.time.Instant connectedAt,boolean synchronizationAvailable){}
    public Status status(long user){
        eligible(user);if(!settings.enabled)return new Status("UNAVAILABLE",null,false,null,false);
        var c=store.connection(user);if(c==null)return new Status("DISCONNECTED",null,false,null,false);
        boolean reconnect=c.status().equals("RECONNECT_REQUIRED")||c.status().equals("CONNECTED")&&!settings.keyId.equals(c.keyId());
        if(!reconnect && "CONNECTED".equals(c.status()) && store.cleaning(user,c.subject()))
            return new Status("CLEANUP_PENDING",c.email(),false,c.connectedAt(),false);
        return new Status(reconnect?"RECONNECT_REQUIRED":c.status(),c.email(),reconnect,c.connectedAt(),settings.syncEnabled && !reconnect && c.status().equals("CONNECTED"));
    }
    public record Started(String authorizationUrl,String browserBinding){@Override public String toString(){return "GoogleStarted[redacted]";}}
    public Started start(long user){
        eligible(user);available();
        String state=crypto.secret(),binding=crypto.secret(),verifier=crypto.secret(),nonce=crypto.secret();
        store.transaction(()->{store.lockEligible(user);var current=store.connection(user);
            if(current!=null&&current.status().equals("REVOCATION_PENDING"))throw new GoogleFailure("REVOCATION_PENDING");
            long version=store.advance(user);
            store.intent(GoogleCrypto.hash(state),user,GoogleCrypto.hash(binding),crypto.encrypt(verifier,"intent:"+GoogleCrypto.hash(state)),settings.keyId,GoogleCrypto.hash(nonce),version);return null;});
        return new Started(provider.authorize(state,verifier,nonce),binding);
    }
    public void complete(String state,String binding,String code,String error){
        available();if(state==null||binding==null||!state.matches("[A-Za-z0-9_-]{43}")||!binding.matches("[A-Za-z0-9_-]{43}"))throw new GoogleFailure("EXPIRED_OR_INVALID");
        // Commit consumption before any provider request; failures never make this intent replayable.
        var intent=store.transaction(()->store.consume(GoogleCrypto.hash(state),GoogleCrypto.hash(binding)));
        eligible(intent.user());
        if(error!=null)throw new GoogleFailure("CONSENT_DENIED");
        if(code==null||code.isBlank()||code.length()>4096)throw new GoogleFailure("INVALID_RESPONSE");
        String verifier=crypto.decrypt(intent.verifier(),intent.keyId(),"intent:"+intent.state());
        var grant=provider.exchange(code,verifier,intent.nonce());
        try{
            store.transaction(()->{
                store.lockEligible(intent.user());var old=store.connection(intent.user());
                if(old==null||old.version()!=intent.version())throw new GoogleFailure("EXPIRED_OR_INVALID");
                String refresh=grant.refreshToken();
                if(refresh==null){
                    if(!grant.subject().equals(old.subject())||!old.status().equals("CONNECTED"))throw new GoogleFailure("RECONNECT_REQUIRED");
                    refresh=crypto.decrypt(old.ciphertext(),old.keyId(),"refresh:"+intent.user()+":"+grant.subject());
                }
                if(old.subject()!=null&&!old.subject().equals(grant.subject()))throw new GoogleFailure("DISCONNECT_FIRST");
                if(!store.save(intent.user(),intent.version(),grant,crypto.encrypt(refresh,"refresh:"+intent.user()+":"+grant.subject()),settings.keyId))throw new GoogleFailure("EXPIRED_OR_INVALID");
                return null;
            });
        }catch(org.springframework.dao.DataIntegrityViolationException e){throw new GoogleFailure("CONNECTION_CONFLICT");}
    }
    public boolean disconnect(long user){
        eligible(user);
        record Disconnect(GoogleStore.Connection previous,boolean cleanupPending){}
        var decision=store.transaction(()->{
            store.lockEligible(user);
            var current=store.connection(user);
            if(current!=null && current.subject()!=null && !current.status().equals("REVOCATION_PENDING") && store.cleanupPending(user,current.subject()))
                return new Disconnect(current,true);
            if(current==null || !current.status().equals("REVOCATION_PENDING"))store.disconnect(user);
            return new Disconnect(current,false);
        });
        // Throw only after committing the durable cancellation request.
        if(decision.cleanupPending())throw new GoogleFailure("CLEANUP_PENDING");
        var old=decision.previous();
        if(old==null||old.subject()==null)return true;
        // Only the caller that committed the pending transition may contact Google.
        // An ambiguous outcome (including process death) keeps the subject quarantined.
        if(old.status().equals("REVOCATION_PENDING")||old.ciphertext()==null)return false;
        try {
            String token=crypto.decrypt(old.ciphertext(),old.keyId(),"refresh:"+user+":"+old.subject());
            if(!provider.revoke(token))return false;
        } catch(GoogleFailure failure){return false;}
        return store.transaction(()->{store.lock(user);return store.release(user,old.version()+1,old.subject());});
    }
    // Retain the eligible-account boundary for event creation and updates.
    public String accessToken(long user){
        return refreshForWorker(user,null,false);
    }
    String workerToken(CalendarSyncStore.Destination owner,boolean deletion) { return refreshForWorker(owner.user(),owner,deletion); }
    private String refreshForWorker(long user,CalendarSyncStore.Destination owner,boolean deletion){
        if(!deletion)eligible(user);
        available();
        var c=store.connection(user);if(c==null||!c.status().equals("CONNECTED"))throw new GoogleFailure("RECONNECT_REQUIRED");
        if(owner!=null && (owner.version()!=c.version() || !owner.subject().equals(c.subject())))throw new GoogleFailure("RECONNECT_REQUIRED");
        if(!deletion && store.cleaning(user,c.subject()))throw new GoogleFailure("CLEANUP_PENDING");
        String token;
        try{token=provider.refresh(crypto.decrypt(c.ciphertext(),c.keyId(),"refresh:"+user+":"+c.subject()));}
        catch(GoogleFailure e){if(e.code.equals("RECONNECT_REQUIRED"))store.transaction(()->{store.lock(user);store.reconnectRequired(user,c.version());return null;});throw e;}
        return store.transaction(()->{if(deletion)store.lock(user);else store.lockEligible(user);var latest=store.connection(user);
            if(latest==null||latest.version()!=c.version()||!latest.status().equals("CONNECTED"))throw new GoogleFailure("RECONNECT_REQUIRED");return token;});
    }
    private void available(){if(!settings.enabled)throw new GoogleFailure("UNAVAILABLE");}
}
