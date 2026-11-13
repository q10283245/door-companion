import wb.*;import org.json.*;
public class TelemetryProbe {
 static int n;static void ok(boolean yes){n++;if(!yes)throw new AssertionError("case "+n);}static void equal(Object expected,Object actual){ok(expected==null?actual==null:expected.equals(actual));}
 public static void main(String[] args)throws Exception{
 equal(0.0,SignalDecoder.speed(new int[]{0}));equal(88.0,SignalDecoder.speed(new int[]{88}));equal(null,SignalDecoder.speed(new int[]{255}));equal(null,SignalDecoder.speed(new int[]{-1}));equal(null,SignalDecoder.speed(new int[]{0,88}));equal(null,SignalDecoder.speed(null));
 equal(62.0,SignalDecoder.fuel(new int[]{62}));equal(62.5,SignalDecoder.fuel(new int[]{2,113}));equal(100.0,SignalDecoder.fuel(new int[]{3,232}));equal(0.0,SignalDecoder.fuel(new int[]{0}));equal(null,SignalDecoder.fuel(new int[]{101}));equal(null,SignalDecoder.fuel(new int[]{255,255}));equal(null,SignalDecoder.fuel(new int[]{-1,10}));equal(null,SignalDecoder.fuel(new int[]{1,2,3}));
 equal(420,SignalDecoder.range(new int[]{1,164}));equal(255,SignalDecoder.range(new int[]{255}));equal(null,SignalDecoder.range(new int[]{255,255}));equal(null,SignalDecoder.range(new int[]{240,0}));
 equal("P",SignalDecoder.gear(new int[]{1}));equal("D",SignalDecoder.gear(new int[]{6}));equal(null,SignalDecoder.gear(new int[]{0}));equal(false,SignalDecoder.lowFuel(new int[]{0}));equal(true,SignalDecoder.lowFuel(new int[]{1}));equal(null,SignalDecoder.lowFuel(new int[]{255}));
 equal("LO",SignalDecoder.ac(new int[]{0}));equal(25.0,SignalDecoder.ac(new int[]{10}));equal("HI",SignalDecoder.ac(new int[]{16}));equal(22.5,SignalDecoder.ac(new int[]{225}));equal(null,SignalDecoder.ac(new int[]{255}));equal(null,SignalDecoder.ac(new int[]{1,225}));
 equal(25.0,SignalDecoder.outside(new int[]{25},"vehiclehub"));equal(25.0,SignalDecoder.outside(new int[]{250},"vehiclehub"));equal(null,SignalDecoder.outside(new int[]{255},"vehiclehub"));equal(null,SignalDecoder.outside(new int[]{90},"vehiclehub"));equal(-5.0,SignalDecoder.outside(new int[]{-5},"celsius"));equal(-5.0,SignalDecoder.outside(new int[]{-50},"tenths"));equal(null,SignalDecoder.outside(new int[]{9999},"tenths"));
 ok(LocationPolicy.rejection(31,121,10,5000,false)==null);ok(LocationPolicy.rejection(31,121,10,120001,false)!=null);ok(LocationPolicy.rejection(31,121,10,-100,false)!=null);ok(LocationPolicy.rejection(91,121,10,0,false)!=null);ok(LocationPolicy.rejection(31,121,4000,0,false)!=null);ok(LocationPolicy.rejection(31,121,10,0,true)!=null);ok(LocationPolicy.rejection(31,121,Double.NaN,0,false)!=null);
 System.out.println(n+" conversion, invalid-value and location-freshness assertions passed");
 }
}
