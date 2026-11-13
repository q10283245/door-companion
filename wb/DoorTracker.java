package wb;
/** Pure door transition and independent open/close cooldown state. */
public final class DoorTracker {
 private final Integer[] value=new Integer[4];private final long[] seen={-1,-1,-1,-1};private final long[][] accepted={{-1,-1},{-1,-1},{-1,-1},{-1,-1}};
 public Integer update(int door,Integer raw,long now){if(raw==null||(raw!=0&&raw!=1))return null;Integer old=seen[door]<0||now-seen[door]>5000?null:value[door];value[door]=raw;seen[door]=now;return old!=null&&!old.equals(raw)?raw:null;}
 public boolean accept(int door,boolean open,long now,long cooldown){int edge=open?1:0;long last=accepted[door][edge];if(last>=0&&now-last<cooldown)return false;accepted[door][edge]=now;return true;}
 public void reset(){for(int i=0;i<4;i++){value[i]=null;seen[i]=-1;accepted[i][0]=accepted[i][1]=-1;}}
}
