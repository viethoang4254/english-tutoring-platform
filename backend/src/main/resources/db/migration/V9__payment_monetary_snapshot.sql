-- Classify only pre-V9 rows as historical without recalculating their values.
ALTER TABLE public.payments
 ALTER COLUMN discount_amount TYPE NUMERIC(19,6),
 ADD COLUMN monetary_policy TEXT NOT NULL DEFAULT 'LEGACY',
 ADD COLUMN discount_percent NUMERIC(5,2),
 ADD COLUMN rounding_adjustment NUMERIC(7,6) NOT NULL DEFAULT 0,
 ADD COLUMN bank_bin TEXT,
 ADD COLUMN bank_name TEXT,
 ADD COLUMN account_number TEXT,
 ADD COLUMN account_holder_name TEXT,
 ADD COLUMN qr_payload TEXT;

ALTER TABLE public.payments ALTER COLUMN monetary_policy SET DEFAULT 'VND_HALF_UP_V1';
CREATE FUNCTION public.require_new_payment_policy() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.monetary_policy IS DISTINCT FROM 'VND_HALF_UP_V1' THEN
  RAISE EXCEPTION 'New payments require the current monetary policy'
   USING ERRCODE='23514', CONSTRAINT='ck_payment_new_policy';
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER require_new_payment_policy BEFORE INSERT ON public.payments
 FOR EACH ROW EXECUTE FUNCTION public.require_new_payment_policy();

ALTER TABLE public.payments DROP CONSTRAINT ck_payments_amounts;
ALTER TABLE public.payments ADD CONSTRAINT ck_payments_amounts CHECK (
 original_amount <> 'NaN'::numeric AND discount_amount <> 'NaN'::numeric
 AND final_amount <> 'NaN'::numeric AND rounding_adjustment <> 'NaN'::numeric
 AND original_amount >= 0 AND discount_amount >= 0 AND discount_amount <= original_amount AND final_amount > 0
 AND (
  (monetary_policy='LEGACY' AND discount_percent IS NULL AND rounding_adjustment=0
   AND discount_amount=round(discount_amount,2) AND final_amount=original_amount-discount_amount)
  OR
  (monetary_policy='VND_HALF_UP_V1' AND original_amount>0 AND discount_percent IS NOT NULL
   AND discount_percent>=0 AND discount_percent<100
   AND discount_amount=original_amount*discount_percent/100
   AND final_amount=round(original_amount-discount_amount,0)
   AND final_amount=original_amount-discount_amount+rounding_adjustment
   AND final_amount=trunc(final_amount) AND final_amount<=9999999999999)
 ));
ALTER TABLE public.payments ADD CONSTRAINT ck_payment_transfer_snapshot CHECK (
 monetary_policy='LEGACY' OR
 (bank_bin IS NOT NULL AND bank_bin ~ '^[0-9]{6}$'
 AND bank_name IS NOT NULL AND length(btrim(bank_name))>0
 AND account_number IS NOT NULL AND account_number ~ '^[0-9]{6,19}$'
 AND account_holder_name IS NOT NULL AND length(btrim(account_holder_name))>0
 AND payment_code ~ '^ELP[0-9A-F]{22}$'
 AND qr_payload IS NOT NULL AND length(qr_payload) BETWEEN 40 AND 512));

CREATE FUNCTION public.preserve_payment_snapshot() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF ROW(NEW.enrollment_id,NEW.teacher_payment_account_id,NEW.payment_code,NEW.provider,
        NEW.original_amount,NEW.discount_amount,NEW.final_amount,NEW.currency,NEW.expires_at,
        NEW.created_at,NEW.monetary_policy,NEW.discount_percent,NEW.rounding_adjustment,
        NEW.bank_bin,NEW.bank_name,NEW.account_number,NEW.account_holder_name,NEW.qr_payload)
 IS DISTINCT FROM
    ROW(OLD.enrollment_id,OLD.teacher_payment_account_id,OLD.payment_code,OLD.provider,
        OLD.original_amount,OLD.discount_amount,OLD.final_amount,OLD.currency,OLD.expires_at,
        OLD.created_at,OLD.monetary_policy,OLD.discount_percent,OLD.rounding_adjustment,
        OLD.bank_bin,OLD.bank_name,OLD.account_number,OLD.account_holder_name,OLD.qr_payload) THEN
  RAISE EXCEPTION 'Payment snapshot is immutable' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER preserve_payment_snapshot BEFORE UPDATE ON public.payments
 FOR EACH ROW EXECUTE FUNCTION public.preserve_payment_snapshot();
