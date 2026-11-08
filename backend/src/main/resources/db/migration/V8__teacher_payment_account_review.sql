-- One terminal decision per immutable submission is its durable review history.
ALTER TABLE public.teacher_payment_accounts
 ADD COLUMN reviewed_by BIGINT REFERENCES public.users(id) ON DELETE RESTRICT,
 ADD COLUMN reviewed_at TIMESTAMPTZ,
 ADD COLUMN rejection_reason TEXT,
 ADD CONSTRAINT ck_receiving_audit CHECK (
   (review_status IN ('UNREVIEWED','PENDING') AND reviewed_by IS NULL AND reviewed_at IS NULL AND rejection_reason IS NULL)
   OR (review_status='APPROVED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND rejection_reason IS NULL)
   OR (review_status='REJECTED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL
       AND rejection_reason IS NOT NULL AND length(btrim(rejection_reason)) BETWEEN 1 AND 2000));

CREATE FUNCTION public.preserve_receiving_review() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF ROW(NEW.review_status,NEW.reviewed_by,NEW.reviewed_at,NEW.rejection_reason)
    IS DISTINCT FROM ROW(OLD.review_status,OLD.reviewed_by,OLD.reviewed_at,OLD.rejection_reason)
    AND NOT (OLD.review_status='PENDING' AND NEW.review_status IN ('APPROVED','REJECTED')) THEN
   RAISE EXCEPTION 'Receiving review is terminal' USING ERRCODE='23514';
 END IF;
 IF NEW.status='ACTIVE' AND OLD.status<>'ACTIVE'
    AND NOT (OLD.review_status='PENDING' AND NEW.review_status='APPROVED') THEN
   RAISE EXCEPTION 'Receiving selection requires a new approval' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER preserve_receiving_review BEFORE UPDATE ON public.teacher_payment_accounts
 FOR EACH ROW EXECUTE FUNCTION public.preserve_receiving_review();
