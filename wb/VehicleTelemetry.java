package wb;
import org.json.*;
/** Normalized AI fields and separate local raw diagnostics. No raw arrays in model context. */
public final class VehicleTelemetry {
 public interface Reader {int[] read(int module,int command);}
 public final JSONObject context=new JSONObject(),diagnostics=new JSONObject();
 static void put(JSONObject j,String k,Object v){try{j.put(k,v==null?JSONObject.NULL:v);}catch(Exception ignored){}}
 public VehicleTelemetry(Reader r,JSONObject cfg){JSONObject missing=new JSONObject();put(context,"quality","decoded_from_vehiclehub_reference; not yet calibrated on this vehicle");put(context,"freshness_note","Queried now from CarInfoService cache; individual signal source timestamps are unavailable.");
  if(cfg.optBoolean("vehicle",true)){field(r,missing,"speed_kmh",0x50004,0,0,"");field(r,missing,"gear",0x50004,26,1,"");}
  if(cfg.optBoolean("fuel",true)){field(r,missing,"fuel_percent",0x50004,68,2,"");field(r,missing,"remaining_range_km",0x50004,72,3,"");field(r,missing,"low_fuel_warning",0x50004,2,4,"");}
  if(cfg.optBoolean("temperature",true)){String profile=cfg.optString("outsideEncoding","vehiclehub");field(r,missing,"outside_temperature_c",0x5000a,19,5,profile);field(r,missing,"driver_ac_setpoint_c",0x5000a,10,6,"");field(r,missing,"passenger_ac_setpoint_c",0x5000a,13,6,"");put(context,"temperature_note","AC fields are requested setpoints, not measured cabin temperature. LO/HI are modes, not Celsius values.");if("vehiclehub".equals(profile))put(context,"outside_temperature_note","VehicleHub compatibility rule: raw >100 divided by 10, otherwise integer Celsius. Encoding is heuristic and requires vehicle comparison; do not infer weather or faults.");}
  if(missing.length()>0)put(context,"unavailable_fields",missing);
 }
 void field(Reader r,JSONObject missing,String key,int module,int command,int kind,String encoding){int[] a=r.read(module,command);Object value=null;switch(kind){case 0:value=SignalDecoder.speed(a);break;case 1:value=SignalDecoder.gear(a);break;case 2:value=SignalDecoder.fuel(a);break;case 3:value=SignalDecoder.range(a);break;case 4:value=SignalDecoder.lowFuel(a);break;case 5:value=SignalDecoder.outside(a,encoding);break;case 6:value=SignalDecoder.ac(a);break;}
  JSONObject diag=new JSONObject();put(diag,"module",String.format("0x%05X",module));put(diag,"command",command);JSONArray raw=new JSONArray();if(a!=null)for(int n:a)raw.put(n);put(diag,"raw",a==null?JSONObject.NULL:raw);put(diag,"decoded",value);put(diag,"encoding",kind==5?encoding:"vehiclehub_reference");put(diagnostics,key,diag);
  if(value==null){put(context,key,JSONObject.NULL);put(missing,key,a==null?"read_unavailable":"invalid_or_unsupported_encoding");}else if(kind==6&&value instanceof String){put(context,key,JSONObject.NULL);put(context,key.replace("_c","_mode"),value);}else put(context,key,value);
 }
}
