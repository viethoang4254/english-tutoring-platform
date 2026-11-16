package com.englishlearning.features.googlecalendar;

import java.util.Map;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@EnableScheduling
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="APP_GOOGLE_CALENDAR_SYNC_ENABLED",havingValue="true")
public class CalendarSyncWorker {
    private final CalendarSyncStore store;
    private final CalendarTransport provider;
    private final GoogleStore connections;
    private final GoogleService oauth;
    private final GoogleSettings settings;
    private final JsonMapper json;
    public CalendarSyncWorker(CalendarSyncStore store,CalendarTransport provider,GoogleStore connections,
            GoogleService oauth,GoogleSettings settings,JsonMapper json) {
        this.store=store;this.provider=provider;this.connections=connections;this.oauth=oauth;this.settings=settings;this.json=json;
    }
    @Scheduled(fixedDelayString="${app.google-calendar.worker-delay-ms:3000}")
    public void tick() {
        if(!settings.enabled)return;
        store.project();
        var destination=store.provision();
        if(destination!=null)provision(destination);
        for(int i=0;i<12;i++){
            var job=store.claim();
            if(job==null)break;
            apply(job);
        }
    }

    private String token(CalendarSyncStore.Destination d) {
        String token=oauth.workerToken(d,false);
        var now=connections.connection(d.user());
        if(now==null||now.version()!=d.version()||!d.subject().equals(now.subject())||!"CONNECTED".equals(now.status()))
            throw new GoogleFailure("RECONNECT_REQUIRED");
        return token;
    }

    private void provision(CalendarSyncStore.Destination d) {
        String token;
        try { token=token(d); }
        catch(GoogleFailure e){store.provisioned(d,null,"NEW");return;}
        try {
            var response=provider.createCalendar(token,"ELP managed calendar "+d.id());
            var id=response.body().path("id").asText("");
            if(response.success()&&!id.isBlank()&&!id.equals("primary"))store.provisioned(d,id,"READY");
            else if(response.status()==429||response.status()==401||response.status()==403&&response.transientFailure())
                store.provisioned(d,null,"NEW");
            else store.provisioned(d,null,response.status()>=500||response.success()?"UNCERTAIN":"FAILED");
        } catch(java.io.IOException e){store.provisioned(d,null,"UNCERTAIN");}
        catch(InterruptedException e){Thread.currentThread().interrupt();store.provisioned(d,null,"UNCERTAIN");}
    }

    private void apply(CalendarSyncStore.Job job) {
        try {
            var d=job.dest();
            String token=job.desired()==null?oauth.workerToken(d,true):token(d);
            if(!store.current(job)){retry(job,"STATE_CHANGED");return;}
            var calendar=provider.calendar(token,d.calendar());
            if(calendar.status()==404||calendar.status()==410){store.missingCalendar(job);return;}
            if(!calendar.success()){failure(job,calendar);return;}
            if(!d.calendar().equals(calendar.body().path("id").asText("")) ||
                    !("ELP managed calendar "+d.id()).equals(calendar.body().path("description").asText(""))){
                store.finish(job,"CALENDAR_OWNERSHIP",false,0);return;
            }
            var current=provider.event(token,d.calendar(),job.event());
            if(current.status()==401) {
                token=job.desired()==null?oauth.workerToken(d,true):token(d);
                if(!store.current(job)){retry(job,"STATE_CHANGED");return;}
                current=provider.event(token,d.calendar(),job.event());
            }
            boolean removed=current.status()==410||current.success()&&"cancelled".equals(current.body().path("status").asText(""));
            if(removed && job.desired()!=null){store.replaceRemovedEvent(job);return;}
            boolean absent=current.status()==404||removed;
            if(!absent&&!current.success()){failure(job,current);return;}
            if(!absent&&(!job.event().equals(current.body().path("id").asText(""))||
                    !d.id().toString().equals(current.body().path("extendedProperties").path("private").path("elpDestination").asText("")))){
                store.finish(job,"OWNERSHIP_CONFLICT",false,0);return;
            }
            if(!store.current(job)){retry(job,"STATE_CHANGED");return;}
            if(job.desired()==null&&absent){store.finish(job,null,false,0);return;}
            CalendarTransport.Reply result;
            String etag=current.body().path("etag").asText("");
            if(job.desired()==null)result=provider.delete(token,d.calendar(),job.event(),etag);
            else {
                var event=event(job);
                result=absent?provider.insert(token,d.calendar(),event):provider.replace(token,d.calendar(),job.event(),event,etag);
            }
            if(result.success()||job.desired()==null&&(result.status()==404||result.status()==410))store.finish(job,null,false,0);
            else failure(job,result);
        } catch(InterruptedException e){Thread.currentThread().interrupt();retry(job,"INTERRUPTED");}
        catch(java.io.IOException e){retry(job,"NETWORK");}
        catch(GoogleFailure e){retry(job,"AUTHORIZATION");}
        catch(tools.jackson.core.JacksonException e){store.finish(job,"INVALID_LOCAL_DATA",false,0);}
        catch(IllegalArgumentException e){store.finish(job,"INVALID_REMOTE_DATA",false,0);}
    }

    Map<String,Object> event(CalendarSyncStore.Job j) {
        JsonNode data=json.readTree(j.desired());
        String title=data.path("name").asText()+" — Buổi "+data.path("number").asInt();
        String topic=data.path("title").asText("");
        if(!topic.isBlank())title+=": "+topic;
        // Free-form course/session titles may themselves contain pasted meeting URLs.
        title=title.replaceAll("(?i)(?:https?://)?meet\\.google\\.com/\\S+","[Google Meet]");
        var day=Instant.ofEpochSecond(data.path("start").asLong()).atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
        String route="TEACHER".equals(data.path("role").asText())?"/teacher/courses/"+data.path("course").asLong():
            "/student/dashboard?day="+day+"#session-"+j.session();
        String link=settings.frontend.replaceAll("/$","")+route;
        return Map.of("id",j.event(),"summary",title,"description",link,
            "start",time(data,"start"),"end",time(data,"end"),
            "extendedProperties",Map.of("private",Map.of("elpDestination",j.dest().id().toString(),"elpRevision",Long.toString(j.revision()))));
    }
    private Map<String,String> time(JsonNode data,String field) {
        var epoch=data.path(field).decimalValue();
        var instant=Instant.ofEpochSecond(epoch.longValue(),epoch.remainder(java.math.BigDecimal.ONE).movePointRight(9).longValue());
        return Map.of("dateTime",instant.toString(),"timeZone",data.path("timezone").asText());
    }
    private void failure(CalendarSyncStore.Job j,CalendarTransport.Reply r) {
        String reason=r.body().path("error").path("errors").path(0).path("reason").asText("");
        if(r.status()==403 && ("insufficientPermissions".equals(reason) || "ACCESS_TOKEN_SCOPE_INSUFFICIENT".equals(
                r.body().path("error").path("details").path(0).path("reason").asText("")))) {
            connections.transaction(()->{
                connections.lock(j.dest().user());
                connections.reconnectRequired(j.dest().user(),j.dest().version());
                return null;
            });
        }
        if(r.transientFailure()||r.status()==401||r.status()==409||r.status()==412)retry(j,"PROVIDER_"+r.status());
        else store.finish(j,"PROVIDER_"+r.status(),false,0);
    }
    private void retry(CalendarSyncStore.Job j,String category) {
        long base=1L<<Math.min(8,j.attempts());
        store.finish(j,category,true,base+ThreadLocalRandom.current().nextLong(base));
    }
}
