# Controlled TEST upgrade — preparation only

No backup, restore, migration, deployment or course creation was executed by this
change. Target only project zobxcixrxsazmngcthvg; preserve OAuth/JWT keys and secrets.

## Matching release

Run TASK-009 on the new launcher commit with package_after_run empty. After every
mandatory job passes, run google-test-package.yml on the exact same commit with
that new acceptance_run_id. Run 44331529516 validates only 8ed291fb, not this change.
Validate the GitHub artifact digest, SHA256SUMS, receipt commit and package run ID;
extract into a new isolated directory. Backend/frontend must share that receipt.
Do not overwrite the old installation. Packaging runs PowerShell 5.1 migration
fixtures and a negative artifact revision check alongside existing package checks.

## Recovery gates (separate authorization required)

PostgreSQL 18 pg_dump/pg_restore are installed under E:\PostgreSQL\18\bin.
Docker was not found on PATH during inspection. Provision a new disposable local
PostgreSQL cluster/database or an approved disposable hosted runner before rehearsal.
Never use live TEST or production as the restore destination.

1. Reconfirm TEST project-qualified identity against independent retained metadata,
   using sslmode=verify-full and the configured CA. Keep credentials in protected
   process environment, never command arguments/logs. Quiesce TEST writes for a
   consistent backup and comparison window; preserve the existing records.
2. Create a fresh custom-format TEST backup in a new restricted directory with
   pg_dump --format=custom --no-owner --no-acl. Inventory required roles/extensions
   and any external storage separately; a database dump does not back up storage.
3. Compute SHA-256, retain the manifest separately, and run pg_restore --list.
   Archive readability is not proof of recovery. Existing pre-V12 archives are not
   a substitute for a fresh pre-upgrade recovery point.
4. Restore into an empty, explicitly verified disposable database with
   pg_restore --exit-on-error --single-transaction --no-owner --no-acl.
   Never use --clean. Do not print restored personal rows or credentials.
5. Verify schema objects, constraints, sequences, V1-V12 history/checksums and
   per-table row counts against the backup snapshot. Confirm Teacher 11 and existing
   OAuth ciphertext are preserved without displaying secrets. Record evidence.
6. Rehearse only pending V13-V15 there with the reviewed Flyway version and migration
   files. Require info to show exactly those three pending, target=15,
   cleanDisabled=true, baselineOnMigrate=false; never repair or baseline. Validate
   V1-V15 checksums and existing data preservation. Keep application/provider workers
   stopped. V13 creates triggers/queue entries; V14 backfills invitation mappings;
   V15 creates reminder storage. Review any conflict before touching live TEST.

Only after recovery proof and explicit migration approval repeat the reviewed plan
on TEST. Deployment requires separate approval. Preserve old artifacts, but do not
blindly launch an old V12/V13-only launcher against V15. Database rollback is an
explicit recovery operation, not dropping migrations or their tables.

## Launch and demo gates

Use the new scripts with an external manifest pinning the new approved commit,
package run, receipt and extracted runtime. First run powershell.exe -File
tools/google-test/Start-TestEnvironment.ps1 -Manifest <external-manifest> -DryRun,
then -VerifyDatabase for authorized read-only verification. StartBackend and frontend
startup remain separate deployment operations. Flyway, SQL initialization and
Hibernate schema mutation stay disabled. Sync/invitations/reminders/Contact delivery
stay false; no SMTP credentials are added by this launcher.

After runtime health and schema checks, obtain Teacher 11's interactive login.
Verify active categories and existing owned records before the documented resumable
four-DRAFT-course/twenty-session workflow. Preserve demo-courses.json unchanged.
Publication is separate: approved receiving account and genuine Teacher Meet link
are required. Never simulate payment confirmation or enable external delivery here.
