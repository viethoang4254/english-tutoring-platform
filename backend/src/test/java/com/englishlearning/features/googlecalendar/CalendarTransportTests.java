package com.englishlearning.features.googlecalendar;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class CalendarTransportTests {
    @Test void onlyManagedCalendarAndEventPathsReceiveBearerAndConditionalWrites() throws Exception {
        var requests=new CopyOnWriteArrayList<List<String>>();
        var queries=new CopyOnWriteArrayList<String>();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/calendars",exchange->{
            var headers=exchange.getRequestHeaders();
            queries.add(String.valueOf(exchange.getRequestURI().getRawQuery()));
            requests.add(List.of(exchange.getRequestMethod(),exchange.getRequestURI().getRawPath(),
                String.valueOf(headers.getFirst("Authorization")),String.valueOf(headers.getFirst("If-Match"))));
            byte[] body="{\"id\":\"synthetic\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,body.length);
            try(var output=exchange.getResponseBody()){output.write(body);}
        });
        server.start();
        try {
            var client=new CalendarTransport(JsonMapper.builder().build(),URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/calendars"));
            String token="synthetic",calendar="test@example.invalid",event="01234abc";
            assertEquals("synthetic",client.createCalendar(token,"synthetic").body().path("id").asText());
            client.calendar(token,calendar);client.event(token,calendar,event);client.insert(token,calendar,Map.of("id",event));
            client.replace(token,calendar,event,Map.of("id",event),"v1");client.delete(token,calendar,event,"v2");
            assertEquals(List.of("POST","GET","GET","POST","PATCH","DELETE"),requests.stream().map(r->r.get(0)).toList());
            assertTrue(requests.stream().allMatch(r->r.get(2).equals("Bearer synthetic")));
            String path="/calendars/test%40example.invalid/events/"+event;
            assertEquals(List.of("PATCH",path,"Bearer synthetic","v1"),requests.get(4));
            assertEquals(List.of("DELETE",path,"Bearer synthetic","v2"),requests.get(5));
            assertThrows(IllegalArgumentException.class,()->client.delete(token,"primary",event,"v2"));
            assertThrows(IllegalArgumentException.class,()->client.delete(token,calendar,event,"*"));
            assertEquals(6,requests.size());
            assertTrue(queries.stream().allMatch("null"::equals));
        } finally {server.stop(0);}
    }
    @Test void invitationWritesNotifyGuestsIncludingRemovalAndCancellation() throws Exception {
        var requests = new CopyOnWriteArrayList<List<String>>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/calendars", exchange -> {
            requests.add(List.of(exchange.getRequestMethod(),
                String.valueOf(exchange.getRequestURI().getRawQuery()),
                String.valueOf(exchange.getRequestHeaders().getFirst("If-Match")),
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        try {
            var client = new CalendarTransport(JsonMapper.builder().build(),
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/calendars"));
            String calendar = "managed@example.invalid", event = "01234abc";
            var attendees = List.of(Map.of("email", "student@example.invalid"));
            client.insert("synthetic", calendar, Map.of("id", event, "attendees", attendees));
            client.replace("synthetic", calendar, event, Map.of("attendees", attendees), "v1");
            client.replace("synthetic", calendar, event, Map.of("attendees", List.of()), "v2");
            client.delete("synthetic", calendar, event, "v3", true);
            assertEquals(List.of("POST", "PATCH", "PATCH", "DELETE"), requests.stream().map(r -> r.get(0)).toList());
            assertTrue(requests.stream().allMatch(r -> r.get(1).equals("sendUpdates=all")));
            assertEquals(List.of("null", "v1", "v2", "v3"), requests.stream().map(r -> r.get(2)).toList());
            var json = JsonMapper.builder().build();
            assertEquals("student@example.invalid", json.readTree(requests.get(0).get(3)).path("attendees").path(0).path("email").asText());
            assertEquals(0, json.readTree(requests.get(2).get(3)).path("attendees").size());
            assertThrows(IllegalArgumentException.class, () -> client.delete("synthetic", "primary", event, "v3", true));
            assertThrows(IllegalArgumentException.class, () -> client.delete("synthetic", calendar, event, "*", true));
            assertEquals(4, requests.size());
        } finally { server.stop(0); }
    }

}
