#!/usr/bin/env python3
"""Run two isolated Minecraft processes prepared by prepareClientOnlySmoke."""
import json
import os
from pathlib import Path
import subprocess
import time

root = Path(__file__).resolve().parents[1] / 'build' / 'client-smoke'
for name in ('port.txt', 'PASS.txt', 'open-device', 'open-storage'):
    (root / name).unlink(missing_ok=True)
processes = []
try:
    for role in ('host', 'client'):
        spec = json.loads((root / f'{role}-command.json').read_text())
        with (root / f'{role}.log').open('w') as log:
            processes.append((role, subprocess.Popen(spec['command'], cwd=spec['cwd'], stdout=log, stderr=subprocess.STDOUT)))
    deadline = time.monotonic() + 180
    while any(p.poll() is None for _, p in processes) and time.monotonic() < deadline:
        if any(p.poll() not in (None, 0) for _, p in processes):
            break
        time.sleep(.5)
finally:
    for role, process in processes:
        if process.poll() is None:
            process.terminate()
        print(role, 'exit', process.wait())
if not (root / 'PASS.txt').exists():
    raise SystemExit(f'Remote client test did not pass; see {root}/host.log and client.log')
print((root / 'PASS.txt').read_text())
