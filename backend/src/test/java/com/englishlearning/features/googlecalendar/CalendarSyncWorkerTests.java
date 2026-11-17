package com.englishlearning.features.googlecalendar;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class CalendarSyncWorkerTests {
    @TempDir java.nio.file.Path folder;
    final JsonMapper json=JsonMapper.builder().build();
    final CalendarSyncStore store=mock(CalendarSyncStore.class);
    final GoogleStore connections=mock(GoogleStore.class);
    final GoogleService oauth=mock(GoogleService.class);
    final CalendarTransport provider=mock(CalendarTransport.class);
    final CalendarSyncStore.Destination destination=new CalendarSyncStore.Destination(UUID.randomUUID(),3,"subject",4,"managed@example.invalid");
    final String desired="""
        {"course":3,"name":"Test","title":"https://meet.google.com/aaa-bbbb-ccc","role":"STUDENT","number":2,
         "start":1793511000,"end":1793514600,"timezone":"America/New_York"}
        """;
    final CalendarSyncStore.Job job=new CalendarSyncStore.Job(destination,7,"a12345",1,UUID.randomUUID(),desired,1);
    CalendarSyncWorker worker;
    CalendarTransport.Reply reply(int status,Object body){return new CalendarTransport.Reply(status,json.valueToTree(body));}
    @BeforeEach void setup()throws Exception {
        worker=new CalendarSyncWorker(store,provider,connections,oauth,GoogleTestSettings.create(folder),json);
        when(connections.connection(3)).thenReturn(new GoogleStore.Connection("subject","synthetic@example.invalid","scope","cipher","key","CONNECTED",4,Instant.EPOCH));
        when(oauth.workerToken(destination,false)).thenReturn("synthetic");
        when(store.current(job)).thenReturn(true);
        when(provider.calendar("synthetic",destination.calendar())).thenReturn(reply(200,Map.of("id",destination.calendar(),"description","ELP managed calendar "+destination.id())));
        when(provider.event("synthetic",destination.calendar(),job.event())).thenReturn(reply(404,Map.of()));
    }
    @Test void uncertainCalendarCreationIsQuarantined()throws Exception {
        when(store.provision()).thenReturn(destination);
        when(provider.createCalendar(anyString(),anyString())).thenThrow(new IOException());
        worker.tick();
        verify(store).provisioned(destination,null,"UNCERTAIN");
        verify(provider,times(1)).createCalendar(anyString(),anyString());
    }
    @Test void eventTimeoutRecoversSameIdAndUsesConditionalUpdate()throws Exception {
        when(store.claim()).thenReturn(job,null,job,null);
        when(provider.event(anyString(),anyString(),anyString())).thenReturn(reply(404,Map.of()),
            reply(200,Map.of("id",job.event(),"etag","version-1","extendedProperties",Map.of("private",Map.of("elpDestination",destination.id().toString())))));
        when(provider.insert(anyString(),anyString(),anyMap())).thenThrow(new IOException());
        when(provider.replace(anyString(),anyString(),anyString(),anyMap(),eq("version-1"))).thenReturn(reply(200,Map.of()));
        worker.tick();worker.tick();
        verify(provider,times(1)).insert(eq("synthetic"),eq(destination.calendar()),eq(worker.event(job)));
        verify(store).finish(job,null,false,0);
        var event=worker.event(job);
        assertFalse(event.toString().contains("meet.google.com"));
        assertEquals(job.event(),event.get("id"));
        assertEquals(Map.of("dateTime",Instant.ofEpochSecond(1793511000).toString(),"timeZone","America/New_York"),event.get("start"));
    }
    @Test void lostInvitationResponseRecoversPrivateEventWithoutNotifyingAgain()throws Exception{
        worker=new CalendarSyncWorker(store,provider,connections,oauth,GoogleTestSettings.create(folder,true),json);
        var teacher=(tools.jackson.databind.node.ObjectNode)json.readTree(desired);
        teacher.put("role","TEACHER");
        teacher.putArray("attendees").add("student@example.invalid").add("STUDENT@example.invalid");
        var invitation=new CalendarSyncStore.Job(destination,7,job.event(),2,UUID.randomUUID(),teacher.toString(),1);
        var payload=worker.event(invitation);
        assertEquals(java.util.List.of(Map.of("email","student@example.invalid")),payload.get("attendees"));
        assertEquals("private",payload.get("visibility"));
        for(String field:java.util.List.of("guestsCanModify","guestsCanInviteOthers","guestsCanSeeOtherGuests"))assertEquals(false,payload.get(field));
        assertTrue(payload.get("description").toString().contains("/student/dashboard/schedule?day="));
        assertFalse(payload.toString().contains("meet.google.com"));
        when(store.current(invitation)).thenReturn(true);
        when(store.markInvitationInsert(invitation)).thenReturn(true);
        when(store.claim()).thenReturn(invitation,null,invitation,null);
        when(provider.event("synthetic",destination.calendar(),job.event())).thenReturn(reply(404,Map.of()),reply(200,payload));
        when(provider.insert("synthetic",destination.calendar(),payload)).thenThrow(new IOException());
        worker.tick();worker.tick();
        verify(provider,times(1)).insert("synthetic",destination.calendar(),payload);
        verify(provider,never()).replace(anyString(),anyString(),anyString(),anyMap(),anyString());
        verify(store).finish(invitation,null,false,0);
    }

    @Test void changingGuestsPreservesRetainedResponseAndRemovesIneligibleGuest()throws Exception{
        worker=new CalendarSyncWorker(store,provider,connections,oauth,GoogleTestSettings.create(folder,true),json);
        var data=(tools.jackson.databind.node.ObjectNode)json.readTree(desired);
        data.put("role","TEACHER");
        data.putArray("attendees").add("retained@example.invalid").add("new@example.invalid");
        var invitation=new CalendarSyncStore.Job(destination,7,job.event(),3,UUID.randomUUID(),data.toString(),1);
        var existing=Map.of("email","retained@example.invalid","responseStatus","accepted");
        when(store.claim()).thenReturn(invitation).thenReturn(null);
        when(store.current(invitation)).thenReturn(true);
        when(provider.event("synthetic",destination.calendar(),job.event())).thenReturn(reply(200,Map.of(
            "id",job.event(),"etag","v2","extendedProperties",Map.of("private",Map.of("elpDestination",destination.id().toString())),
            "attendees",java.util.List.of(existing,Map.of("email","removed@example.invalid")))));
        when(provider.replace(eq("synthetic"),eq(destination.calendar()),eq(job.event()),anyMap(),eq("v2")))
            .thenAnswer(call->{
                Map<String,Object> patch=call.getArgument(3);
                assertEquals(json.valueToTree(java.util.List.of(Map.of("email","new@example.invalid"),existing)),
                    json.valueToTree(patch.get("attendees")));
                return reply(200,Map.of());
            });
        worker.tick();
        verify(store).finish(invitation,null,false,0);
    }

    @Test void rescheduleNotifiesWithoutReplacingGuestResponses()throws Exception{
        worker=new CalendarSyncWorker(store,provider,connections,oauth,GoogleTestSettings.create(folder,true),json);
        var data=(tools.jackson.databind.node.ObjectNode)json.readTree(desired);
        data.put("role","TEACHER");data.putArray("attendees").add("paid@example.invalid");
        var changed=new CalendarSyncStore.Job(destination,7,job.event(),2,UUID.randomUUID(),data.toString(),1);
        var remote=new java.util.HashMap<>(worker.event(changed));
        remote.put("start",Map.of("dateTime","2025-01-01T00:00:00Z","timeZone","America/New_York"));
        remote.put("etag","before");
        when(store.claim()).thenReturn(changed).thenReturn(null);
        when(store.current(changed)).thenReturn(true);
        when(provider.event("synthetic",destination.calendar(),job.event())).thenReturn(reply(200,remote));
        when(provider.patch(eq("synthetic"),eq(destination.calendar()),eq(job.event()),anyMap(),eq("before"),eq(true)))
            .thenAnswer(call->{Map<String,Object> patch=call.getArgument(3);assertFalse(patch.containsKey("attendees"));return reply(200,Map.of());});
        worker.tick();
        verify(store).finish(changed,null,false,0);
    }

    @Test void cancellationRequiresEvidenceAfterAnAmbiguousInsert()throws Exception{
        worker=new CalendarSyncWorker(store,provider,connections,oauth,GoogleTestSettings.create(folder,true),json);
        var removal=new CalendarSyncStore.Job(destination,7,job.event(),2,UUID.randomUUID(),null,1);
        when(store.claim()).thenReturn(removal,null,removal,null);
        when(store.current(removal)).thenReturn(true);
        when(store.uncertainInsert(removal)).thenReturn(true);
        when(oauth.workerToken(destination,true)).thenReturn("synthetic");
        when(provider.event("synthetic",destination.calendar(),job.event())).thenReturn(reply(404,Map.of()),
            reply(200,Map.of("id",job.event(),"etag","current","extendedProperties",Map.of("private",
                Map.of("elpDestination",destination.id().toString(),"elpInvitations","1")))));
        when(provider.delete("synthetic",destination.calendar(),job.event(),"current",true)).thenReturn(reply(204,Map.of()));
        worker.tick();
        verify(store).finish(eq(removal),eq("INSERT_OUTCOME_UNKNOWN"),eq(true),anyLong());
        verify(store,never()).finish(removal,null,false,0);
        worker.tick();verify(store).finish(removal,null,false,0);
    }

    @Test void staleRevisionCannotWrite()throws Exception {
        when(store.claim()).thenReturn(job).thenReturn(null);
        when(store.current(job)).thenReturn(true,false);
        worker.tick();
        verify(provider,never()).insert(anyString(),anyString(),anyMap());
    }
    @Test void disconnectedConnectionCannotWrite() {
        when(store.claim()).thenReturn(job).thenReturn(null);
        when(connections.connection(3)).thenReturn(null);
        worker.tick();
        verifyNoInteractions(provider);
    }
    @Test void creationPersistsAssignedCalendarBeforeAnyEventWrite()throws Exception{
        when(store.provision()).thenReturn(destination);
        when(provider.createCalendar("synthetic","ELP managed calendar "+destination.id())).thenReturn(reply(200,Map.of("id","secondary@example.invalid")));
        worker.tick();
        verify(store).provisioned(destination,"secondary@example.invalid","READY");
        verify(provider,never()).insert(anyString(),anyString(),anyMap());
    }
    @Test void cancelledSessionDeletesOnlyOwnedEventWithExactVersion()throws Exception{
        var deletion=new CalendarSyncStore.Job(destination,job.session(),job.event(),2,UUID.randomUUID(),null,1);
        when(store.claim()).thenReturn(deletion).thenReturn(null);
        when(store.current(deletion)).thenReturn(true);
        when(oauth.workerToken(destination,true)).thenReturn("synthetic");
        when(provider.event("synthetic",destination.calendar(),job.event())).thenReturn(reply(200,Map.of(
            "id",job.event(),"etag","version-2","extendedProperties",Map.of("private",Map.of("elpDestination",destination.id().toString())))));
        when(provider.delete("synthetic",destination.calendar(),job.event(),"version-2")).thenReturn(reply(204,Map.of()));
        worker.tick();verify(store).finish(deletion,null,false,0);verify(oauth,never()).workerToken(destination,false);
    }
    @Test void missingCalendarStopsWritesAndRequiresRecovery()throws Exception{
        when(store.claim()).thenReturn(job).thenReturn(null);
        when(provider.calendar(anyString(),anyString())).thenReturn(reply(404,Map.of()));
        worker.tick();verify(store).missingCalendar(job);
        verify(provider,never()).insert(anyString(),anyString(),anyMap());
    }
    @Test void providerFailureDoesNotDiscardOtherWork()throws Exception{
        var next=new CalendarSyncStore.Job(destination,8,"b12345",1,UUID.randomUUID(),desired,1);
        when(store.claim()).thenReturn(job,next,null);when(store.current(next)).thenReturn(true);
        when(provider.event("synthetic",destination.calendar(),next.event())).thenReturn(reply(404,Map.of()));
        when(provider.insert(eq("synthetic"),eq(destination.calendar()),eq(worker.event(job)))).thenReturn(reply(503,Map.of()));
        when(provider.insert(eq("synthetic"),eq(destination.calendar()),eq(worker.event(next)))).thenReturn(reply(200,Map.of("id",next.event())));
        worker.tick();verify(store).finish(eq(job),eq("PROVIDER_503"),eq(true),anyLong());verify(store).finish(next,null,false,0);
    }
    @Test void unauthorizedEventReadRefreshesAndRechecksTheJob()throws Exception{
        when(store.claim()).thenReturn(job).thenReturn(null);
        when(provider.event("synthetic",destination.calendar(),job.event())).thenReturn(reply(401,Map.of()),reply(404,Map.of()));
        when(provider.insert(anyString(),anyString(),anyMap())).thenReturn(reply(200,Map.of("id",job.event())));
        worker.tick();
        verify(oauth,times(2)).workerToken(destination,false);
        verify(store,atLeast(3)).current(job);
        verify(store).finish(job,null,false,0);
    }
    @Test void cancelledProviderTombstoneReplacesTheIdOnlyForWantedSessions()throws Exception{
        when(store.claim()).thenReturn(job).thenReturn(null);
        when(provider.event(anyString(),anyString(),anyString())).thenReturn(reply(410,Map.of()));
        worker.tick();verify(store).replaceRemovedEvent(job);
        verify(provider,never()).insert(anyString(),anyString(),anyMap());
    }
    @Test void googleAccountSwitchCannotReachThePreviousCalendar() {
        when(store.claim()).thenReturn(job).thenReturn(null);
        when(connections.connection(3)).thenReturn(new GoogleStore.Connection("other","other@example.invalid","scope","cipher","key","CONNECTED",5,Instant.EPOCH));
        worker.tick();verifyNoInteractions(provider);
    }
    @Test void rateLimitAndConflictResponsesKeepTheSameEventForBoundedRetry()throws Exception {
        when(store.claim()).thenReturn(job,null,job,null);
        when(provider.insert(anyString(),anyString(),anyMap())).thenReturn(reply(429,Map.of()),reply(409,Map.of()));
        worker.tick();worker.tick();
        verify(store).finish(eq(job),eq("PROVIDER_429"),eq(true),anyLong());
        verify(store).finish(eq(job),eq("PROVIDER_409"),eq(true),anyLong());
        verify(store,never()).replaceRemovedEvent(any());
    }
    @Test void scopesRevokedOnProviderDisableTheMatchingConnectionVersion()throws Exception{
        when(store.claim()).thenReturn(job).thenReturn(null);
        when(connections.transaction(any())).thenAnswer(call->((java.util.function.Supplier<?>)call.getArgument(0)).get());
        when(provider.event(anyString(),anyString(),anyString())).thenReturn(reply(403,Map.of("error",
            Map.of("errors",java.util.List.of(Map.of("reason","insufficientPermissions"))))));
        worker.tick();
        verify(connections).reconnectRequired(3,4);
        verify(store).finish(job,"PROVIDER_403",false,0);
        verify(provider,never()).insert(anyString(),anyString(),anyMap());
    }
    @Test void daylightSavingOverlapKeepsDistinctInstantsAndTheCourseZone(){
        var earlier=Instant.parse("2030-11-03T05:30:00Z");
        var later=Instant.parse("2030-11-03T06:30:00Z");
        var data=new java.util.HashMap<String,Object>();
        data.put("name","DST");data.put("course",3);data.put("number",1);data.put("role","STUDENT");
        data.put("timezone","America/New_York");data.put("start",earlier.getEpochSecond());data.put("end",later.getEpochSecond());
        var dst=new CalendarSyncStore.Job(destination,8,"b12345",1,UUID.randomUUID(),json.writeValueAsString(data),1);
        var event=worker.event(dst);
        assertEquals(Map.of("dateTime",earlier.toString(),"timeZone","America/New_York"),event.get("start"));
        assertEquals(Map.of("dateTime",later.toString(),"timeZone","America/New_York"),event.get("end"));
        assertEquals(earlier.atZone(java.time.ZoneId.of("America/New_York")).toLocalTime(),later.atZone(java.time.ZoneId.of("America/New_York")).toLocalTime());
    }
    @Test void ratesAndOutagesRetryButForbiddenDoesNot(){
        assertTrue(reply(429,Map.of()).transientFailure());
        assertTrue(reply(503,Map.of()).transientFailure());
        assertTrue(reply(403,Map.of("error",Map.of("errors",java.util.List.of(Map.of("reason","rateLimitExceeded"))))).transientFailure());
        assertFalse(reply(403,Map.of()).transientFailure());
    }
}
