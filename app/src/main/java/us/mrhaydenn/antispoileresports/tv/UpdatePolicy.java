package us.mrhaydenn.antispoileresports.tv;

import java.net.URI;

public final class UpdatePolicy {
    public static final String REPOSITORY="MrHaydenn/Anti-Spoiler-Esports---Android-TV";
    public static final String EXPERIMENTAL_MANIFEST="https://raw.githubusercontent.com/"+REPOSITORY+"/updates/experimental/update.json";
    public static String channel(String value){return "experimental".equals(value)?"experimental":"release";}
    public static boolean newer(long installed,long candidate){return candidate>installed&&candidate<=2100000000L;}
    public static boolean trustedDownload(String value){
        try{
            URI u=new URI(value);if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null)return false;
            return ("raw.githubusercontent.com".equals(u.getHost())&&u.getPath().startsWith("/"+REPOSITORY+"/updates/experimental/"))
                ||("github.com".equals(u.getHost())&&u.getPath().startsWith("/"+REPOSITORY+"/releases/download/"));
        }catch(Exception e){return false;}
    }
    public static boolean trustedRedirect(String value){
        try{URI u=new URI(value);String h=u.getHost();return "https".equals(u.getScheme())&&u.getUserInfo()==null&&h!=null&&(h.equals("api.github.com")||h.equals("github.com")||h.equals("raw.githubusercontent.com")||h.endsWith(".githubusercontent.com"));}catch(Exception e){return false;}
    }
    private UpdatePolicy(){}
}
