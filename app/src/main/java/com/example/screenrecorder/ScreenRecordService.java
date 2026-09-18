package com.example.screenrecorder;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.Button;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScreenRecordService extends Service {
 public static volatile boolean recording=false, starting=false;
 public static volatile String message="جاهز للتسجيل";
 private MediaProjection projection;
 private MediaRecorder recorder;
 private VirtualDisplay display;
 private ParcelFileDescriptor descriptor;
 private Uri output;
 private boolean started, cleaning;
 private WindowManager windows;
 private Button overlay;
 private String filename;
 public void onCreate(){super.onCreate(); getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("recording","تسجيل الشاشة",NotificationManager.IMPORTANCE_LOW));}
 public int onStartCommand(Intent intent,int flags,int id){
  if(intent==null || "STOP".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
  if(projection!=null || recording)return START_NOT_STICKY;
  try {
   boolean mic=intent.getBooleanExtra("mic",false);
   Intent stop=new Intent(this,ScreenRecordService.class).setAction("STOP");
   PendingIntent stopAction=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   PendingIntent open=PendingIntent.getActivity(this,2,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification notification=new Notification.Builder(this,"recording").setSmallIcon(android.R.drawable.presence_video_online).setContentTitle("جارٍ تسجيل الشاشة").setContentText("اضغط إيقاف لإنهاء التسجيل").setContentIntent(open).addAction(new Notification.Action.Builder(android.R.drawable.ic_media_pause,"إيقاف",stopAction).build()).setOngoing(true).build();
   int type=ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
   if(mic && Build.VERSION.SDK_INT>=30)type|=ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
   startForeground(42,notification,type);
   Intent data=intent.getParcelableExtra("data");
   if(data==null)throw new IllegalArgumentException("إذن الشاشة مفقود");
   projection=getSystemService(MediaProjectionManager.class).getMediaProjection(intent.getIntExtra("resultCode",Activity.RESULT_CANCELED),data);
   projection.registerCallback(new MediaProjection.Callback(){public void onStop(){stopSelf();}},new Handler(getMainLooper()));
   DisplayMetrics metrics=new DisplayMetrics(); windows=getSystemService(WindowManager.class); windows.getDefaultDisplay().getRealMetrics(metrics);
   double scale=Math.min(1.0,1080.0/Math.min(metrics.widthPixels,metrics.heightPixels));
   int width=((int)(metrics.widthPixels*scale)/2)*2, height=((int)(metrics.heightPixels*scale)/2)*2;
   filename="record_"+new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.US).format(new Date())+".mp4";
   ContentValues values=new ContentValues(); values.put(MediaStore.Video.Media.DISPLAY_NAME,filename);values.put(MediaStore.Video.Media.MIME_TYPE,"video/mp4");values.put(MediaStore.Video.Media.RELATIVE_PATH,Environment.DIRECTORY_MOVIES+"/ScreenRecords");values.put(MediaStore.Video.Media.IS_PENDING,1);
   output=getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,values);
   if(output==null)throw new IllegalStateException("تعذر إنشاء ملف الفيديو");
   descriptor=getContentResolver().openFileDescriptor(output,"w");
   if(descriptor==null)throw new IllegalStateException("تعذر فتح ملف الفيديو");
   recorder=new MediaRecorder();
   if(mic)recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
   recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
   recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);if(mic)recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
   recorder.setVideoSize(width,height);recorder.setVideoEncodingBitRate(6000000);recorder.setVideoFrameRate(30);recorder.setOutputFile(descriptor.getFileDescriptor());recorder.prepare();
   display=projection.createVirtualDisplay("ScreenRecord",width,height,metrics.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,recorder.getSurface(),null,null);
   recorder.start();started=true;recording=true;starting=false;message="جارٍ تسجيل الشاشة";
   showOverlay();
  }catch(Exception e){message="تعذر التسجيل: "+e.getMessage();starting=false;stopSelf();}
  return START_NOT_STICKY;
 }
 private void showOverlay(){
  if(!Settings.canDrawOverlays(this))return;
  try{overlay=new Button(this);overlay.setText("إيقاف التسجيل");overlay.setOnClickListener(v -> stopSelf());
   WindowManager.LayoutParams params=new WindowManager.LayoutParams(-2,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);params.gravity=Gravity.TOP|Gravity.END;params.x=16;params.y=100;windows.addView(overlay,params);
  }catch(RuntimeException ignored){overlay=null;}
 }
 public IBinder onBind(Intent intent){return null;}
 public void onDestroy(){
  if(!cleaning){cleaning=true;boolean valid=false;
   if(overlay!=null){try{windows.removeView(overlay);}catch(RuntimeException ignored){}overlay=null;}
   if(recorder!=null){try{if(started){recorder.stop();valid=true;}}catch(RuntimeException e){message="لم يُحفظ التسجيل؛ قد تكون مدته قصيرة جدًا.";}finally{try{recorder.release();}catch(RuntimeException ignored){}recorder=null;}}
   if(display!=null){display.release();display=null;}
   if(projection!=null){projection.stop();projection=null;}
   if(descriptor!=null){try{descriptor.close();}catch(Exception ignored){}descriptor=null;}
   if(output!=null){try{if(valid){ContentValues values=new ContentValues();values.put(MediaStore.Video.Media.IS_PENDING,0);getContentResolver().update(output,values,null,null);message="تم الحفظ في Movies/ScreenRecords: "+filename;}else getContentResolver().delete(output,null,null);}catch(RuntimeException e){message="تعذر حفظ الفيديو: "+e.getMessage();}output=null;}
   recording=false;starting=false;stopForeground(STOP_FOREGROUND_REMOVE);
  }
  super.onDestroy();
 }
}
