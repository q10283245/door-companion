package wb;
import android.content.Context;import android.location.*;import java.util.*;import org.json.*;
public final class NativeAddress {
 static void add(JSONObject j,String k,String v)throws JSONException{if(v!=null&&!v.trim().isEmpty())j.put(k,v);}
 public static JSONObject lookup(Context context,String provider,String key,double lat,double lon)throws Exception{
  if(provider.equals("amap"))return AmapAddress.lookup(key,lat,lon);
  if(!Geocoder.isPresent())throw new AddressResolver.Unavailable("车机没有系统地址解析服务，可改用高德并填写 Key");
  List<Address> list=new Geocoder(context,Locale.SIMPLIFIED_CHINESE).getFromLocation(lat,lon,1);if(list==null||list.isEmpty())return null;Address a=list.get(0);JSONObject out=new JSONObject();add(out,"country",a.getCountryName());add(out,"province",a.getAdminArea());add(out,"city",a.getLocality());add(out,"district",a.getSubLocality()!=null?a.getSubLocality():a.getSubAdminArea());add(out,"street",a.getThoroughfare());add(out,"number",a.getSubThoroughfare());add(out,"landmark",a.getFeatureName());return out;
 }
}
