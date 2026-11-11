ALTER TABLE public.courses
  ADD COLUMN introduction text,
  ADD COLUMN learning_objectives text,
  ADD COLUMN target_students text,
  ADD COLUMN entry_requirements text,
  ADD COLUMN learning_outcomes text;

CREATE TABLE public.course_stages (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  course_id bigint NOT NULL REFERENCES public.courses(id) ON DELETE CASCADE,
  position integer NOT NULL CHECK (position > 0),
  title text NOT NULL CHECK (btrim(title) <> ''),
  UNIQUE(course_id, position)
);
CREATE TABLE public.course_topics (
  stage_id bigint NOT NULL REFERENCES public.course_stages(id) ON DELETE CASCADE,
  position integer NOT NULL CHECK (position > 0),
  title text NOT NULL CHECK (btrim(title) <> ''),
  description text NOT NULL,
  PRIMARY KEY(stage_id, position)
);
