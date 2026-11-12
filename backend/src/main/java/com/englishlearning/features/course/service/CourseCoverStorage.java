package com.englishlearning.features.course.service;

import com.englishlearning.features.paymentaccount.storage.SupabaseQrStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class CourseCoverStorage {
    private static final String BUCKET = "course-covers";
    private final SupabaseQrStorage storage;

    public CourseCoverStorage(
            @Value("${app.bank-qr.storage-origin:}") String origin,
            @Value("${app.bank-qr.service-key:}") String key,
            @Value("${app.bank-qr.bucket:}") String evidenceBucket, JsonMapper json) {
        if (BUCKET.equals(evidenceBucket)) throw new IllegalStateException("Course covers require a separate bucket");
        this.storage = new SupabaseQrStorage(origin, BUCKET, key, json);
    }

    public void put(String object, byte[] png) { storage.put(object,png); }
    public byte[] read(String object) { return storage.read(object); }
    public void delete(String object) { storage.delete(object); }
}
