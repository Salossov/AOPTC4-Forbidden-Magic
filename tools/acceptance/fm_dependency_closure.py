"""Audit declared NeoForge mod IDs and required dependency presence using real TOML tables.

Python 3.11+ is required for the standard-library TOML parser. Version ranges are
enforced by NeoForge during live acceptance, not by this staging presence audit.
"""
import argparse
import json
from pathlib import Path
import sys
import tomllib
from zipfile import BadZipFile, ZipFile

PLATFORM = frozenset({'minecraft', 'neoforge', 'forge', 'java'})


def audit(jars):
    declared = {}
    requirements = []
    errors = []
    for path in sorted(map(Path, jars)):
        try:
            with ZipFile(path) as archive:
                name = 'META-INF/neoforge.mods.toml'
                if name not in archive.namelist():
                    continue  # Thin API/library JARs do not declare loader mod IDs.
                metadata = tomllib.loads(archive.read(name).decode('utf-8-sig'))
            mods = metadata.get('mods', [])
            if not isinstance(mods, list):
                raise ValueError('mods must be an array of tables')
            for mod in mods:
                mod_id = mod.get('modId')
                if not isinstance(mod_id, str) or not mod_id:
                    raise ValueError('a [[mods]] declaration has no modId')
                if mod_id in declared or mod_id in PLATFORM:
                    errors.append(f'Duplicate mod ID {mod_id!r}: {declared.get(mod_id, "platform")} and {path.name}')
                else:
                    declared[mod_id] = path.name
            dependencies = metadata.get('dependencies', {})
            if not isinstance(dependencies, dict):
                raise ValueError('dependencies must be a table')
            for owner, entries in dependencies.items():
                if not isinstance(entries, list):
                    raise ValueError(f'dependencies.{owner} must be an array of tables')
                for dependency in entries:
                    target = dependency.get('modId')
                    if not isinstance(target, str) or not target:
                        raise ValueError(f'dependencies.{owner} has no modId')
                    kind = dependency.get('type')
                    if kind is None:
                        mandatory = dependency.get('mandatory', True)
                        if not isinstance(mandatory, bool):
                            raise ValueError('mandatory must be boolean')
                        kind = 'required' if mandatory else 'optional'
                    elif isinstance(kind, str):
                        kind = kind.lower()
                    if not isinstance(kind, str) or kind not in {'required', 'optional', 'incompatible', 'discouraged'}:
                        raise ValueError(f'Unknown dependency type {kind!r}')
                    requirements.append(dict(jar=path.name, owner=owner, modId=target, type=kind))
        except (BadZipFile, OSError, UnicodeError, ValueError, AttributeError) as error:
            errors.append(f'{path.name}: {error}')
    provided = set(declared) | PLATFORM
    for dependency in requirements:
        present = dependency['modId'] in provided
        dependency['present'] = present
        if dependency['type'] == 'required' and not present:
            errors.append(f"{dependency['jar']} requires missing mod {dependency['modId']!r}")
        elif dependency['type'] == 'incompatible' and present:
            errors.append(f"{dependency['jar']} conflicts with present mod {dependency['modId']!r}")
    return dict(passed=not errors, providedMods=sorted(provided), declaredMods=declared,
                dependencies=requirements, errors=errors,
                scope='Declared mod IDs and dependency presence; version ranges require NeoForge runtime')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mods-dir', type=Path, required=True)
    args = parser.parse_args()
    jars = list(args.mods_dir.glob('*.jar'))
    if not jars:
        parser.error('No staged JARs found')
    report = audit(jars)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if report['passed'] else 1


if __name__ == '__main__':
    sys.exit(main())
