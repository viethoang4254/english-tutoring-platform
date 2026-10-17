"""CI-only authenticated STARTTLS capture sink. No relay or external delivery."""
import hmac
import json
import logging
import os
from pathlib import Path
import ssl
import time
import uuid

from aiosmtpd.controller import Controller
from aiosmtpd.smtp import AuthResult, LoginPassword


def main():
    if os.environ.get("GITHUB_ACTIONS") != "true" or os.environ.get("RUNNER_ENVIRONMENT") != "github-hosted":
        raise SystemExit("GitHub-hosted CI only")
    root = Path(os.environ["TASK009_DIR"])
    logging.disable(logging.CRITICAL)

    def authenticate(server, session, envelope, mechanism, data):
        valid = isinstance(data, LoginPassword) and hmac.compare_digest(
            data.login, os.environ["SMTP_USERNAME"].encode()
        ) and hmac.compare_digest(data.password, os.environ["SMTP_PASSWORD"].encode())
        return AuthResult(success=valid, handled=False)

    class Capture:
        async def handle_RCPT(self, server, session, envelope, address, options):
            if not address.lower().endswith("@example.invalid"):
                return "550 Test recipient domain required"
            envelope.rcpt_tos.append(address)
            return "250 OK"

        async def handle_DATA(self, server, session, envelope):
            target = root / "mail" / (uuid.uuid4().hex + ".json")
            temporary = target.with_suffix(".tmp")
            temporary.write_text(json.dumps({"to": envelope.rcpt_tos,
                                             "message": envelope.content.decode("utf-8")}), encoding="utf-8")
            temporary.replace(target)
            return "250 Captured locally; never relayed"

    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    context.load_cert_chain(root / "smtp-cert.pem", root / "smtp-private.pem")
    controller = Controller(Capture(), hostname="127.0.0.1", port=1025,
                            tls_context=context, require_starttls=True,
                            auth_required=True, auth_require_tls=True,
                            authenticator=authenticate)
    controller.start()
    try:
        while True:
            time.sleep(1)
    finally:
        controller.stop()


if __name__ == "__main__":
    main()
