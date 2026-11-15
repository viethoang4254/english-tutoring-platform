package com.englishlearning.features.googlecalendar;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class GoogleStore {
    private final ObjectProvider<JdbcTemplate> jdbc;
    private final ObjectProvider<PlatformTransactionManager> manager;
    public GoogleStore(ObjectProvider<JdbcTemplate> jdbc,ObjectProvider<PlatformTransactionManager> manager){this.jdbc=jdbc;this.manager=manager;}
    public <T>T transaction(Supplier<T> work){return new TransactionTemplate(manager.getObject()).execute(tx->work.get());}
    public void lock(long user){jdbc.getObject().queryForObject("SELECT id FROM public.users WHERE id=? FOR UPDATE",Long.class,user);}
    // Called inside the mutation transaction. Both reads bypass the JPA persistence context.
    public void lockEligible(long user){
        var db=jdbc.getObject();
        var rows=db.query("SELECT id,role,locked,email_verified,student_login_without_verification FROM public.users WHERE id=? FOR UPDATE",
            (rs,n)->new com.englishlearning.features.user.service.AccountSnapshot(rs.getLong("id"),
                com.englishlearning.features.user.entity.User.Role.valueOf(rs.getString("role")),rs.getBoolean("locked"),
                rs.getBoolean("email_verified"),rs.getBoolean("student_login_without_verification")),user);
        if(rows.isEmpty())throw new GoogleFailure("DENIED");
        var account=rows.get(0);
        if(account.locked())throw new GoogleFailure("DENIED");
        switch(account.role()){
            case STUDENT -> {if(!account.loginEligible())throw new GoogleFailure("DENIED");}
            case TEACHER -> {
                var approvals=db.queryForList("SELECT id FROM public.teacher_applications WHERE teacher_id=? AND status='APPROVED' ORDER BY id LIMIT 1 FOR SHARE",Long.class,user);
                if(approvals.isEmpty())throw new GoogleFailure("DENIED");
            }
            default -> throw new GoogleFailure("DENIED");
        }
    }
    public record Connection(String subject,String email,String scopes,String ciphertext,String keyId,String status,long version,Instant connectedAt) {
        @Override public String toString(){return "GoogleConnection[redacted]";}
    }
    public Connection connection(long user){
        var rows=jdbc.getObject().query("SELECT * FROM public.google_calendar_connections WHERE user_id=?",(r,n)->new Connection(r.getString("google_subject"),r.getString("google_email"),r.getString("granted_scopes"),r.getString("refresh_ciphertext"),r.getString("key_id"),r.getString("status"),r.getLong("version"),r.getTimestamp("connected_at")==null?null:r.getTimestamp("connected_at").toInstant()),user);
        return rows.isEmpty()?null:rows.get(0);
    }
    public long advance(long user){
        jdbc.getObject().update("INSERT INTO public.google_calendar_connections(user_id) VALUES (?) ON CONFLICT(user_id) DO NOTHING",user);
        return jdbc.getObject().queryForObject("UPDATE public.google_calendar_connections SET version=version+1,updated_at=now() WHERE user_id=? RETURNING version",Long.class,user);
    }
    public void intent(String state,long user,String binding,String verifier,String keyId,String nonce,long version){
        jdbc.getObject().update("DELETE FROM public.google_calendar_oauth_intents WHERE user_id=? OR expires_at<now()",user);
        jdbc.getObject().update("INSERT INTO public.google_calendar_oauth_intents(state_hash,user_id,browser_hash,verifier_ciphertext,key_id,nonce_hash,connection_version,expires_at) VALUES (?,?,?,?,?,?,?,now()+interval '10 minutes')",state,user,binding,verifier,keyId,nonce,version);
    }
    public record Intent(String state,long user,String verifier,String keyId,String nonce,long version){
        @Override public String toString(){return "GoogleIntent[redacted]";}
    }
    public Intent consume(String state,String binding){
        var rows=jdbc.getObject().query("SELECT * FROM public.google_calendar_oauth_intents WHERE state_hash=? AND browser_hash=? AND consumed_at IS NULL AND expires_at>now() FOR UPDATE",(r,n)->new Intent(state,r.getLong("user_id"),r.getString("verifier_ciphertext"),r.getString("key_id"),r.getString("nonce_hash"),r.getLong("connection_version")),state,binding);
        if(rows.isEmpty())throw new GoogleFailure("EXPIRED_OR_INVALID");
        jdbc.getObject().update("UPDATE public.google_calendar_oauth_intents SET consumed_at=now(),verifier_ciphertext=NULL WHERE state_hash=?",state);
        return rows.get(0);
    }
    public boolean save(long user,long version,GoogleProvider.Grant grant,String encrypted,String keyId){
        var db = jdbc.getObject();
        db.update("INSERT INTO google_calendar_subject_releases(google_subject) VALUES (?) ON CONFLICT DO NOTHING",grant.subject());
        long released = db.queryForObject("SELECT release_order FROM google_calendar_subject_releases WHERE google_subject=? FOR UPDATE",Long.class,grant.subject());
        return db.update("UPDATE public.google_calendar_connections SET google_subject=?,google_email=?,granted_scopes=?,refresh_ciphertext=?,key_id=?,status='CONNECTED',connected_at=now(),updated_at=now(),version=version+1 WHERE user_id=? AND version=? AND status<>'REVOCATION_PENDING' AND EXISTS (SELECT 1 FROM google_calendar_oauth_intents i WHERE i.user_id=? AND i.connection_version=? AND i.consent_order>?)",grant.subject(),grant.email(),grant.scopes(),encrypted,keyId,user,version,user,version,released)==1;
    }
    public void reconnectRequired(long user,long version){
        jdbc.getObject().update("UPDATE public.google_calendar_connections SET status='RECONNECT_REQUIRED',refresh_ciphertext=NULL,key_id=NULL,version=version+1,updated_at=now() WHERE user_id=? AND version=?",user,version);
    }
    public void disconnect(long user){
        advance(user);
        jdbc.getObject().update("UPDATE public.google_calendar_connections SET status=CASE WHEN google_subject IS NULL THEN 'DISCONNECTED' ELSE 'REVOCATION_PENDING' END,refresh_ciphertext=NULL,key_id=NULL,google_email=NULL,granted_scopes=NULL,connected_at=NULL WHERE user_id=?",user);
        jdbc.getObject().update("DELETE FROM public.google_calendar_oauth_intents WHERE user_id=?",user);
    }
    public boolean release(long user,long version,String subject){
        var db=jdbc.getObject();
        db.queryForObject("SELECT release_order FROM google_calendar_subject_releases WHERE google_subject=? FOR UPDATE",Long.class,subject);
        if(db.update("UPDATE google_calendar_connections SET status='DISCONNECTED',google_subject=NULL,version=version+1,updated_at=now() WHERE user_id=? AND version=? AND google_subject=? AND status='REVOCATION_PENDING'",user,version,subject)!=1)return false;
        db.update("UPDATE google_calendar_subject_releases SET release_order=nextval('public.google_calendar_consent_order') WHERE google_subject=?",subject);
        return true;
    }
}
