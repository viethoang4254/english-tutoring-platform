package com.englishlearning.features.googlecalendar;

import java.net.URI;
import java.nio.file.*;
import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public final class GoogleSettings {
    public static final String PATH="/api/v1/integrations/google-calendar";
    public static final String SCOPE="https://www.googleapis.com/auth/calendar.app.created";
    public final String clientId,clientSecret,redirectUri,keyId,frontend;
    public final boolean enabled,secure,syncEnabled;
    private final byte[] key;
    public GoogleSettings(Environment env) {
        clientId=env.getProperty("APP_GOOGLE_CALENDAR_CLIENT_ID","");
        clientSecret=env.getProperty("APP_GOOGLE_CALENDAR_CLIENT_SECRET","");
        redirectUri=env.getProperty("APP_GOOGLE_CALENDAR_REDIRECT_URI","");
        keyId=env.getProperty("APP_GOOGLE_CALENDAR_TOKEN_KEY_ID","");
        String origins=env.getProperty("app.security.browser.allowed-origins","");
        frontend=safeOrigin(origins.split(",",-1)[0].trim());
        secure=!env.getProperty("app.security.browser.local-http",Boolean.class,false);
        byte[] loaded=null;
        boolean valid=false;
        try {
            URI callback=URI.create(redirectUri),ui=URI.create(frontend);
            boolean tls="https".equals(callback.getScheme())&&"https".equals(ui.getScheme());
            boolean local=!secure&&"http".equals(callback.getScheme())&&"http".equals(ui.getScheme())
                &&com.englishlearning.features.auth.security.BrowserAuthSettings.loopback(callback.getHost())
                &&com.englishlearning.features.auth.security.BrowserAuthSettings.loopback(ui.getHost());
            var scopes=Set.of(env.getProperty("APP_GOOGLE_CALENDAR_SCOPES","openid email "+SCOPE).trim().split(" +"));
            valid=(tls||local)&&callback.getHost()!=null&&callback.getRawUserInfo()==null&&callback.getRawQuery()==null
                &&callback.getRawFragment()==null&&(PATH+"/callback").equals(callback.getPath())
                &&ui.getHost()!=null&&ui.getRawUserInfo()==null&&ui.getRawQuery()==null&&ui.getRawFragment()==null
                &&(ui.getPath().isEmpty()||ui.getPath().equals("/"))
                &&scopes.equals(Set.of("openid","email",SCOPE))&&!clientId.isBlank()&&!clientSecret.isBlank()
                &&keyId.matches("[A-Za-z0-9_-]{1,40}");
            if(valid&&env.getProperty("APP_GOOGLE_CALENDAR_ENABLED",Boolean.class,false))
                loaded=Base64.getDecoder().decode(Files.readString(Path.of(env.getProperty("APP_GOOGLE_CALENDAR_TOKEN_KEY_FILE",""))).trim());
        } catch(Exception invalid) { valid=false; }
        key=loaded;
        syncEnabled=env.getProperty("APP_GOOGLE_CALENDAR_SYNC_ENABLED",Boolean.class,false);
        enabled=valid&&loaded!=null&&loaded.length==32&&env.getProperty("APP_GOOGLE_CALENDAR_ENABLED",Boolean.class,false);
    }
    private static String safeOrigin(String value){
        try{URI uri=URI.create(value);boolean https="https".equals(uri.getScheme());
            boolean local="http".equals(uri.getScheme())&&com.englishlearning.features.auth.security.BrowserAuthSettings.loopback(uri.getHost());
            return (https||local)&&uri.getHost()!=null&&uri.getRawUserInfo()==null&&uri.getRawQuery()==null&&uri.getRawFragment()==null
                &&(uri.getPath().isEmpty()||uri.getPath().equals("/"))?value:"";
        }catch(Exception invalid){return "";}
    }
    byte[] key() { if(!enabled)throw new GoogleFailure("UNAVAILABLE");return key.clone(); }
    public String returnUrl(String result) { return frontend.replaceAll("/$","")+"/account?googleCalendar="+result; }
}
