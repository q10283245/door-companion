package wb;
import android.content.*;
public final class BootReceiver extends BroadcastReceiver {
 public void onReceive(Context context,Intent intent){String a=intent.getAction();if(!Intent.ACTION_BOOT_COMPLETED.equals(a)&&!Intent.ACTION_MY_PACKAGE_REPLACED.equals(a))return;try{org.json.JSONObject cfg=new org.json.JSONObject(context.getSharedPreferences("assistant",0).getString("config","{}"));if(cfg.optBoolean("autoStart",true)&&cfg.optBoolean("enabled",true))context.startForegroundService(new Intent(context,AssistantService.class));}catch(Exception ignored){}}
}
