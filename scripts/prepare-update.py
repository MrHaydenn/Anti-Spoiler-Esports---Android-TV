import os,json,hashlib,shutil
from pathlib import Path
repo='MrHaydenn/Anti-Spoiler-Esports---Android-TV'
channel=os.environ['CHANNEL'];version=os.environ['VERSION_NAME'];code=int(os.environ['VERSION_CODE'])
if channel not in ('experimental','release'):raise SystemExit('Invalid channel')
source=Path('app/build/outputs/apk/release/app-release.apk')
if not source.is_file():raise SystemExit('Signed release APK missing')
output=Path('dist');output.mkdir(exist_ok=True)
name='anti-spoiler-esports-tv.apk';shutil.copyfile(source,output/name)
url=('https://raw.githubusercontent.com/'+repo+'/updates/experimental/'+name if channel=='experimental' else 'https://github.com/'+repo+'/releases/download/v'+version+'/'+name)
manifest={'schemaVersion':1,'packageName':'us.mrhaydenn.antispoileresports.tv','channel':channel,'versionCode':code,'versionName':version,'minSdk':26,'apkUrl':url,'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'commit':os.environ['GITHUB_SHA'],'sourceBranch':os.environ['GITHUB_REF_NAME']}
(output/'update.json').write_text(json.dumps(manifest,indent=2)+'\n')
print('Prepared '+channel+' update package. No GitHub Release was created.')
