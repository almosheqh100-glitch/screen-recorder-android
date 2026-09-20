package com.example.screenrecorder;

import android.app.*;
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
 private Button selectAll, deleteSelected;
 private final LinkedHashMap<CheckBox,File> choices=new LinkedHashMap<>();

 public void onCreate(Bundle state){
  super.onCreate(state);
  LinearLayout rootView=new LinearLayout(this);rootView.setOrientation(LinearLayout.VERTICAL);rootView.setPadding(30,40,30,30);
  TextView title=new TextView(this);title.setText("مقاطع الفيديو المسجلة");title.setTextSize(25);rootView.addView(title);
  LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
  Button back=new Button(this);back.setText("العودة");back.setOnClickListener(v->finish());actions.addView(back,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
  selectAll=new Button(this);selectAll.setText("تحديد الكل");selectAll.setOnClickListener(v->toggleAll());actions.addView(selectAll,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
  deleteSelected=new Button(this);deleteSelected.setText("حذف المحدد");deleteSelected.setEnabled(false);deleteSelected.setOnClickListener(v->confirmDelete());actions.addView(deleteSelected,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
  rootView.addView(actions);
  player=new VideoView(this);player.setMediaController(new MediaController(this));rootView.addView(player,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,520));
  ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);rootView.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
  setContentView(rootView);load();
 }
 protected void onResume(){super.onResume();load();}
 private void load(){
  if(list==null)return;list.removeAllViews();choices.clear();
  File base=getExternalFilesDir(Environment.DIRECTORY_MOVIES);
  File[] files=base==null?null:new File(base,"ScreenRecords").listFiles((d,n)->n.toLowerCase(Locale.US).endsWith(".mp4"));
  if(files==null||files.length==0){TextView empty=new TextView(this);empty.setText("لا توجد تسجيلات حتى الآن");empty.setTextSize(18);empty.setPadding(0,30,0,0);list.addView(empty);updateActions();return;}
  Arrays.sort(files,(a,b)->Long.compare(b.lastModified(),a.lastModified()));
  DateFormat format=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT);
  for(File file:files){
   LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
   CheckBox check=new CheckBox(this);check.setText(format.format(new Date(file.lastModified()))+" — "+formatSize(file.length()));check.setOnCheckedChangeListener((b,on)->updateActions());row.addView(check,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
   Button play=new Button(this);play.setText("تشغيل");play.setOnClickListener(v->{player.setVideoPath(file.getAbsolutePath());player.start();});row.addView(play);
   choices.put(check,file);list.addView(row);
  }
  updateActions();
 }
 private void toggleAll(){boolean all=allSelected();for(CheckBox box:choices.keySet())box.setChecked(!all);updateActions();}
 private boolean allSelected(){return !choices.isEmpty()&&selectedCount()==choices.size();}
 private int selectedCount(){int count=0;for(CheckBox box:choices.keySet())if(box.isChecked())count++;return count;}
 private void updateActions(){int count=selectedCount();if(deleteSelected!=null){deleteSelected.setEnabled(count>0);deleteSelected.setText(count>0?"حذف المحدد ("+count+")":"حذف المحدد");}if(selectAll!=null)selectAll.setText(allSelected()?"إلغاء تحديد الكل":"تحديد الكل");}
 private void confirmDelete(){int count=selectedCount();if(count==0)return;new AlertDialog.Builder(this).setTitle("حذف المقاطع").setMessage("هل تريد حذف "+count+" من المقاطع نهائيًا؟").setNegativeButton("إلغاء",null).setPositiveButton("حذف",(d,w)->deleteNow()).show();}
 private void deleteNow(){int deleted=0;for(Map.Entry<CheckBox,File> entry:new ArrayList<>(choices.entrySet()))if(entry.getKey().isChecked()&&entry.getValue().delete())deleted++;player.stopPlayback();Toast.makeText(this,"تم حذف "+deleted+" من المقاطع",Toast.LENGTH_SHORT).show();load();}
 private String formatSize(long bytes){return String.format(Locale.getDefault(),"%.1f MB",bytes/1048576.0);}
}
