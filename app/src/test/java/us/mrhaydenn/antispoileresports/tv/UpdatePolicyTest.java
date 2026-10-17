package us.mrhaydenn.antispoileresports.tv;
import org.junit.Test;
import static org.junit.Assert.*;
public class UpdatePolicyTest {
 @Test public void releaseIsTheDefaultAndExperimentalIsOptIn(){assertEquals("release",UpdatePolicy.channel(null));assertEquals("release",UpdatePolicy.channel("other"));assertEquals("experimental",UpdatePolicy.channel("experimental"));}
 @Test public void updatesNeverDowngradeOrRepeat(){assertFalse(UpdatePolicy.newer(100,99));assertFalse(UpdatePolicy.newer(100,100));assertTrue(UpdatePolicy.newer(100,101));assertFalse(UpdatePolicy.newer(100,2100000001L));}
 @Test public void onlyThisRepositoryCanSupplyApks(){assertTrue(UpdatePolicy.trustedDownload(UpdatePolicy.EXPERIMENTAL_MANIFEST));assertTrue(UpdatePolicy.trustedDownload("https://github.com/"+UpdatePolicy.REPOSITORY+"/releases/download/v1/app.apk"));assertFalse(UpdatePolicy.trustedDownload("http://raw.githubusercontent.com/"+UpdatePolicy.REPOSITORY+"/updates/experimental/app.apk"));assertFalse(UpdatePolicy.trustedDownload("https://github.com/another/repo/releases/download/v1/app.apk"));assertFalse(UpdatePolicy.trustedDownload("https://github.com.evil.example/"+UpdatePolicy.REPOSITORY+"/releases/download/v1/app.apk"));assertFalse(UpdatePolicy.trustedDownload("https://github.com@evil.example/"+UpdatePolicy.REPOSITORY+"/releases/download/v1/app.apk"));}
 @Test public void redirectsStayOnGithubHttpsHosts(){assertTrue(UpdatePolicy.trustedRedirect("https://release-assets.githubusercontent.com/download"));assertFalse(UpdatePolicy.trustedRedirect("http://github.com/download"));assertFalse(UpdatePolicy.trustedRedirect("https://githubusercontent.com.evil.example/file"));}
}
