package wb;
/** Pure validation; monotonic age avoids head-unit calendar changes. */
public final class LocationPolicy {
 public static String rejection(double lat,double lon,double accuracy,long ageMs,boolean mock){if(mock)return "模拟定位不用于车况";if(!Double.isFinite(lat)||!Double.isFinite(lon)||lat< -90||lat>90||lon< -180||lon>180)return "经纬度无效";if(!Double.isFinite(accuracy)||accuracy<=0||accuracy>3000)return "定位精度不足";if(ageMs<0||ageMs>120000)return "定位过期，等待新位置";return null;}
}
