"""Regressions for self-satisfying dependencies and TOML table handling."""
from pathlib import Path
from tempfile import TemporaryDirectory
import json
import subprocess
import sys
import unittest
from zipfile import ZipFile
from fm_dependency_closure import audit


class DependencyClosureTests(unittest.TestCase):
    def setUp(self):
        self.temp = TemporaryDirectory(prefix='fm-metadata-')
        self.root = Path(self.temp.name)
        self.addCleanup(self.temp.cleanup)

    def jar(self, name, text=None):
        path = self.root / (name + '.jar')
        with ZipFile(path, 'w') as archive:
            archive.writestr('META-INF/neoforge.mods.toml' if text is not None else 'api.txt', text or 'thin library')
        return path

    def test_dependency_does_not_provide_itself(self):
        result = audit([self.jar('a', '[[mods]]\nmodId="a"\n[[dependencies.a]]\nmodId="missing"\ntype="required"')])
        self.assertFalse(result['passed'])
        self.assertNotIn('missing', result['providedMods'])
        self.assertIn('missing', result['errors'][0])

    def test_optional_and_legacy_false_are_not_required(self):
        result = audit([self.jar('a', '[[mods]]\nmodId="a"\n[[dependencies.a]]\nmodId="b"\ntype="optional"\n[[dependencies.a]]\nmodId="c"\nmandatory=false')])
        self.assertTrue(result['passed'], result['errors'])
        self.assertNotIn('b', result['providedMods'])
        self.assertNotIn('c', result['providedMods'])

    def test_missing_transitive_dependency_is_rejected(self):
        a = self.jar('a', '[[mods]]\nmodId="a"\n[[dependencies.a]]\nmodId="b"\nmandatory=true')
        b = self.jar('b', '[[mods]]\nmodId="b"\n[[dependencies.b]]\nmodId="c"\ntype="required"')
        self.assertFalse(audit([a,b])['passed'])
        self.assertTrue(audit([a,b,self.jar('c', '[[mods]]\nmodId="c"')])['passed'])

    def test_duplicate_ids_are_rejected(self):
        result = audit([self.jar('one','[[mods]]\nmodId="a"'),self.jar('two','[[mods]]\nmodId="a"')])
        self.assertFalse(result['passed'])
        self.assertIn('Duplicate', result['errors'][0])

    def test_multiple_mods_and_literal_strings(self):
        result = audit([self.jar('pair', "[[mods]] # first\nmodId='a'\n[[mods]]\nmodId='b'\n[[dependencies.a]]\nmodId='b'\ntype='required'")])
        self.assertTrue(result['passed'], result['errors'])
        self.assertEqual(set(result['declaredMods']), {'a','b'})

    def test_description_cannot_declare_a_fake_mod(self):
        text = "[[mods]]\nmodId='a'\ndescription='''\n[[mods]]\nmodId='fake'\n'''\n[[dependencies.a]]\nmodId='fake'\ntype='required'"
        result = audit([self.jar('a',text)])
        self.assertFalse(result['passed'])
        self.assertNotIn('fake', result['providedMods'])

    def test_invalid_toml_is_rejected(self):
        self.assertFalse(audit([self.jar('invalid','[[mods]')])['passed'])
        self.assertFalse(audit([self.jar('type','[[mods]]\nmodId="a"\n[[dependencies.a]]\nmodId="b"\ntype=[]')])['passed'])

    def test_cli_missing_dependency_exits_nonzero(self):
        self.jar('a','[[mods]]\nmodId="a"\n[[dependencies.a]]\nmodId="missing"\ntype="required"')
        process=subprocess.run([sys.executable,str(Path(__file__).with_name('fm_dependency_closure.py')),'--mods-dir',str(self.root)],capture_output=True,text=True)
        self.assertEqual(process.returncode,1,process.stderr)
        self.assertFalse(json.loads(process.stdout)['passed'])

    def test_invalid_zip_is_rejected(self):
        path = self.root / 'broken.jar'
        path.write_text('not a zip')
        self.assertFalse(audit([path])['passed'])

    def test_incompatible_present_mod_is_rejected(self):
        text = '[[mods]]\nmodId="a"\n[[dependencies.a]]\nmodId="b"\ntype="incompatible"'
        self.assertFalse(audit([self.jar('a',text),self.jar('b','[[mods]]\nmodId="b"')])['passed'])

    def test_platform_dependencies_and_thin_library(self):
        text = '[[mods]]\nmodId="a"\n[[dependencies.a]]\nmodId="neoforge"\ntype="REQUIRED"'
        self.assertTrue(audit([self.jar('a',text),self.jar('api')])['passed'])

    def test_legacy_mandatory_true_and_invalid_boolean(self):
        text = '[[mods]]\nmodId="a"\n[[dependencies.a]]\nmodId="b"\nmandatory=true'
        self.assertFalse(audit([self.jar('a',text)])['passed'])
        self.assertFalse(audit([self.jar('bad',text.replace('true','"true"'))])['passed'])


if __name__ == '__main__':
    unittest.main()
