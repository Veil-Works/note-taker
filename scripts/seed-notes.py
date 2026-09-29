#!/usr/bin/env python3
"""Add repeatable, untagged sample notes to the dedicated debug emulator.

The app is stopped before taking a database/WAL backup. All edits happen on a
SQLite backup copy, then the stopped app's database is replaced atomically.
Existing notes are preserved; stable sample IDs make repeated seeding idempotent.
Run through ./scripts/dev seed so the Android environment is configured.
"""
import argparse
import datetime
import json
import os
from pathlib import Path
import sqlite3
import subprocess
import uuid

ROOT = Path(__file__).resolve().parent.parent
PACKAGE = 'app.notetaker'
SERIAL = os.environ.get('ANDROID_SERIAL', 'emulator-5554')
AVD = os.environ.get('AVD_NAME', 'NoteTaker_API_36')
ADB = str(Path(os.environ['ANDROID_HOME']) / 'platform-tools' / 'adb')


def adb(*args, **kwargs):
    return subprocess.run([ADB, '-s', SERIAL, *args], check=True, **kwargs)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fixture', type=Path, default=ROOT / 'fixtures/sample-notes.json')
    args = parser.parse_args()
    actual = adb('emu', 'avd', 'name', capture_output=True, text=True).stdout.splitlines()[0]
    if actual != AVD:
        raise SystemExit(f'Expected {AVD}, found {actual}; refusing to seed another device.')
    notes = json.loads(args.fixture.read_text())
    assert notes and all(isinstance(note, str) and note.strip() for note in notes)
    stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    backup_dir = ROOT / '.local' / 'backups' / f'before-sample-notes-{stamp}'
    backup_dir.mkdir(parents=True)
    adb('shell', 'am', 'force-stop', PACKAGE)
    present = adb('shell', 'run-as', PACKAGE, 'ls', 'databases', capture_output=True, text=True).stdout.splitlines()
    if 'notes.db' not in present:
        raise SystemExit('Launch the app once before seeding so its database exists.')
    for name in ('notes.db', 'notes.db-wal', 'notes.db-shm'):
        if name in present:
            with (backup_dir / name).open('wb') as output:
                adb('exec-out', 'run-as', PACKAGE, 'cat', f'databases/{name}', stdout=output)
    # Open a working copy so the original backup remains byte-for-byte intact.
    working_dir = backup_dir / 'working'
    working_dir.mkdir()
    for name in ('notes.db', 'notes.db-wal'):
        original = backup_dir / name
        if original.exists():
            (working_dir / name).write_bytes(original.read_bytes())
    seeded_path = backup_dir / 'seeded.db'
    with sqlite3.connect(working_dir / 'notes.db') as original, sqlite3.connect(seeded_path) as seeded:
        original.backup(seeded)
        if seeded.execute('PRAGMA user_version').fetchone()[0] != 7:
            raise SystemExit('Expected Room schema 7. Install and launch the current debug app first.')
        before = seeded.execute('SELECT COUNT(*) FROM notes').fetchone()[0]
        now_ms = int(datetime.datetime.now(datetime.timezone.utc).timestamp() * 1000)
        namespace = uuid.UUID('2e73e9b1-28eb-4ff1-bb40-7873d608f4d2')
        # Spread samples across three weeks; group input order is intentionally mixed in time.
        with seeded:
            for index, content in enumerate(notes):
                age_hours = ((index * 137) % (21 * 24))
                timestamp = now_ms - age_hours * 60 * 60 * 1000
                seeded.execute('INSERT OR IGNORE INTO notes (id, content, createdAt, updatedAt) VALUES (?, ?, ?, ?)',
                               (str(uuid.uuid5(namespace, content)), content, timestamp, timestamp))
            seeded.execute('UPDATE organization_state SET generation = generation + 1, error = NULL WHERE id = 1')
        after = seeded.execute('SELECT COUNT(*) FROM notes').fetchone()[0]
        assert seeded.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
        assert not seeded.execute('PRAGMA foreign_key_check').fetchall()
        seeded.execute('PRAGMA wal_checkpoint(TRUNCATE)')
        seeded.execute('PRAGMA journal_mode=DELETE')
    remote = f'/data/local/tmp/notetaker-seed-{stamp}.db'
    adb('push', str(seeded_path), remote)
    adb('shell', 'chmod', '644', remote)
    # Keep the original database files together on the emulator as another backup.
    # All names below are fixed or generated from digits, never note content.
    old_dir = f'files/before-sample-notes-{stamp}'
    command = (
        f'cp {remote} databases/notes.seeded.db && mkdir -p {old_dir} && '
        f'for name in notes.db notes.db-wal notes.db-shm; do '
        f'if [ -f databases/$name ]; then mv databases/$name {old_dir}/$name || exit 1; fi; done && '
        'mv databases/notes.seeded.db databases/notes.db'
    )
    adb('shell', 'run-as', PACKAGE, 'sh', '-c', "'" + command + "'")
    adb('shell', 'am', 'start', '-W', '-n', f'{PACKAGE}/.MainActivity')
    print(f'Added {after - before} sample notes; notebook now contains {after} notes.')
    print(f'Original database backup: {backup_dir}')
    print('No topics were assigned manually; automatic organization will run in the app.')


if __name__ == '__main__':
    main()
