package us.mrhaydenn.antispoileresports.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView web;
    private AppUpdater updater;
    private final android.os.Handler updateHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable updateTick=new Runnable(){public void run(){if(updater!=null)updater.onResume();updateHandler.postDelayed(this,15*60000L);}};
    private FrameLayout root;
    private LinearLayout status;
    private TextView statusText;
    private String serverUrl;
    private boolean failed, nativeDialog, backHeld;

    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        serverUrl = getPreferences(MODE_PRIVATE).getString("server_url", BuildConfig.DEFAULT_SERVER_URL);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(21,21,23));
        setContentView(root);
        try { web = new WebView(this); }
        catch (RuntimeException error) {
            TextView unavailable = new TextView(this);
            unavailable.setText("Android System WebView is unavailable. Update the TV system and Android System WebView, then reopen this app.");
            unavailable.setTextColor(Color.WHITE); unavailable.setTextSize(22); unavailable.setPadding(48,48,48,48);
            root.addView(unavailable); return;
        }
        root.addView(web, new FrameLayout.LayoutParams(-1,-1));
        web.setBackgroundColor(Color.rgb(21,21,23));
        web.setFocusable(true); web.setFocusableInTouchMode(true);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);
        settings.setUserAgentString(settings.getUserAgentString() + " AntiSpoilerEsportsTV/0.1");
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                if (sameOrigin(request.getUrl(), Uri.parse(serverUrl))) return false;
                Toast.makeText(MainActivity.this, "Use the in-app player. Hold Back to change server settings.", Toast.LENGTH_LONG).show();
                return true;
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                failed = false; statusText.setText("Connecting to Anti-Spoiler Esports..."); status.setVisibility(View.VISIBLE);
            }
            @Override public void onPageFinished(WebView view, String url) {
                if (!failed) {
                    status.setVisibility(View.GONE); web.requestFocus();
                    web.evaluateJavascript("window.ASE_TV && window.ASE_TV.focusInitial()", null);
                }
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showError("The server could not be reached. Check the address, network connection, and whether the server is running.");
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                if (request.isForMainFrame()) showError("The server returned HTTP " + response.getStatusCode() + ". Check the reverse proxy and server.");
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                showError("The HTTPS certificate is not ready or is invalid. Wait for the certificate to become active, then retry. The app will not bypass certificate errors.");
            }
        });
        updater=new AppUpdater(this);
        createStatusPanel();
        loadSite();
    }

    private static int port(Uri uri) { return uri.getPort() == -1 ? ("https".equals(uri.getScheme()) ? 443 : 80) : uri.getPort(); }
    private static boolean sameOrigin(Uri a, Uri b) {
        return a.getHost() != null && a.getHost().equalsIgnoreCase(b.getHost()) && a.getScheme().equalsIgnoreCase(b.getScheme()) && port(a) == port(b);
    }
    private void createStatusPanel() {
        status = new LinearLayout(this); status.setOrientation(LinearLayout.VERTICAL);
        status.setGravity(android.view.Gravity.CENTER); status.setPadding(48,32,48,32);
        status.setBackgroundColor(Color.rgb(21,21,23));
        TextView title = new TextView(this); title.setText("Anti-Spoiler Esports"); title.setTextSize(28); title.setTextColor(Color.rgb(185,145,255)); status.addView(title);
        statusText = new TextView(this); statusText.setTextSize(19); statusText.setTextColor(Color.WHITE); statusText.setPadding(0,20,0,24); status.addView(statusText);
        Button retry = new Button(this); retry.setText("Retry connection"); retry.setOnClickListener(v -> loadSite()); status.addView(retry);
        Button connection = new Button(this); connection.setText("Server settings"); connection.setOnClickListener(v -> connectionSettings()); status.addView(connection);
        Button updates=new Button(this);updates.setText("App updates");updates.setOnClickListener(v->updater.showSettings());status.addView(updates);
        root.addView(status, new FrameLayout.LayoutParams(-1,-1));
    }
    private void showError(String message) { failed=true; statusText.setText(message); status.setVisibility(View.VISIBLE); status.getChildAt(2).requestFocus(); }
    private void loadSite() { if(web != null) { failed=false; status.setVisibility(View.VISIBLE); web.loadUrl(serverUrl + "/tv"); } }

    private void connectionSettings() {
        EditText address = new EditText(this); address.setSingleLine(true); address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI); address.setText(serverUrl); address.selectAll();
        String help = "Public HTTPS server address. Hold Back while browsing to reopen these settings." + (BuildConfig.DEBUG ? " Debug builds also allow http://YOUR-SERVER-IP:10117 for LAN testing." : "");
        nativeDialog=true;
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Server connection").setMessage(help).setView(address)
            .setPositiveButton("Save and connect",null).setNegativeButton("Cancel",(d,w)->{}).setNeutralButton("App updates",(d,w)->updateHandler.post(()->updater.showSettings())).create();
        dialog.setOnDismissListener(d->{nativeDialog=false;if(web!=null)web.requestFocus();});
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String value=address.getText().toString().trim().replaceAll("/+$","");
            if(value.endsWith("/tv"))value=value.substring(0,value.length()-3);
            Uri uri=Uri.parse(value);
            boolean scheme="https".equalsIgnoreCase(uri.getScheme()) || (BuildConfig.DEBUG && "http".equalsIgnoreCase(uri.getScheme()));
            if(!scheme || uri.getHost()==null || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null || (uri.getPath()!=null && !uri.getPath().isEmpty())){
                address.setError("Enter a server origin such as https://antispoileresports.mrhaydenn.us"); return;
            }
            serverUrl=value;getPreferences(MODE_PRIVATE).edit().putString("server_url",value).apply();dialog.dismiss();loadSite();
        }));
        dialog.show();
    }

    public boolean canShowUpdate(){return !isFinishing()&&!isDestroyed()&&!nativeDialog&&hasWindowFocus();}
    public void showManagedDialog(AlertDialog dialog){nativeDialog=true;dialog.setOnDismissListener(d->{nativeDialog=false;if(web!=null)web.requestFocus();});dialog.show();}

    private void exitPrompt() {
        nativeDialog=true;
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Exit Anti-Spoiler Esports?")
            .setPositiveButton("Exit",(d,w)->finish()).setNegativeButton("Stay",(d,w)->{}).create();
        dialog.setOnDismissListener(d->{nativeDialog=false;if(web!=null)web.requestFocus();});dialog.show();dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
    }
    private String remoteKey(int code) {
        switch(code){
            case KeyEvent.KEYCODE_DPAD_LEFT:return "ArrowLeft";
            case KeyEvent.KEYCODE_DPAD_RIGHT:return "ArrowRight";
            case KeyEvent.KEYCODE_DPAD_UP:return "ArrowUp";
            case KeyEvent.KEYCODE_DPAD_DOWN:return "ArrowDown";
            case KeyEvent.KEYCODE_DPAD_CENTER:case KeyEvent.KEYCODE_ENTER:return "Enter";
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:return "MediaPlayPause";
            case KeyEvent.KEYCODE_MEDIA_PLAY:return "MediaPlay";
            case KeyEvent.KEYCODE_MEDIA_PAUSE:return "MediaPause";
            case KeyEvent.KEYCODE_MEDIA_REWIND:return "MediaRewind";
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:return "MediaFastForward";
            default:return null;
        }
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int code=event.getKeyCode();
        if(code==KeyEvent.KEYCODE_BACK && event.getAction()==KeyEvent.ACTION_UP && backHeld){backHeld=false;return true;}
        if(web==null || nativeDialog) return super.dispatchKeyEvent(event);
        if(code==KeyEvent.KEYCODE_MENU){if(event.getAction()==KeyEvent.ACTION_UP)connectionSettings();return true;}
        if(code==KeyEvent.KEYCODE_BACK){
            if(event.getAction()==KeyEvent.ACTION_DOWN && event.getRepeatCount()==1){backHeld=true;connectionSettings();return true;}
            if(event.getAction()==KeyEvent.ACTION_UP && !event.isCanceled()){
                if(status.getVisibility()==View.VISIBLE){exitPrompt();return true;}
                web.evaluateJavascript("!!(window.ASE_TV && window.ASE_TV.handleKey('Back'))",result->{if(!"true".equals(result))exitPrompt();});
            }
            return true;
        }
        String key=remoteKey(code);
        if(key==null || status.getVisibility()==View.VISIBLE || !web.hasFocus())return super.dispatchKeyEvent(event);
        if(event.getAction()==KeyEvent.ACTION_DOWN){
            final KeyEvent original=new KeyEvent(event);
            web.evaluateJavascript("!!(window.ASE_TV && window.ASE_TV.handleKey("+JSONObject.quote(key)+"))",result->{
                if(!"true".equals(result)){web.dispatchKeyEvent(original);web.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,code));}
            });
        }
        return true;
    }
    @Override protected void onPause(){updateHandler.removeCallbacks(updateTick);if(web!=null){web.evaluateJavascript("window.ASE_TV && window.ASE_TV.setActive(false)",null);web.onPause();}super.onPause();}
    @Override protected void onResume(){super.onResume();updateHandler.removeCallbacks(updateTick);updateHandler.postDelayed(updateTick,1500);if(updater!=null)updater.onResume();if(web!=null){web.onResume();web.evaluateJavascript("window.ASE_TV && window.ASE_TV.setActive(true)",null);}}
    @Override protected void onDestroy(){updateHandler.removeCallbacks(updateTick);if(updater!=null)updater.close();if(web!=null){web.stopLoading();root.removeView(web);web.destroy();}super.onDestroy();}
}
