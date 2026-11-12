package com.englishlearning.features.course;

import com.englishlearning.features.course.service.CourseCoverStorage;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Test-classpath only. Images live for the lifetime of the disposable browser backend. */
@Component
@Primary
@Profile("task009-cover")
public class BrowserCourseCoverStorage extends CourseCoverStorage {
    private final ConcurrentHashMap<String, byte[]> objects = new ConcurrentHashMap<>();

    public BrowserCourseCoverStorage(JsonMapper mapper) {
        super("", "", "", mapper);
        if (!"github-hosted".equals(System.getenv("RUNNER_ENVIRONMENT"))
                || !"jdbc:postgresql://127.0.0.1:15434/task009_profile_browser_ci".equals(System.getenv("DATABASE_URL"))) {
            throw new IllegalStateException("Disposable TASK-009 backend required");
        }
    }

    @Override public void put(String object, byte[] png) {
        if (objects.putIfAbsent(object, png.clone()) != null) throw new IllegalStateException("Duplicate cover");
    }
    @Override public byte[] read(String object) { return objects.get(object).clone(); }
    @Override public void delete(String object) { objects.remove(object); }
}
