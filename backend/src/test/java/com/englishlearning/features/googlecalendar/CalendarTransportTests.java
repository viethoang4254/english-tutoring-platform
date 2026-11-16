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
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/calendars",exchange->{
            var headers=exchange.getRequestHeaders();
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
            assertEquals(List.of("POST","GET","GET","POST","PUT","DELETE"),requests.stream().map(r->r.get(0)).toList());
            assertTrue(requests.stream().allMatch(r->r.get(2).equals("Bearer synthetic")));
            String path="/calendars/test%40example.invalid/events/"+event;
            assertEquals(List.of("PUT",path,"Bearer synthetic","v1"),requests.get(4));
            assertEquals(List.of("DELETE",path,"Bearer synthetic","v2"),requests.get(5));
            assertThrows(IllegalArgumentException.class,()->client.delete(token,"primary",event,"v2"));
            assertThrows(IllegalArgumentException.class,()->client.delete(token,calendar,event,"*"));
            assertEquals(6,requests.size());
        } finally {server.stop(0);}
    }
}
