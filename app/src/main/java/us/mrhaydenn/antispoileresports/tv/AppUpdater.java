package us.mrhaydenn.antispoileresports.tv;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AppUpdater {
    private final MainActivity activity;
    private final SharedPreferences prefs;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy=new AtomicBoolean(false);
    private File pendingInstall;
    private boolean permissionRequested;
    public AppUpdater(MainActivity activity){this.activity=activity;prefs=activity.getSharedPreferences("app_updates",0);}
    private String channel(){return UpdatePolicy.channel(prefs.getString("channel","release"));}
    private void ui(Runnable action){activity.runOnUiThread(()->{if(!activity.isFinishing()&&!activity.isDestroyed())action.run();});}
    private void toast(String text){Toast.makeText(activity,text,Toast.LENGTH_LONG).show();}

    public void showSettings(){
        LinearLayout layout=new LinearLayout(activity);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(32,16,32,8);
        TextView version=new TextView(activity);version.setText("Installed: "+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")\nRelease receives only published GitHub Releases. Experimental receives successful push builds.");version.setTextSize(16);layout.addView(version);
        RadioGroup group=new RadioGroup(activity);
        RadioButton release=new RadioButton(activity);release.setId(1001);release.setText("Release (default)");group.addView(release);
        RadioButton experimental=new RadioButton(activity);experimental.setId(1002);experimental.setText("Experimental — latest push build");group.addView(experimental);
        group.check(channel().equals("release")?1001:1002);layout.addView(group);
        CheckBox automatic=new CheckBox(activity);automatic.setText("Automatically check for updates");automatic.setChecked(prefs.getBoolean("automatic",true));layout.addView(automatic);
        group.setOnCheckedChangeListener((g,id)->{prefs.edit().putString("channel",id==1002?"experimental":"release").apply();pendingInstall=null;});
        automatic.setOnCheckedChangeListener((button,checked)->prefs.edit().putBoolean("automatic",checked).apply());
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("App updates").setView(layout)
            .setPositiveButton("Check now",(d,w)->check(true)).setNegativeButton("Close",(d,w)->{}).setNeutralButton("Install downloaded",(d,w)->{permissionRequested=false;installPending();}).create();
        activity.showManagedDialog(dialog);dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(pendingInstall!=null);
    }

    public void check(boolean manual){
        if(!manual&&(!prefs.getBoolean("automatic",true)||!activity.canShowUpdate()))return;
        String selected=channel();long interval=selected.equals("experimental")?15*60000L:6*3600000L;
        if(!manual&&System.currentTimeMillis()-prefs.getLong("last_check_"+selected,0)<interval)return;
        if(!busy.compareAndSet(false,true)){if(manual)toast("An update check or download is already running.");return;}
        prefs.edit().putLong("last_check_"+selected,System.currentTimeMillis()).apply();
        if(manual)toast("Checking GitHub for "+selected+" updates...");
        worker.execute(()->{
            try{
                JSONObject manifest=fetchManifest(selected);
                Candidate candidate=manifest==null?null:new Candidate(manifest,selected);
                ui(()->{
                    if(!selected.equals(channel()))return;
                    if(candidate==null){if(manual)toast(selected.equals("release")?"No release has been published yet.":"No experimental build is available yet.");return;}
                    if(!UpdatePolicy.newer(BuildConfig.VERSION_CODE,candidate.code)){if(manual)toast("No newer "+selected+" update is available.");return;}
                    if(!activity.canShowUpdate()){prefs.edit().remove("last_check_"+selected).apply();return;}
                    AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Update available")
                        .setMessage(candidate.name+"\nChannel: "+selected+"\n\nDownload and install this update? Android will ask you to approve installation.")
                        .setPositiveButton("Download update",(d,w)->download(candidate)).setNegativeButton("Later",(d,w)->{}).create();
                    activity.showManagedDialog(dialog);
                });
            }catch(Exception error){if(manual)ui(()->toast("Could not check for updates: "+safeMessage(error)));}
            finally{busy.set(false);}
        });
    }

    private JSONObject fetchManifest(String selected)throws Exception{
        String url=UpdatePolicy.EXPERIMENTAL_MANIFEST;
        if(selected.equals("release")){
            JSONObject release=getJson("https://api.github.com/repos/"+UpdatePolicy.REPOSITORY+"/releases/latest",true);
            if(release==null)return null;
            if(release.getBoolean("draft")||release.getBoolean("prerelease"))return null;
            JSONArray assets=release.getJSONArray("assets");url=null;
            for(int i=0;i<assets.length();i++){JSONObject asset=assets.getJSONObject(i);if("update.json".equals(asset.getString("name")))url=asset.getString("browser_download_url");}
            if(url==null)throw new IOException("The published release is missing update.json.");
            if(!UpdatePolicy.trustedDownload(url))throw new IOException("Invalid release manifest address.");
        }
        return getJson(url,true);
    }
    private HttpURLConnection open(String address)throws IOException{
        for(int redirects=0;redirects<6;redirects++){
            if(!UpdatePolicy.trustedRedirect(address))throw new IOException("Untrusted update address.");
            HttpURLConnection connection=(HttpURLConnection)new URL(address).openConnection();
            connection.setConnectTimeout(20000);connection.setReadTimeout(30000);connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent","AntiSpoilerEsportsTV/"+BuildConfig.VERSION_NAME);
            connection.setRequestProperty("Accept","application/vnd.github+json");
            int status=connection.getResponseCode();
            if(status>=300&&status<400){String location=connection.getHeaderField("Location");connection.disconnect();if(location==null)throw new IOException("Invalid update redirect.");address=new URL(new URL(address),location).toString();continue;}
            return connection;
        }
        throw new IOException("Too many update redirects.");
    }
    private JSONObject getJson(String address,boolean missingIsEmpty)throws Exception{
        HttpURLConnection connection=open(address);
        try{
            int status=connection.getResponseCode();if(status==404&&missingIsEmpty)return null;
            if(status!=200)throw new IOException(status==403||status==429?"GitHub is temporarily rate limited. Try again later.":"GitHub returned HTTP "+status+".");
            ByteArrayOutputStream buffer=new ByteArrayOutputStream();
            try(InputStream in=connection.getInputStream()){byte[] chunk=new byte[8192];int n;while((n=in.read(chunk))!=-1){if(buffer.size()+n>262144)throw new IOException("Update metadata is too large.");buffer.write(chunk,0,n);}}
            return new JSONObject(buffer.toString(StandardCharsets.UTF_8.name()));
        }finally{connection.disconnect();}
    }

    private final class Candidate {
        final int code;final String name,url,checksum,selected;
        Candidate(JSONObject json,String selected)throws Exception{
            if(json.getInt("schemaVersion")!=1||!activity.getPackageName().equals(json.getString("packageName"))||!selected.equals(json.getString("channel")))throw new IOException("Update metadata does not match this app/channel.");
            if(selected.equals("release")&&!"release".equals(json.getString("sourceBranch")))throw new IOException("Release update was not built from the release branch.");
            code=json.getInt("versionCode");name=json.getString("versionName");url=json.getString("apkUrl");checksum=json.getString("sha256").toLowerCase();this.selected=selected;
            if(code<=0||code>2100000000||name.length()>100||!checksum.matches("[a-f0-9]{64}")||!UpdatePolicy.trustedDownload(url))throw new IOException("Invalid update metadata.");
            if(json.getInt("minSdk")>Build.VERSION.SDK_INT)throw new IOException("This update needs a newer Android version.");
        }
    }
    private void download(Candidate candidate){
        if(!candidate.selected.equals(channel())||!busy.compareAndSet(false,true))return;
        toast("Downloading update...");
        worker.execute(()->{
            File folder=new File(activity.getCacheDir(),"updates"),temporary=new File(folder,"update.apk.part");
            try{
                if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Cannot create update cache.");
                HttpURLConnection connection=open(candidate.url);
                MessageDigest digest=MessageDigest.getInstance("SHA-256");long size=0;
                try{
                    if(connection.getResponseCode()!=200)throw new IOException("APK download failed.");
                    try(InputStream in=connection.getInputStream();OutputStream out=new FileOutputStream(temporary)){
                        byte[] chunk=new byte[32768];int n;while((n=in.read(chunk))!=-1){size+=n;if(size>80*1024*1024)throw new IOException("APK exceeds the update size limit.");digest.update(chunk,0,n);out.write(chunk,0,n);}
                    }
                }finally{connection.disconnect();}
                StringBuilder actual=new StringBuilder();for(byte b:digest.digest())actual.append(String.format("%02x",b&255));
                if(!actual.toString().equals(candidate.checksum))throw new IOException("APK checksum verification failed.");
                verifyPackage(temporary,candidate.code);
                File apk=new File(folder,"update.apk");if(apk.exists()&&!apk.delete())throw new IOException("Cannot replace cached update.");if(!temporary.renameTo(apk))throw new IOException("Cannot save verified update.");
                ui(()->{if(candidate.selected.equals(channel())){pendingInstall=apk;permissionRequested=false;if(activity.canShowUpdate())installPending();}});
            }catch(Exception error){temporary.delete();ui(()->toast("Update not installed: "+safeMessage(error)));}
            finally{busy.set(false);}
        });
    }
    private void verifyPackage(File file,int expectedCode)throws Exception{
        PackageManager manager=activity.getPackageManager();int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
        PackageInfo downloaded=manager.getPackageArchiveInfo(file.getAbsolutePath(),flags),installed=manager.getPackageInfo(activity.getPackageName(),flags);
        if(downloaded==null||!activity.getPackageName().equals(downloaded.packageName))throw new IOException("Downloaded APK is not this app.");
        long version=Build.VERSION.SDK_INT>=28?downloaded.getLongVersionCode():downloaded.versionCode;
        if(version!=expectedCode||version<=BuildConfig.VERSION_CODE)throw new IOException("APK version does not match the update.");
        Signature[] a=Build.VERSION.SDK_INT>=28&&downloaded.signingInfo!=null?downloaded.signingInfo.getApkContentsSigners():downloaded.signatures;
        Signature[] b=Build.VERSION.SDK_INT>=28&&installed.signingInfo!=null?installed.signingInfo.getApkContentsSigners():installed.signatures;
        if(a==null||b==null||a.length==0||!new HashSet<>(Arrays.asList(a)).equals(new HashSet<>(Arrays.asList(b))))throw new IOException("APK signing certificate differs from the installed app. Install the official GitHub build first.");
    }
    private void installPending(){
        if(pendingInstall==null||!pendingInstall.isFile())return;
        if(!activity.getPackageManager().canRequestPackageInstalls()){
            permissionRequested=true;
            AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Allow app updates")
                .setMessage("Allow Anti-Spoiler Esports to install its downloaded updates. Android will still ask you to confirm each installation.")
                .setPositiveButton("Open Android settings",(d,w)->{try{activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())));}catch(Exception e){toast("Open Android settings and allow installations from Anti-Spoiler Esports.");}})
                .setNegativeButton("Later",(d,w)->{}).create();activity.showManagedDialog(dialog);return;
        }
        Uri uri=Uri.parse("content://"+activity.getPackageName()+".updates/update.apk");
        Intent install=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pendingInstall=null;
        try{activity.startActivity(install);}catch(Exception e){toast("Android could not open its package installer.");}
    }
    public void onResume(){if(pendingInstall!=null&&activity.canShowUpdate()&&(activity.getPackageManager().canRequestPackageInstalls()||!permissionRequested))installPending();else check(false);}
    public void close(){worker.shutdownNow();}
    private static String safeMessage(Exception error){String message=error.getMessage();return message==null?"Please try again later.":message;}
}
