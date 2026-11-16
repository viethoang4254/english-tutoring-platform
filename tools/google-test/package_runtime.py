"""Hosted Windows packaging only; never loads application credentials."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import zlib

if os.environ.get('GITHUB_ACTIONS') != 'true' or os.environ.get('RUNNER_OS') != 'Windows':
    raise SystemExit('Hosted Windows packaging required')
repo = Path(__file__).resolve().parents[2]
output = Path(os.environ['RUNNER_TEMP']) / 'google-oauth-package'
output.mkdir(exist_ok=False)
runtime = output / 'runtime'
(runtime / 'backend').mkdir(parents=True)
jars = list((repo / 'backend/target').glob('english-learning-backend-*.jar'))
if len(jars) != 1:
    raise SystemExit('Expected one executable JAR')
shutil.copyfile(jars[0], runtime / 'backend/application.jar')
front = runtime / 'frontend'
front.mkdir()
for directory in ('.next', 'node_modules', 'public'):
    source = repo / 'frontend' / directory
    if source.exists():
        shutil.copytree(source, front / directory, ignore=shutil.ignore_patterns('cache') if directory == '.next' else None)
for name in ('package.json', 'package-lock.json'):
    shutil.copyfile(repo / 'frontend' / name, front / name)
files = {}
for file in runtime.rglob('*'):
    if file.is_file():
        if file.name == '.env' or file.name.startswith('.env.') or file.suffix in ('.p12', '.jks', '.key'):
            raise SystemExit('Unexpected configuration material in runtime bundle')
        files[file.relative_to(runtime).as_posix()] = hashlib.sha256(file.read_bytes()).hexdigest()
migrations = {}
for file in (repo / 'backend/src/main/resources/db/migration').glob('V*.sql'):
    crc = 0
    for line in file.read_text(encoding='utf-8-sig').splitlines():
        crc = zlib.crc32(line.encode('utf-8'), crc)
    migrations[file.name] = crc if crc < 2**31 else crc - 2**32
receipt = dict(commit=os.environ['GITHUB_SHA'], repository=os.environ['GITHUB_REPOSITORY'],
               runId=os.environ['GITHUB_RUN_ID'], acceptanceRunId=os.environ['ACCEPTANCE_RUN_ID'],
               platform='win32-x64', apiOrigin='http://localhost:8081', files=files, migrations=migrations)
(output / 'receipt.json').write_text(json.dumps(receipt, indent=2), encoding='utf-8')
shutil.make_archive(str(output / 'runtime'), 'zip', runtime)
digest = hashlib.sha256((output / 'runtime.zip').read_bytes()).hexdigest()
(output / 'SHA256SUMS').write_text(digest + '  runtime.zip\n', encoding='ascii')
shutil.rmtree(runtime)  # Only the fresh staging directory created above.
