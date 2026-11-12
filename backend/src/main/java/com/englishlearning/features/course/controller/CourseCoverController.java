package com.englishlearning.features.course.controller;

import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.course.repository.CourseRepository;
import com.englishlearning.features.course.service.*;
import com.englishlearning.features.paymentaccount.service.*;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class CourseCoverController {
    private final CourseService courses;
    private final CourseRepository repository;
    private final CourseCoverStorage storage;

    public CourseCoverController(CourseService courses, CourseRepository repository, CourseCoverStorage storage) {
        this.courses = courses;
        this.repository = repository;
        this.storage = storage;
    }

    @PutMapping(value="/teacher/courses/{id}/cover", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> replace(@AuthenticationPrincipal AccountPrincipal actor, @PathVariable String id,
            @RequestPart("image") MultipartFile image) throws IOException {
        long course = CourseInput.id(id);
        if (!"DRAFT".equals(courses.get(actor.id(), course).status()))
            throw new CourseFailure(CourseFailure.Reason.NOT_DRAFT);
        if (image.isEmpty() || image.getSize() > QrImage.MAX) throw CourseFailure.invalid();
        String object = UUID.randomUUID() + ".png";
        try {
            storage.put(object, QrImage.sanitize(image.getBytes()));
        } catch (ReceivingFailure failure) {
            if (failure.reason() == ReceivingFailure.Reason.INVALID) throw CourseFailure.invalid();
            throw failure;
        }
        // Revalidate ownership and draft state in the short DB transaction after upload.
        String previous;
        try {
            previous = courses.replaceCover(actor.id(),course,object);
        } catch (CourseFailure rejected) {
            discard(object);
            throw rejected;
        }
        // Never delete the new object when the commit outcome is unknown.
        if (previous != null) discard(previous);
        return ResponseEntity.noContent().build();
    }

    private void discard(String object) {
        try { storage.delete(object); }
        catch (ReceivingFailure failure) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Private course cover cleanup deferred");
        }
    }

    @GetMapping(value="/public/courses/{id}/cover", produces=MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> published(@PathVariable String id) {
        return image(repository.cover(CourseInput.id(id), true));
    }

    @GetMapping(value="/teacher/courses/{id}/cover", produces=MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> owned(@AuthenticationPrincipal AccountPrincipal actor, @PathVariable String id) {
        long course = CourseInput.id(id);
        courses.get(actor.id(),course);
        return image(repository.cover(course,false));
    }

    private ResponseEntity<byte[]> image(String object) {
        if (object == null) throw new CourseFailure(CourseFailure.Reason.MISSING);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
                .body(storage.read(object));
    }
}
