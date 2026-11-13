package wb;
import android.content.*;import android.content.res.*;import android.media.*;
/** Newly synthesized door recordings for the public app; no network or TTS. */
public final class DoorAudio {
 final Context context;MediaPlayer player;public volatile boolean busy;public java.util.function.Consumer<String> diagnostic;
 DoorAudio(Context c){context=c;}
 void note(String s){if(diagnostic!=null)diagnostic.accept(s);}
 public void play(int door,boolean open){stop();busy=true;String[] ids={"front_left","front_right","rear_left","rear_right"};String name="door-audio/door_"+ids[door]+(open?"_open.wav":"_closed.wav");MediaPlayer p=new MediaPlayer();player=p;try{p.setAudioAttributes(new AudioAttributes.Builder().setUsage(13).setContentType(4).build());try(AssetFileDescriptor fd=context.getAssets().openFd(name)){p.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());}p.setOnPreparedListener(mp->{if(player==mp){mp.start();note("本地门音频开始播放");}});p.setOnCompletionListener(mp->{note("本地门音频播放完成");if(player==mp)stop();});p.setOnErrorListener((mp,what,extra)->{note("本地音频错误 "+what+"/"+extra);if(player==mp)stop();return true;});p.prepareAsync();}catch(Exception e){note("本地音频失败："+e.getClass().getSimpleName());stop();}}
 public void stop(){busy=false;if(player!=null){MediaPlayer old=player;player=null;try{old.reset();old.release();}catch(Exception ignored){}}}
}
