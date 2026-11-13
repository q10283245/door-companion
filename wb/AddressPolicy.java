package wb;
import org.json.*;import java.util.*;
/** Address components are provider data, never a model-generated guess. */
public final class AddressPolicy {
 public static double distance(double lat1,double lon1,double lat2,double lon2){double dLat=Math.toRadians(lat2-lat1),dLon=Math.toRadians(lon2-lon1);double a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);return 6371000*2*Math.asin(Math.sqrt(Math.min(1,Math.max(0,a))));}
 public static String text(JSONObject j,String name){Object v=j.opt(name);if(!(v instanceof String))return "";String s=((String)v).replaceAll("[\\p{Cntrl}]"," ").trim();return s.substring(0,Math.min(160,s.length()));}
 public static JSONObject format(JSONObject raw,double uncertainty)throws JSONException{JSONObject a=new JSONObject();if(!Double.isFinite(uncertainty)||uncertainty<0)return a;for(String k:new String[]{"country","province","city","district","township","street","number","landmark"}){if(uncertainty>1000&&(k.equals("district")||k.equals("township")))continue;if(uncertainty>100&&(k.equals("street")||k.equals("township")||k.equals("landmark")))continue;if(uncertainty>30&&k.equals("number"))continue;String s=text(raw,k);if(!s.isEmpty())a.put(k,s);}
  if(!a.has("city")&&text(raw,"country").equals("中国")&&Arrays.asList("北京市","天津市","上海市","重庆市").contains(text(raw,"province")))a.put("city",text(raw,"province"));
  StringBuilder display=new StringBuilder();for(String k:new String[]{"province","city","district","township","street","number","landmark"}){String part=text(a,k);if(!part.isEmpty()&&display.indexOf(part)<0)display.append(part);}
  if(display.length()==0)return new JSONObject();a.put("display_name",display+"附近");a.put("detail_level",a.has("street")||a.has("township")||a.has("landmark")?"street_or_nearby":a.has("district")?"district":a.has("city")?"city":"region");a.put("note","逆地理编码返回的附近地址，不代表车辆位于某栋建筑或门牌内；不推断目的地、天气。");return a;
 }
}
