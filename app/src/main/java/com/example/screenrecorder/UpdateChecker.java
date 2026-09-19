package com.example.screenrecorder;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public final class UpdateChecker {
 private static final String API="https://api.github.com/repos/almosheqh100-glitch/screen-recorder-android/releases/latest";
 private static final String RELEASES="https://github.com/almosheqh100-glitch/screen-recorder-android/releases/latest";
 public static void check(Activity activity, boolean userRequested){
  new Thread(()->{
   try{
    HttpURLConnection c=(HttpURLConnection)new URL(API).openConnection();
    c.setRequestProperty("Accept","application/vnd.github+json");c.setRequestProperty("User-Agent","ScreenRecorder-Android");c.setConnectTimeout(8000);c.setReadTimeout(8000);
    if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());
    String json=read(c.getInputStream());JSONObject release=new JSONObject(json);String tag=release.getString("tag_name");int latest=parse(tag);long current=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).getLongVersionCode();String download=apkUrl(release);
    new Handler(Looper.getMainLooper()).post(()->{if(latest>current)show(activity,release.optString("name",tag),download);else if(userRequested)toast(activity,"لديك أحدث إصدار");});
   }catch(Exception e){if(userRequested)new Handler(Looper.getMainLooper()).post(()->toast(activity,"تعذر التحقق من التحديث الآن"));}
  }).start();
 }
 private static String apkUrl(JSONObject release){JSONArray assets=release.optJSONArray("assets");if(assets!=null)for(int i=0;i<assets.length();i++){JSONObject asset=assets.optJSONObject(i);if(asset!=null && "app-release.apk".equals(asset.optString("name")))return asset.optString("browser_download_url",RELEASES);}return release.optString("html_url",RELEASES);}
 private static int parse(String tag){String digits=tag.replaceAll("[^0-9]","");return digits.isEmpty()?0:Integer.parseInt(digits);}
 private static String read(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];for(int n;(n=in.read(b))!=-1;)out.write(b,0,n);return out.toString(StandardCharsets.UTF_8.name());}
 private static void show(Activity a,String version,String url){new AlertDialog.Builder(a).setTitle("يتوفر تحديث جديد").setMessage("الإصدار "+version+" متاح الآن. هل تريد تنزيله؟").setNegativeButton("لاحقًا",null).setPositiveButton("تنزيل",(d,w)->a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)))).show();}
 private static void toast(Context c,String s){android.widget.Toast.makeText(c,s,android.widget.Toast.LENGTH_SHORT).show();}
 private UpdateChecker(){}
}
