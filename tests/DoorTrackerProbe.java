import wb.DoorTracker;
public class DoorTrackerProbe {
 static int n;static void check(boolean b){n++;if(!b)throw new AssertionError("case "+n);}
 public static void main(String[] args){DoorTracker t=new DoorTracker();for(int d=0;d<4;d++){check(t.update(d,0,1000)==null);check(t.update(d,1,1100)==1);check(t.update(d,1,1200)==null);check(t.accept(d,true,1100,15000));check(!t.accept(d,true,1200,15000));check(t.update(d,0,1300)==0);check(t.accept(d,false,1300,15000));}check(t.update(0,null,1400)==null);check(t.update(0,7,1500)==null);check(t.update(0,1,8000)==null);check(t.update(0,0,8100)==0);check(t.accept(0,true,17000,15000));t.reset();for(int d=0;d<4;d++){check(t.update(d,1,18000)==null);check(t.accept(d,true,18000,15000));}System.out.println(n+" door baseline/edge/cooldown assertions passed");}
}
