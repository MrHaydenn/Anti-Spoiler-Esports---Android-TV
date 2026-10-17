import unittest,os,sys,subprocess,tempfile,json,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
class BuildRules(unittest.TestCase):
    def metadata(self,**changes):
        env={**os.environ,'GITHUB_RUN_NUMBER':'12','GITHUB_RUN_ATTEMPT':'1','INPUT_CHANNEL':'experimental','INPUT_VERSION':'0.1.0','GITHUB_EVENT_NAME':'push','GITHUB_REF_NAME':'main',**changes}
        return subprocess.run([sys.executable,str(ROOT/'scripts/build-metadata.py')],env=env,capture_output=True,text=True)
    def test_pushes_are_experimental_and_versions_increase(self):
        first=self.metadata();second=self.metadata(GITHUB_RUN_ATTEMPT='2')
        self.assertEqual(first.returncode,0);self.assertIn('channel=experimental',first.stdout)
        a=dict(line.split('=',1) for line in first.stdout.splitlines());b=dict(line.split('=',1) for line in second.stdout.splitlines())
        self.assertGreater(int(b['version_code']),int(a['version_code']))
    def test_release_candidate_requires_manual_release_branch(self):
        self.assertNotEqual(self.metadata(INPUT_CHANNEL='release').returncode,0)
        self.assertNotEqual(self.metadata(INPUT_CHANNEL='release',GITHUB_EVENT_NAME='workflow_dispatch').returncode,0)
        self.assertEqual(self.metadata(INPUT_CHANNEL='release',GITHUB_EVENT_NAME='workflow_dispatch',GITHUB_REF_NAME='release').returncode,0)
    def test_untrusted_version_input_is_rejected(self):
        self.assertNotEqual(self.metadata(INPUT_VERSION='0.1.0; echo wrong').returncode,0)
    def test_manifest_has_exact_apk_hash_and_matching_channel(self):
        for channel in ['release','experimental']:
            with tempfile.TemporaryDirectory(prefix='ase-build-test-') as folder:
                root=Path(folder);apk=root/'app/build/outputs/apk/release/app-release.apk';apk.parent.mkdir(parents=True);apk.write_bytes(b'unit-test-fixture-not-an-installable-apk')
                env={**os.environ,'CHANNEL':channel,'VERSION_NAME':'0.1.0','VERSION_CODE':'100121','GITHUB_SHA':'a'*40,'GITHUB_REF_NAME':'release' if channel=='release' else 'main'}
                run=subprocess.run([sys.executable,str(ROOT/'scripts/prepare-update.py')],cwd=root,env=env,capture_output=True,text=True)
                self.assertEqual(run.returncode,0,run.stderr)
                manifest=json.loads((root/'dist/update.json').read_text())
                self.assertEqual(manifest['sha256'],hashlib.sha256(apk.read_bytes()).hexdigest());self.assertEqual(manifest['channel'],channel)
                self.assertIn('/releases/download/v0.1.0/' if channel=='release' else '/updates/experimental/',manifest['apkUrl'])
    def test_publisher_never_calls_release_or_tag_endpoints(self):
        text=(ROOT/'scripts/publish-experimental.py').read_text()
        self.assertNotIn("api('/releases",text);self.assertNotIn("refs/tags",text)
if __name__=='__main__':unittest.main()
