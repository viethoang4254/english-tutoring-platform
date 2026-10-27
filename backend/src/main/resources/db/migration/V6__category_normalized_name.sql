-- Fail on conflicting existing names; never rewrite or discard Category data.
CREATE UNIQUE INDEX uq_course_categories_normalized_name
    ON public.course_categories (lower(btrim(name)));
