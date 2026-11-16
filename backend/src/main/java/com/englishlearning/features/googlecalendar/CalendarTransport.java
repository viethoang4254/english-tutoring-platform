package com.englishlearning.features.googlecalendar;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
final class CalendarTransport {
    private final JsonMapper json;
    private final URI root;
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(Duration.ofSeconds(5)).build();

    @org.springframework.beans.factory.annotation.Autowired
    CalendarTransport(JsonMapper json) {
        this(json, URI.create("https://www.googleapis.com/calendar/v3/calendars"));
    }

    CalendarTransport(JsonMapper json, URI root) {
        this.json = json;
        this.root = root;
    }

    record Reply(int status, JsonNode body) {
        @Override public String toString() { return "CalendarResponse[" + status + "]"; }
        boolean success() { return status >= 200 && status < 300; }
        boolean transientFailure() {
            String reason = body.path("error").path("errors").path(0).path("reason").asText("");
            return status == 408 || status == 429 || status >= 500 ||
                (status == 403 && ("userRateLimitExceeded".equals(reason) || "rateLimitExceeded".equals(reason)));
        }
    }

    Reply createCalendar(String token, String marker) throws IOException, InterruptedException {
        return request(token, "POST", "", Map.of("summary", "English Learning Platform — Lịch học", "description", marker), null);
    }

    Reply calendar(String token, String calendar) throws IOException, InterruptedException {
        return request(token, "GET", calendarPath(calendar), null, null);
    }

    Reply event(String token, String calendar, String event) throws IOException, InterruptedException {
        return request(token, "GET", eventPath(calendar, event), null, null);
    }

    Reply insert(String token, String calendar, Map<String,Object> event) throws IOException, InterruptedException {
        return request(token, "POST", calendarPath(calendar) + "/events", event, null);
    }

    Reply replace(String token, String calendar, String event, Map<String,Object> data, String etag) throws IOException, InterruptedException {
        return request(token, "PUT", eventPath(calendar, event), data, requireTag(etag));
    }

    Reply delete(String token, String calendar, String event, String etag) throws IOException, InterruptedException {
        return request(token, "DELETE", eventPath(calendar, event), null, requireTag(etag));
    }

    private static String requireTag(String tag) {
        if (tag == null || tag.isBlank() || tag.equals("*")) throw new IllegalArgumentException("Missing event version");
        return tag;
    }

    private static String calendarPath(String calendar) {
        if (calendar == null || calendar.isBlank() || calendar.equals("primary") || calendar.equals(".") || calendar.equals(".."))
            throw new IllegalArgumentException("Missing managed calendar");
        return "/" + GoogleProvider.encode(calendar);
    }

    private static String eventPath(String calendar, String event) {
        if (!event.matches("[0-9a-v]{5,1024}")) throw new IllegalArgumentException("Invalid managed event ID");
        return calendarPath(calendar) + "/events/" + event;
    }

    private Reply request(String token, String method, String suffix, Object data, String tag) throws IOException, InterruptedException {
        var req = HttpRequest.newBuilder(URI.create(root.toString() + suffix)).timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer " + token).header("Accept", "application/json");
        if (tag != null) req.header("If-Match", tag);
        if (data != null) req.header("Content-Type", "application/json");
        var response = client.send(req.method(method, data == null ? HttpRequest.BodyPublishers.noBody() :
            HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data))).build(), HttpResponse.BodyHandlers.ofString());
        try {
            return new Reply(response.statusCode(), response.body().isBlank() ? json.createObjectNode() : json.readTree(response.body()));
        } catch (RuntimeException malformed) {
            throw new IOException("Invalid Calendar response");
        }
    }
}
