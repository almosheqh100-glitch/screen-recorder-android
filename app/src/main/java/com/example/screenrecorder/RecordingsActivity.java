package com.example.screenrecorder;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.ViewGroup;
import android.widget.*;
import java.io.File;
import java.text.DateFormat;
import java.util.*;

public class RecordingsActivity extends Activity {
 private LinearLayout list;
 private VideoView player;
 public void onCreate(Bundle state){
  super.onCreate(state);
  LinearLayout rootView=new LinearLayout(this);rootView.setOrientation(LinearLayout.VERTICAL);rootView.setPadding(30,40,30,30);
  TextView title=new TextView(this);title.setText("مقاطع الفيديو المسجلة");title.setTextSize(25);rootView.addView(title);
  Button back=new Button(this);back.setText("العودة");back.setOnClickListener(v->finish());rootView.addView(back);
  player=new VideoView(this);player.setMediaController(new MediaController(this));rootView.addView(player,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,520));
  ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);rootView.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
  setContentView(rootView);load();
 }
 protected void onResume(){super.onResume();load();}
 private void load(){
  if(list==null)return;list.removeAllViews();
  File base=getExternalFilesDir(Environment.DIRECTORY_MOVIES);
  File[] files=base==null?null:new File(base,"ScreenRecords").listFiles((d,n)->n.toLowerCase(Locale.US).endsWith(".mp4"));
  if(files==null||files.length==0){TextView empty=new TextView(this);empty.setText("لا توجد تسجيلات حتى الآن");empty.setTextSize(18);empty.setPadding(0,30,0,0);list.addView(empty);return;}
  Arrays.sort(files,(a,b)->Long.compare(b.lastModified(),a.lastModified()));
  DateFormat format=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT);
  for(File file:files){Button item=new Button(this);item.setAllCaps(false);item.setText(format.format(new Date(file.lastModified()))+" — "+formatSize(file.length()));item.setOnClickListener(v->{player.setVideoPath(file.getAbsolutePath());player.start();});list.addView(item);}
 }
 private String formatSize(long bytes){return String.format(Locale.getDefault(),"%.1f MB",bytes/1048576.0);}
}
