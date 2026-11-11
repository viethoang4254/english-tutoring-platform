package com.englishlearning.features.course.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record CourseOutline(@NotNull @Size(max=10000) String introduction,
        @NotNull @Size(max=10000) String objectives, @NotNull @Size(max=10000) String audience,
        @NotNull @Size(max=10000) String requirements, @NotNull @Size(max=10000) String outcomes,
        @NotNull @Size(max=60) List<@NotNull @Valid Stage> stages) {
    public record Stage(@NotBlank @Size(max=200) String title, @NotNull @Size(max=100) List<@NotNull @Valid Topic> topics) {}
    public record Topic(@NotBlank @Size(max=200) String title, @NotBlank @Size(max=2000) String description) {}
}
