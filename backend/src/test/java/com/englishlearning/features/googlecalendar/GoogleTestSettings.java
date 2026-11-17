package com.englishlearning.features.googlecalendar;

import java.nio.file.*;
import java.util.Base64;
import org.springframework.mock.env.MockEnvironment;

final class GoogleTestSettings {
    static GoogleSettings create(Path folder)throws Exception{
        return create(folder,false);
    }
    static GoogleSettings create(Path folder,boolean invitations)throws Exception{
        Path key=folder.resolve("synthetic-key");Files.writeString(key,Base64.getEncoder().encodeToString(new byte[32]));
        return new GoogleSettings(new MockEnvironment().withProperty("APP_GOOGLE_CALENDAR_ENABLED","true")
            .withProperty("APP_GOOGLE_CALENDAR_INVITATIONS_ENABLED",Boolean.toString(invitations))
            .withProperty("APP_GOOGLE_CALENDAR_SYNC_ENABLED",Boolean.toString(invitations))
            .withProperty("APP_GOOGLE_CALENDAR_CLIENT_ID","synthetic-client").withProperty("APP_GOOGLE_CALENDAR_CLIENT_SECRET","synthetic-secret")
            .withProperty("APP_GOOGLE_CALENDAR_REDIRECT_URI","https://backend.example/api/v1/integrations/google-calendar/callback")
            .withProperty("APP_GOOGLE_CALENDAR_TOKEN_KEY_ID","test-1").withProperty("APP_GOOGLE_CALENDAR_TOKEN_KEY_FILE",key.toString())
            .withProperty("app.security.browser.allowed-origins","https://frontend.example"));
    }
}
