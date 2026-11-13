package wb;
/** Read-only conversions traced to the supplied VehicleHub APK. Null means unavailable. */
public final class SignalDecoder {
 private static Integer scalar(int[] a){return a!=null&&a.length==1?a[0]:null;}
 private static Integer unsigned(int[] a){if(a==null)return null;if(a.length==1)return a[0]>=0?a[0]:null;if(a.length==2&&a[0]>=0&&a[0]<=255&&a[1]>=0&&a[1]<=255)return (a[0]<<8)|a[1];return null;}
 public static Double speed(int[] a){Integer v=scalar(a);return v!=null&&v>=0&&v<255?v.doubleValue():null;}
 public static Double fuel(int[] a){Integer v=unsigned(a);if(v==null)return null;if(a.length==1)return v<=100?v.doubleValue():null;return v<=1000?v/10.0:null;}
 public static Integer range(int[] a){Integer v=unsigned(a);return v!=null&&v<0xf000?v:null;}
 public static String gear(int[] a){Integer v=scalar(a);if(v==null)return null;if(v>=6&&v<=12)return "D";switch(v){case 1:return "P";case 2:return "R";case 3:return "N";case 4:return "D";case 5:return "M";default:return null;}}
 public static Boolean lowFuel(int[] a){Integer v=scalar(a);return v!=null&&(v==0||v==1)?v==1:null;}
 public static Object ac(int[] a){Integer v=scalar(a);if(v==null||v<0||v==255)return null;if(v==0)return "LO";if(v==16||v==31)return "HI";if(v>=1&&v<=15)return (double)(v+15);if(v>=160&&v<=350)return v/10.0;if(v>=17&&v<=30)return v.doubleValue();return null;}
 public static Double outside(int[] a,String encoding){Integer v=scalar(a);if(v==null||v==255||v==65535||v==-1)return null;double c;
  if("celsius".equals(encoding))c=v;
  else if("tenths".equals(encoding))c=v/10.0;
  else if("vehiclehub".equals(encoding)){if(v<0)return null;c=v>100?v/10.0:v;}else return null;
  return c>=-50&&c<=85?c:null;
 }
}
