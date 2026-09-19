package com.example.screenrecorder;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.*;

public class MainActivity extends Activity {
 private TextView status;
 private Button record;
 private CheckBox mic;
 private boolean requesting;
 private final Handler handler = new Handler();
 private final Runnable refresh = new Runnable() { public void run() {
  record.setText(ScreenRecordService.recording ? "إيقاف التسجيل" : "بدء التسجيل");
  record.setEnabled(!requesting && !ScreenRecordService.starting);
  mic.setEnabled(!ScreenRecordService.recording && !requesting && !ScreenRecordService.starting);
  status.setText(ScreenRecordService.recording ? "جارٍ تسجيل الشاشة" : ScreenRecordService.message);
  handler.postDelayed(this, 500);
 }};
 public void onCreate(Bundle state) {
  super.onCreate(state);
  LinearLayout box = new LinearLayout(this); box.setOrientation(1); box.setPadding(40,70,40,40);
  TextView title=new TextView(this); title.setText("تسجيل الشاشة"); title.setTextSize(28); box.addView(title);
  status=new TextView(this); status.setPadding(0,30,0,30); box.addView(status);
  mic=new CheckBox(this); mic.setText("تسجيل صوت الميكروفون"); box.addView(mic);
  record=new Button(this); box.addView(record); record.setOnClickListener(v -> {
   if(ScreenRecordService.recording) stopService(new Intent(this,ScreenRecordService.class)); else begin();
  });
  Button library=new Button(this); library.setText("مقاطع الفيديو المسجلة"); box.addView(library); library.setOnClickListener(v -> startActivity(new Intent(this,RecordingsActivity.class)));
  Button update=new Button(this); update.setText("التحقق من التحديث"); box.addView(update); update.setOnClickListener(v -> UpdateChecker.check(this,true));
  Button overlay=new Button(this); overlay.setText("تفعيل زر إيقاف عائم (اختياري)"); box.addView(overlay);
  overlay.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
  TextView help=new TextView(this); help.setText("يمكن الإيقاف من الإشعار. تُحفظ التسجيلات داخل التطبيق. افتح مقاطع الفيديو المسجلة لمشاهدتها. الصوت المدعوم: الميكروفون فقط."); box.addView(help);
  setContentView(box);
  UpdateChecker.check(this,false);
 }
 protected void onResume(){super.onResume();handler.post(refresh);}
 protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
 private void begin(){
  if(mic.isChecked() && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},2);return;}
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},3);return;}
  capture();
 }
 private void capture(){requesting=true; MediaProjectionManager manager=getSystemService(MediaProjectionManager.class); startActivityForResult(manager.createScreenCaptureIntent(),1);}
 public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);
  if(code==3){capture();} else if(code==2 && results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED){begin();} else ScreenRecordService.message="لم تُمنح صلاحية الميكروفون. يمكنك التسجيل بدون صوت.";
 }
 protected void onActivityResult(int code,int result,Intent data){super.onActivityResult(code,result,data);requesting=false;
  if(code==1 && result==RESULT_OK && data!=null){
   ScreenRecordService.starting=true;
   Intent service=new Intent(this,ScreenRecordService.class).putExtra("resultCode",result).putExtra("data",data).putExtra("mic",mic.isChecked());
   try{startForegroundService(service);}catch(RuntimeException e){ScreenRecordService.starting=false;ScreenRecordService.message="تعذر بدء الخدمة: "+e.getMessage();}
  } else ScreenRecordService.message="تم إلغاء التسجيل";
 }
}
