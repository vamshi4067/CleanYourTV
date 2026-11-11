#!/usr/bin/env python3
"""Check a shareable tree without printing matched confidential values.

Uses the Python standard library. Exit nonzero on findings. This deliberately
does not claim to identify private content inside image pixels, audio, or video.
"""
import re
import struct
import sys
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SKIP = {'.git', '.gradle', 'build', '__pycache__', '.venv', 'venv'}
FORBIDDEN_DIRS = {'backup', 'tv-backup', 'shots', 'work', 'adbkey', '.tv-build', 'traces', 'bench-results'}
FORBIDDEN_SUFFIXES = {'.apk', '.aab', '.jks', '.keystore', '.p12', '.pfx', '.pem', '.key', '.hprof', '.log'}
TEXT = {'.md', '.txt', '.java', '.xml', '.json', '.py', '.sh', '.ps1', '.gradle', '.properties', '.toml', '.yml', '.yaml', '.html', '.css', '.js', '.bat', '.gitignore', '.gitattributes'}
RULES = {
    'private-key': re.compile(r'-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----'),
    'github-token': re.compile(r'\b(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{35,})\b'),
    'cloud-access-key': re.compile(r'\b(?:AKIA|ASIA)[A-Z0-9]{16}\b'),
    'google-api-key': re.compile(r'\bAIza[0-9A-Za-z_-]{35}\b'),
    'service-secret': re.compile(r'\b(?:sk-(?:proj-)?[A-Za-z0-9_-]{24,}|xox[baprs]-[A-Za-z0-9-]{20,})\b'),
    'authenticated-url': re.compile(r'https?://[^\s/:@]+:[^\s/@]+@'),
    'local-home-path': re.compile(r'(?:/Users/|/home/)[A-Za-z0-9._-]+/'),
    'hardware-address': re.compile(r'(?i)\b(?:[0-9a-f]{2}:){5}[0-9a-f]{2}\b'),
    'private-ipv4': re.compile(r'\b(?:10(?:\.\d{1,3}){3}|192\.168(?:\.\d{1,3}){2}|172\.(?:1[6-9]|2\d|3[01])(?:\.\d{1,3}){2})\b'),
    'coordinate-query': re.compile(r'(?:latitude|longitude)=-?\d+(?:\.\d+)?'),
}
# Documentation/test fixture addresses, never an actual device default.
EXAMPLE_IPS = {'192.168.1.50', '192.168.1.100', '192.168.1.10', '192.168.1.123', '192.168.1.42', '192.168.0.50', '192.168.0.100', '192.168.0.1', '192.168.1.1', '10.0.0.1', '10.0.0.2'}
baseline_path = ROOT / 'scripts/privacy-baseline.json'
baseline = json.loads(baseline_path.read_text()) if baseline_path.exists() else {}
findings = []
checked = 0
for path in sorted(ROOT.rglob('*')):
    rel = path.relative_to(ROOT)
    if any(p in SKIP for p in rel.parts):
        continue
    if path.is_symlink():
        findings.append((str(rel), 0, 'symlink'))
        continue
    if not path.is_file():
        continue
    if any(p in FORBIDDEN_DIRS for p in rel.parts) or path.suffix.lower() in FORBIDDEN_SUFFIXES or path.name.startswith(('adbkey', 'id_rsa', 'id_ed25519')) or path.name == 'local.properties' or (path.name.startswith('.env') and path.name != '.env.example'):
        findings.append((str(rel), 0, 'private-file'))
        continue
    checked += 1
    if path == Path(__file__).resolve():
        continue  # Detector definitions and fixture addresses are not credentials.
    if path.suffix.lower() in TEXT or path.name in {'LICENSE', 'gradlew', '.gitignore', '.gitattributes', '.editorconfig'}:
        try:
            text = path.read_text(encoding='utf-8')
        except UnicodeDecodeError:
            continue
        reviewed = baseline.get(str(rel), {})
        baseline_matches = reviewed.get('sha256') == hashlib.sha256(path.read_bytes()).hexdigest()
        for number, line in enumerate(text.splitlines(), 1):
            for label, pattern in RULES.items():
                for match in pattern.finditer(line):
                    if label == 'private-ipv4' and match.group() in EXAMPLE_IPS:
                        continue
                    if baseline_matches and label in reviewed.get('rules', []):
                        continue  # Exact original upstream fixtures, reviewed before publication.
                    findings.append((str(rel), number, label))
        if path.name == 'weather.xml' and re.search(r'<string name="weather_(?:latitude|longitude)"[^>]*>\s*[^\s<]',text):
            findings.append((str(rel), 0, 'configured-weather-location'))
    # Gallery JPEGs must be freshly encoded, with no EXIF/comment packets.
    if rel.parts[:2] == ('docs', 'media') and path.suffix == '.jpg':
        data = path.read_bytes()
        if b'Exif\x00\x00' in data or b'http://ns.adobe.com/xap/' in data or b'ICC_PROFILE' in data:
            findings.append((str(rel), 0, 'image-metadata'))
if findings:
    for path, line, rule in findings:
        print(f'{path}:{line}: {rule}')
    print(f'FAIL: {len(findings)} findings; values redacted; {checked} files inspected')
    sys.exit(1)
print(f'PASS: {checked} files checked; no configured weather coordinates, common secret patterns, private-file paths, or gallery metadata found')
