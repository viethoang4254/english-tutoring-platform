"""Bounded TASK-009 CI support. Never targets a developer or shared database."""
import base64
from email import policy
from email.parser import Parser
import json
import os
from pathlib import Path
import re
import secrets
import signal
import smtplib
import socket
import ssl
import subprocess
import sys
import time
import urllib.request
import xml.etree.ElementTree as ET

REPO = Path(__file__).resolve().parents[2]
DB = "task009_profile_browser_ci"
URL = f"jdbc:postgresql://127.0.0.1:55434/{DB}"


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def root():
    require(os.environ.get("GITHUB_ACTIONS") == "true" and os.environ.get("RUNNER_ENVIRONMENT") == "github-hosted",
            "GitHub-hosted CI required")
    return Path(os.environ["RUNNER_TEMP"]) / "task009"


def run(args, *, data=None, cwd=None, timeout=120):
    result = subprocess.run(args, input=data, text=True, capture_output=True, cwd=cwd, timeout=timeout)
    require(result.returncode == 0, "CI command failed; private output withheld")
    return result.stdout.strip()


def resources(minimum=1536):
    memory = dict(re.findall(r"^(\w+):\s+(\d+)", Path("/proc/meminfo").read_text(), re.M))
    require(int(memory["MemTotal"]) >= 6_500_000, "Runner needs at least 7 GB RAM")
    require(int(memory["MemAvailable"]) // 1024 >= minimum, "Insufficient available memory")


def export(values):
    with open(os.environ["GITHUB_ENV"], "a", encoding="utf-8") as stream:
        for key, value in values.items():
            require("\n" not in str(value), "Invalid environment value")
            stream.write(f"{key}={value}\n")
    os.environ.update({key: str(value) for key, value in values.items()})


def prepare():
    resources(3072)
    directory = root()
    directory.mkdir(mode=0o700)
    (directory / "mail").mkdir(mode=0o700)
    credentials = {key: secrets.token_hex(24) for key in
                   ("DATABASE_PASSWORD", "SMTP_PASSWORD", "TEST_SMTP_TRUST_PASSWORD")}
    credentials["APP_SECURITY_PASSWORDRESET_HMACSECRET"] = base64.b64encode(secrets.token_bytes(32)).decode()
    for value in credentials.values():
        print("::add-mask::" + value)
    export({**credentials, "TASK009_DIR": str(directory), "TASK009_CONTAINER": "task009-" + secrets.token_hex(6),
            "DATABASE_URL": URL, "DATABASE_USERNAME": "task007_test",
            "SMTP_HOST": "127.0.0.1", "SMTP_PORT": "1025", "SMTP_USERNAME": "task009_sink",
            "SMTP_FROM": "noreply@example.invalid", "TEST_SMTP_USER": "task009_sink",
            "TEST_SMTP_PASSWORD": credentials["SMTP_PASSWORD"],
            "TEST_SMTP_TRUST_STORE": str(directory / "trust.p12"), "TEST_SMTP_CAPTURE_DIR": str(directory / "mail"),
            "APP_SECURITY_JWT_PRIVATEKEY": (directory / "jwt-private.pem").as_uri(),
            "APP_SECURITY_JWT_PUBLICKEY": (directory / "jwt-public.pem").as_uri(),
            "APP_SECURITY_BROWSER_LOCALHTTP": "true", "APP_SECURITY_BROWSER_ALLOWEDORIGINS": "http://127.0.0.1:3000",
            "SERVER_ADDRESS": "127.0.0.1", "SERVER_PORT": "8081",
            "SPRING_FLYWAY_ENABLED": "true", "SPRING_FLYWAY_CLEANDISABLED": "true",
            "SPRING_JPA_HIBERNATE_DDLAUTO": "validate", "SPRING_SQL_INIT_MODE": "never",
            "NEXT_PUBLIC_API_ORIGIN": "http://127.0.0.1:8081",
            "MAVEN_OPTS": "-Xms64m -Xmx256m -XX:MaxMetaspaceSize=192m"})
    (directory / "pg-password").write_text(credentials["DATABASE_PASSWORD"], encoding="utf-8")
    run(["openssl", "genpkey", "-algorithm", "RSA", "-pkeyopt", "rsa_keygen_bits:2048", "-out", str(directory / "jwt-private.pem")])
    run(["openssl", "pkey", "-in", str(directory / "jwt-private.pem"), "-pubout", "-out", str(directory / "jwt-public.pem")])
    run(["openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "2", "-subj", "/CN=localhost",
         "-addext", "subjectAltName=IP:127.0.0.1,DNS:localhost", "-keyout", str(directory / "smtp-private.pem"),
         "-out", str(directory / "smtp-cert.pem")])
    # Public CA certificate only: Java can load it without a password in process arguments.
    run(["keytool", "-J-Dkeystore.pkcs12.certProtectionAlgorithm=NONE", "-J-Dkeystore.pkcs12.macAlgorithm=NONE",
         "-importcert", "-noprompt", "-alias", "sink", "-file", str(directory / "smtp-cert.pem"),
         "-keystore", str(directory / "trust.p12"), "-storetype", "PKCS12", "-storepass:env", "TEST_SMTP_TRUST_PASSWORD"])
    run([sys.executable, "-m", "venv", str(directory / "venv")])
    run([str(directory / "venv/bin/pip"), "install", "aiosmtpd==1.4.6"], timeout=180)


def spawn(name, args, cwd=REPO):
    resources()
    with (root() / (name + ".log")).open("w") as log:
        process = subprocess.Popen(args, cwd=cwd, stdout=log, stderr=log, start_new_session=True)
    (root() / (name + ".pid")).write_text(str(process.pid))


def wait_for(check):
    for _ in range(60):
        try:
            check()
            return
        except (OSError, RuntimeError, smtplib.SMTPException):
            time.sleep(1)
    raise RuntimeError("Required isolated service did not become ready")


def smtp_check():
    context = ssl.create_default_context(cafile=str(root() / "smtp-cert.pem"))
    with smtplib.SMTP("127.0.0.1", 1025, timeout=5) as client:
        client.ehlo()
        require(not client.has_extn("auth"), "AUTH exposed before TLS")
        require(client.mail("noreply@example.invalid")[0] == 530, "STARTTLS must be mandatory")
        client.starttls(context=context)
        client.ehlo()
        require(client.mail("noreply@example.invalid")[0] == 530, "SMTP authentication must be mandatory")
        client.login(os.environ["SMTP_USERNAME"], os.environ["SMTP_PASSWORD"])
        require(client.mail("noreply@example.invalid")[0] == 250, "Authenticated SMTP unavailable")
        require(client.rcpt("forbidden@example.com")[0] == 550, "External recipients must be rejected")


def maven(arguments):
    resources()
    run(["bash", "mvnw", "-B", *arguments], cwd=REPO / "backend", timeout=600)


def smtp_test():
    maven(["test-compile", "dependency:build-classpath", "-Dmdep.includeScope=test",
           "-Dmdep.outputFile=" + str(root() / "classpath")])
    agent = [p for p in (root() / "classpath").read_text().split(os.pathsep) if Path(p).name.startswith("mockito-core-")]
    require(len(agent) == 1, "Expected one resolved Mockito agent")
    jvm = "-Xms128m -Xmx512m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m -javaagent:" + agent[0]
    try:
        maven(["-DlocalResetSmtp=true", "-Dtest=LocalResetSmtpTests", "-DforkCount=1", "-DreuseForks=true",
               "-Djunit.jupiter.execution.parallel.enabled=false", "-DargLine=" + jvm, "test"])
    finally:
        report = REPO / "backend/target/surefire-reports/TEST-com.englishlearning.features.auth.service.LocalResetSmtpTests.xml"
        require(report.exists(), "SMTP test report missing")
        suite = ET.parse(report).getroot()
        counts = {k: int(suite.attrib[k]) for k in ("tests", "failures", "errors", "skipped")}
        counts["passed"] = counts["tests"] - counts["failures"] - counts["errors"] - counts["skipped"]
        print("SMTP: " + json.dumps(counts))
        require(counts["passed"] == 1 and counts["tests"] == 1, "SMTP acceptance did not pass")


def container():
    name = os.environ["TASK009_CONTAINER"]
    require(re.fullmatch(r"task009-[a-f0-9]{12}", name) is not None, "Invalid container identity")
    require(os.environ.get("DATABASE_URL") == URL and os.environ.get("DATABASE_USERNAME") == "task007_test",
            "Exact isolated database target required")
    return name


def sql(statement):
    name = container()
    info = json.loads(run(["docker", "inspect", name]))[0]
    require(info["Config"]["Labels"].get("task009") == name, "Unowned container")
    require(info["HostConfig"]["PortBindings"]["5432/tcp"] == [{"HostIp": "127.0.0.1", "HostPort": "55434"}], "Unsafe binding")
    return run(["docker", "exec", "-i", name, "psql", "-X", "-U", "task007_test", "-d", DB,
                "-At", "-v", "ON_ERROR_STOP=1"], data=statement)


def start_db():
    resources()
    with socket.socket() as check:
        check.bind(("127.0.0.1", 55434))
    name = container()
    run(["docker", "run", "--detach", "--name", name, "--label", "task009=" + name,
         "--memory", "512m", "--publish", "127.0.0.1:55434:5432", "--env", "POSTGRES_USER=task007_test",
         "--env", "POSTGRES_DB=" + DB, "--env", "POSTGRES_PASSWORD_FILE=/run/secrets/password",
         "--mount", "type=bind,src=" + str(root() / "pg-password") + ",dst=/run/secrets/password,readonly",
         "postgres:18", "-c", "shared_buffers=128MB", "-c", "max_connections=30"], timeout=180)
    wait_for(lambda: require(sql("SELECT current_database()||'|'||current_user") == DB + "|task007_test", "Database identity mismatch"))
    require(sql("SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema')") == "0",
            "Fresh empty database required; never clean or reuse")
    (root() / "empty-db-verified").touch()


def start_backend():
    require((root() / "empty-db-verified").exists(), "Missing empty-database preflight")
    container()
    with socket.socket() as check:
        check.bind(("127.0.0.1", 8081))
    maven(["-DskipTests", "package"])
    jars = list((REPO / "backend/target").glob("english-learning-backend-*.jar"))
    require(len(jars) == 1, "Expected one executable backend JAR")
    spawn("backend", ["java", "-Xms128m", "-Xmx512m", "-XX:MaxMetaspaceSize=256m",
                      "-Djavax.net.ssl.trustStore=" + str(root() / "trust.p12"),
                      "-Djavax.net.ssl.trustStoreType=PKCS12", "-jar", str(jars[0])])
    def ready():
        with urllib.request.urlopen("http://127.0.0.1:8081/api/v1/auth/csrf", timeout=3) as response:
            require(response.status == 200 and bool(json.load(response).get("token")), "Backend not ready")
    wait_for(ready)
    require(sql("SELECT string_agg(version,',' ORDER BY installed_rank) FROM flyway_schema_history WHERE success") == "1,2,3,4,5", "Unexpected migration history")
    require(sql("SELECT count(*) FROM flyway_schema_history") == "5", "Unexpected migration entry")
    require(sql("SELECT count(*) FROM pg_tables WHERE schemaname='public' AND tablename<>'flyway_schema_history'") == "23", "Unexpected table count")
    print("Isolated backend ready; V1-V5 and 23 application tables verified")


def fixture():
    data = json.load(sys.stdin)
    identifier = str(data["id"])
    require(re.fullmatch(r"[1-9][0-9]*", identifier) is not None, "Invalid fixture ID")
    require(sql(f"SELECT count(*) FROM users WHERE id={identifier} AND email LIKE 'ci-%@example.invalid'") == "1", "Fixture account required")
    if data["action"] == "admin":
        require(sql(f"SELECT role FROM users WHERE id={identifier}") == "STUDENT", "Fresh Student fixture required")
        sql(f"UPDATE users SET role='ADMIN',student_login_without_verification=false,email_verified=true WHERE id={identifier}")
    elif data["action"] == "lock":
        sql(f"UPDATE users SET locked=true WHERE id={identifier}")
    elif data["action"] == "snapshot":
        print(sql(f"""SELECT json_build_object(
          'security',md5((to_jsonb(u)-ARRAY['full_name','phone','updated_at'])::text),
          'applications',(SELECT md5(coalesce(jsonb_agg(to_jsonb(a) ORDER BY id)::text,'[]')) FROM teacher_applications a WHERE teacher_id=u.id),
          'teaching',(SELECT md5(coalesce(jsonb_agg(to_jsonb(p))::text,'[]')) FROM teacher_profiles p WHERE teacher_id=u.id),
          'verificationCount',(SELECT count(*) FROM email_verification_credentials WHERE user_id=u.id)) FROM users u WHERE id={identifier}"""))
    else:
        raise RuntimeError("Unknown fixture operation")


def verification_code():
    email = json.load(sys.stdin)["email"]
    require(re.fullmatch(r"ci-[a-f0-9-]+@example\.invalid", email) is not None, "Fixture recipient required")
    matches = []
    for path in (root() / "mail").glob("*.json"):
        capture = json.loads(path.read_text())
        if capture["to"] == [email]:
            message = Parser(policy=policy.default).parsestr(capture["message"])
            if message["Subject"] == "Verify your English Tutoring Platform email":
                matches.extend(re.findall(r"(?m)^[A-Za-z0-9_-]{43}\r?$", message.get_content()))
    require(len(matches) == 1, "Expected exactly one captured verification message")
    # Private pipe to the browser fixture, never a workflow log.
    print(matches[0].strip())


def cleanup():
    for name in ("backend", "smtp"):
        pidfile = root() / (name + ".pid")
        if pidfile.exists():
            pid = int(pidfile.read_text())
            command = Path(f"/proc/{pid}/cmdline")
            if command.exists() and (str(root()).encode() in command.read_bytes() or b"smtp_sink.py" in command.read_bytes()):
                os.killpg(pid, signal.SIGTERM)
    name = os.environ.get("TASK009_CONTAINER", "")
    if re.fullmatch(r"task009-[a-f0-9]{12}", name):
        result = subprocess.run(["docker", "inspect", name], capture_output=True, text=True)
        if result.returncode == 0 and json.loads(result.stdout)[0]["Config"]["Labels"].get("task009") == name:
            run(["docker", "rm", "-f", "-v", name])


if __name__ == "__main__":
    try:
        os.umask(0o077)
        root()
        action = sys.argv[1]
        if action == "prepare": prepare()
        elif action == "smtp":
            with socket.socket() as check:
                check.bind(("127.0.0.1", 1025))
            spawn("smtp", [str(root() / "venv/bin/python"), str(Path(__file__).with_name("smtp_sink.py"))])
            wait_for(smtp_check)
        elif action == "smtp-test": smtp_test()
        elif action == "database": start_db()
        elif action == "backend": start_backend()
        elif action == "fixture": fixture()
        elif action == "verification-code": verification_code()
        elif action == "resources": resources()
        elif action == "cleanup": cleanup()
        else: raise RuntimeError("Unknown CI action")
    except Exception:
        # Subprocess errors and SMTP diagnostics can contain credential material.
        print("TASK-009 CI step failed; sensitive diagnostics withheld", file=sys.stderr)
        sys.exit(1)
