package wb;
import org.json.*;import java.io.*;import java.nio.file.*;import java.util.*;
/** Bounded private journal. Never accepts configuration, headers or credentials. */
public final class RequestJournal {
 private final File file;private JSONArray entries=new JSONArray();
 public RequestJournal(File f){file=f;try{if(f.isFile()&&f.length()<1000000)entries=new JSONArray(new String(Files.readAllBytes(f.toPath()),"UTF-8"));}catch(Exception ignored){}trim();try{for(int i=0;i<entries.length();i++){JSONObject j=entries.getJSONObject(i);if("请求中".equals(j.optString("status"))||"等待播报".equals(j.optString("status")))j.put("status","已中断：应用进程重启");}save();}catch(Exception ignored){}}
 private void trim(){while(entries.length()>80)entries.remove(0);}
 private void save(){if(file==null)return;try{File temp=new File(file.getPath()+".tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(entries.toString().getBytes("UTF-8"));out.getFD().sync();}Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}catch(Exception ignored){}}
 public synchronized String begin(String source,String model,String host,JSONObject context){String id=UUID.randomUUID().toString();try{JSONObject j=new JSONObject().put("id",id).put("time",new java.text.SimpleDateFormat("MM-dd HH:mm:ss",Locale.CHINA).format(new Date())).put("source",source).put("model",model).put("host",host).put("status","请求中");if(context!=null)j.put("context",new JSONObject(context.toString()));entries.put(j);trim();save();}catch(Exception ignored){}return id;}
 public synchronized void payload(String id,JSONObject body){try{for(int i=entries.length()-1;i>=0;i--){JSONObject j=entries.getJSONObject(i);if(id.equals(j.optString("id"))){j.put("requestBody",new JSONObject(body.toString()));save();break;}}}catch(Exception ignored){}}
 public synchronized void update(String id,String status,int http,long ms,String text,JSONObject usage){try{for(int i=entries.length()-1;i>=0;i--){JSONObject j=entries.getJSONObject(i);if(!id.equals(j.optString("id")))continue;j.put("status",status);if(http>=0)j.put("http",http);if(ms>=0)j.put("elapsed_ms",ms);if(text!=null)j.put("text",text.substring(0,Math.min(160,text.length())));if(usage!=null){JSONObject tokens=new JSONObject();for(String k:new String[]{"prompt_tokens","completion_tokens","total_tokens"})if(usage.has(k))tokens.put(k,usage.optLong(k));j.put("usage",tokens);}save();break;}}catch(Exception ignored){}}
 public synchronized JSONArray list(){JSONArray copy=new JSONArray();try{for(int i=entries.length()-1;i>=0;i--)copy.put(new JSONObject(entries.getJSONObject(i).toString()));}catch(Exception ignored){}return copy;}
 public synchronized void clear(){entries=new JSONArray();save();}
}
