import os,re
channel=os.environ.get('INPUT_CHANNEL','experimental')
version=os.environ.get('INPUT_VERSION','0.1.0')
if channel not in ('release','experimental') or not re.fullmatch(r'\d+\.\d+\.\d+',version):
    raise SystemExit('Invalid build channel or version.')
if channel=='release' and (os.environ.get('GITHUB_EVENT_NAME')!='workflow_dispatch' or os.environ.get('GITHUB_REF_NAME')!='release'):
    raise SystemExit('A stable candidate requires manual dispatch on the release branch. This does not publish a GitHub Release.')
run=int(os.environ['GITHUB_RUN_NUMBER']);attempt=int(os.environ.get('GITHUB_RUN_ATTEMPT','1'))
if not 1<=attempt<=9:raise SystemExit('Start a new workflow instead of another rerun.')
code=100000+run*10+attempt
if code>2100000000:raise SystemExit('Android version code limit reached.')
print('channel='+channel)
print('version_code='+str(code))
print('version_name='+version+('' if channel=='release' else '-experimental.'+str(run)+'.'+str(attempt)))
