-- Preserve legacy bank identifiers and every historical Payment reference.
ALTER TABLE public.teacher_payment_accounts
 ADD COLUMN bank_bin TEXT,
 ADD COLUMN bank_name TEXT,
 ADD COLUMN review_status TEXT NOT NULL DEFAULT 'UNREVIEWED',
 ADD COLUMN qr_object TEXT,
 ADD COLUMN qr_sha256 TEXT,
 ADD COLUMN qr_size INTEGER,
 ADD COLUMN submission_key UUID,
 ADD COLUMN request_sha256 TEXT;
UPDATE public.teacher_payment_accounts SET status='INACTIVE' WHERE status='ACTIVE';
ALTER TABLE public.teacher_payment_accounts
 ADD CONSTRAINT ck_receiving_review CHECK (review_status IN ('UNREVIEWED','PENDING','APPROVED','REJECTED')),
 ADD CONSTRAINT ck_receiving_selection CHECK (status <> 'ACTIVE' OR review_status='APPROVED'),
 ADD CONSTRAINT ck_receiving_submission CHECK (review_status='UNREVIEWED' OR
   (bank_bin IS NOT NULL AND bank_bin ~ '^[0-9]{6}$' AND bank_name IS NOT NULL AND length(btrim(bank_name))>0
    AND qr_object IS NOT NULL AND qr_object ~ '^[0-9a-f-]{36}\.png$'
    AND qr_sha256 IS NOT NULL AND qr_sha256 ~ '^[0-9a-f]{64}$'
    AND qr_size IS NOT NULL AND qr_size BETWEEN 1 AND 5242880
    AND submission_key IS NOT NULL AND request_sha256 IS NOT NULL AND request_sha256 ~ '^[0-9a-f]{64}$'));
CREATE UNIQUE INDEX uq_receiving_pending ON public.teacher_payment_accounts(teacher_id) WHERE review_status='PENDING';
CREATE UNIQUE INDEX uq_receiving_submission_key ON public.teacher_payment_accounts(teacher_id,submission_key) WHERE submission_key IS NOT NULL;
CREATE UNIQUE INDEX uq_receiving_qr_object ON public.teacher_payment_accounts(qr_object) WHERE qr_object IS NOT NULL;

-- Corrections/replacements are new rows; review/selection may change in a later authorized batch.
CREATE FUNCTION public.preserve_receiving_submission() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF ROW(NEW.teacher_id,NEW.bank_code,NEW.bank_bin,NEW.bank_name,NEW.account_number,NEW.account_holder_name,
        NEW.provider,NEW.qr_object,NEW.qr_sha256,NEW.qr_size,NEW.submission_key,NEW.request_sha256,NEW.created_at)
    IS DISTINCT FROM
    ROW(OLD.teacher_id,OLD.bank_code,OLD.bank_bin,OLD.bank_name,OLD.account_number,OLD.account_holder_name,
        OLD.provider,OLD.qr_object,OLD.qr_sha256,OLD.qr_size,OLD.submission_key,OLD.request_sha256,OLD.created_at) THEN
   RAISE EXCEPTION 'Receiving submission is immutable' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER preserve_receiving_submission BEFORE UPDATE ON public.teacher_payment_accounts
 FOR EACH ROW EXECUTE FUNCTION public.preserve_receiving_submission();
