package wb;
import android.app.*;import android.os.*;import android.content.*;import android.webkit.*;import android.view.*;
public class MainActivity extends Activity {
 WebView web; final Handler ui=new Handler();
 public void onCreate(Bundle b){super.onCreate(b);getWindow().setFlags(1024,1024);startForegroundService(new Intent(this,AssistantService.class));web=new WebView(this);setContentView(web);web.setBackgroundColor(0xff0c1418);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);web.getSettings().setMixedContentMode(0);web.addJavascriptInterface(new Bridge(),"Native");web.setWebViewClient(new WebViewClient(){public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return true;}});web.loadUrl("file:///android_asset/index.html");}
 class Bridge {
 @JavascriptInterface public String state(){return Engine.get(MainActivity.this).state().toString();}
 @JavascriptInterface public String config(){return Engine.get(MainActivity.this).config().toString();}
 @JavascriptInterface public void save(String s){Engine.get(MainActivity.this).save(s);}
 @JavascriptInterface public void importConfig(){runOnUiThread(()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,71);});}
 @JavascriptInterface public void requestLocation(){runOnUiThread(()->{if(!Engine.get(MainActivity.this).location.permitted())requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION},72);else {Engine.get(MainActivity.this).refreshLocation();android.widget.Toast.makeText(MainActivity.this,"定位已授权，启用位置上下文后生效",0).show();}});}
 @JavascriptInterface public void locationSettings(){runOnUiThread(()->{try{startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));}catch(Exception e){android.widget.Toast.makeText(MainActivity.this,"请在系统设置中打开定位",0).show();}});}
 @JavascriptInterface public void clearRequests(){Engine.get(MainActivity.this).clearRequests();}
 @JavascriptInterface public void batterySettings(){runOnUiThread(()->{try{startActivity(new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));}catch(Exception e){android.widget.Toast.makeText(MainActivity.this,"此车机没有电池优化设置页",1).show();}});}
 @JavascriptInterface public void modelTest(){Engine.get(MainActivity.this).modelTest();}
 @JavascriptInterface public void audition(){Engine.get(MainActivity.this).audition();}
 @JavascriptInterface public void stopSpeech(){Engine.get(MainActivity.this).stopSpeech();}
 @JavascriptInterface public void simulate(boolean open){Engine.get(MainActivity.this).simulate(open);}
 }
 protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=71||result!=RESULT_OK||data==null||data.getData()==null)return;try(java.io.InputStream in=getContentResolver().openInputStream(data.getData())){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>16384)throw new java.io.IOException();}org.json.JSONObject imported=new org.json.JSONObject(out.toString("UTF-8"));org.json.JSONObject c=Engine.get(this).config();for(String k:new String[]{"baseUrl","model","key"}){if(!imported.has(k))throw new java.io.IOException();c.put(k,imported.getString(k));}if(!c.getString("baseUrl").startsWith("https://"))throw new java.io.IOException();c.put("ai",false);Engine.get(this).save(c.toString());web.reload();android.widget.Toast.makeText(this,"模型配置已导入，请确认后开启 AI",1).show();}catch(Exception e){android.widget.Toast.makeText(this,"导入失败，请选择有效的模型配置 JSON",1).show();}}
 protected void onResume(){super.onResume();Engine.get(this).refreshLocation();}
 public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==72){Engine.get(this).refreshLocation();android.widget.Toast.makeText(this,Engine.get(this).location.permitted()?"定位已授权，等待有效位置":"未授予定位，其他播报仍可使用",1).show();}}
 protected void onDestroy(){web.removeJavascriptInterface("Native");web.destroy();super.onDestroy();}
}
