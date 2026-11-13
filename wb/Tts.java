package wb;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

/**
 * 车机 TTS 客户端 —— 直连讯飞私有 AIDL 服务（诊断级）。
 *
 * 协议（从 IflytekSpeechClient.apk 字节码逆向确认）：
 *   ITtsAidlService 事务码：
 *     1 registerClient(listener, usage:int, isSingleTts:boolean) → boolean
 *       服务端实现：校验 binder 活性 → 同步回调 listener.getClientId()（事务码5）
 *       → listenerMap 存档 → linkToDeath → post 延时任务创建 TtsClient（key=clientId）→ 返回 true
 *       usage 不校验；官方系统客户端传 usage=16, isSingleTts=false
 *     5 startSpeak(listener, String) → int
 *       服务端：listener 空则 -1；文本空则 -1；clientMap 查不到 clientId 则 -1001；
 *       命中则 TtsClient.startSpeak(text)（文本为纯文本，日志侧才加密）
 *     8 stopSpeak(listener) → int
 *   ITtsAgentAidlListener 回调（我们实现，Stub 方向）：
 *     1 onPlayBegin 2 onPlayCompleted 3 onPlayInterrupted 4 onProgressReturn(int,int)
 *     5 getClientId → long（注册与每次 startSpeak 都会被同步调用）
 *     6 onTtsInited(boolean,int) 7 onTTSPlayPause 8 onTTSPlayResume
 */
public class Tts {

    static final String PKG  = "com.iflytek.cutefly.speechclient.hmi";
    static final String SVC  = "com.iflytek.autofly.tts.ttsservice.TtsAdapterService";
    static final String DESC = "com.iflytek.autofly.tts.ITtsAidlService";
    static final String LDESC = "com.iflytek.autofly.tts.ITtsAgentAidlListener";

    static final int TX_REGISTER = 1, TX_RELEASE = 2, TX_SETPARAM = 3, TX_START = 5, TX_STOP = 8;

    /** 官方系统客户端 TtsHelper.init 用的 usage=16；isSingleTts=false */
    static final int USAGE = 16;

    /** getClientId 回调返回的 long，任意稳定值即可（key 只要求一致） */
    static final long CLIENT_ID = 0x444F4F52L; // "WBP"

    static final String TAG = "wb-tts";

    private IBinder remote;
    private Context ctx;
    private final Binder listener = new Listener();

    private volatile String state = "未连接";
    private volatile boolean registered = false;
    public volatile boolean busy = false;
    private volatile boolean inited = false;      // 服务端 onTtsInited(true) 后为真
    private volatile int lastCode = -999;
    private volatile long plays = 0;
    private final StringBuilder trace = new StringBuilder();

    public java.util.function.Consumer<String> diagnostic;
    private synchronized void note(String s) {
        if(diagnostic!=null)try{diagnostic.accept(s);}catch(Exception ignored){}
        Log.i(TAG, s);
        synchronized (trace) {
            if (trace.length() > 1500) trace.delete(0, 500);
            trace.append(s).append('\n');
        }
    }

    public String status() {
        String s = state;
        if (registered) s += " 已注册";
        if (inited) s += " inited";
        if (plays > 0) s += " 播报" + plays + "次";
        if (lastCode != -999) s += " rc=" + lastCode;
        return s;
    }

    /** 供诊断按钮：最近全过程轨迹 */
    public String trace() {
        synchronized (trace) {
            return trace.toString();
        }
    }

    public boolean isReady() { return remote != null && registered; }

    // ---------------- 连接 ----------------

    public synchronized void connect(Context c) {
        if (c == null || remote != null) return;
        ctx = c;
        try {
            Intent i = new Intent();
            i.setComponent(new ComponentName(PKG, SVC));
            boolean ok = c.bindService(i, conn, Context.BIND_AUTO_CREATE);
            state = ok ? "绑定中…" : "bindService 失败";
            note("bindService: " + ok);
        } catch (Throwable t) {
            state = "绑定异常: " + t;
            note("bind 异常 " + t);
        }
    }

    public synchronized void release() {
        try {
            if (remote != null) transactSimple(TX_RELEASE, true);
        } catch (Throwable ignored) {
        }
        try {
            if (ctx != null) ctx.unbindService(conn);
        } catch (Throwable ignored) {
        }
        remote = null;
        registered = false;
        inited = false;
        busy = false;
        state = "已释放";
    }

    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName n, IBinder b) {
            remote = b;
            state = "已连接";
            note("onServiceConnected: " + n);
            new Thread(() -> {
                boolean ok = register();
                registered = ok;
                if (ok) {
                    state = "已注册";
                } else {
                    state = "注册失败";
                    // 注册失败自动重试 3 次（服务端可能尚未把 clientMap 建好）
                    for (int i = 1; i <= 3 && !registered; i++) {
                        try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
                        note("注册重试 " + i);
                        registered = register();
                        if (registered) state = "已注册(重试" + i + ")";
                    }
                    if (!registered) state = "注册失败×4";
                }
            }, "wb-tts-reg").start();
        }

        @Override public void onServiceDisconnected(ComponentName n) {
            remote = null;
            registered = false;
            inited = false;
            busy = false;
            state = "服务断开";
            note("onServiceDisconnected");
        }
    };

    private boolean register() {
        Parcel d = Parcel.obtain(), r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESC);
            d.writeStrongBinder(listener);
            d.writeInt(USAGE);          // usage（服务端不校验，仅透传日志）
            d.writeInt(0);              // isSingleTts=false（与官方客户端一致）
            remote.transact(TX_REGISTER, d, r, 0);
            r.readException();
            boolean ok = r.readInt() != 0;
            note("registerClient rc=" + ok);
            return ok;
        } catch (Throwable t) {
            state = "注册异常: " + t.getClass().getSimpleName();
            note("register 异常 " + t);
            return false;
        } finally {
            r.recycle();
            d.recycle();
        }
    }

    // ---------------- 播报 ----------------

    /**
     * 播报一段文本。异步执行 transact（真实结果写进 status()/trace()），
     * 返回值仅表示入队：0 已入队，-1 未就绪。
     */
    public int speak(final String text) {
        final IBinder b = remote;
        if (b == null) {
            state = "TTS 未连接";
            note("speak 拒绝: 未连接");
            return -1;
        }
        busy = true;
        final String t = (text == null || text.trim().isEmpty()) ? "语音播报测试" : text;
        new Thread(() -> {
            if (!registered) {
                // 未注册就尝试补注册一次
                note("speak 时未注册，补注册");
                registered = register();
            }
            if (!registered) {
                busy = false;
                state = "TTS 未就绪(注册失败)";
                note("speak 拒绝: 注册失败");
                return;
            }
            speakSync(b, t);
        }, "wb-tts").start();
        return 0;
    }

    private void speakSync(IBinder b, String t) {
        Parcel d = Parcel.obtain(), r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESC);
            d.writeStrongBinder(listener);
            d.writeString(t);
            b.transact(TX_START, d, r, 0);
            r.readException();
            int rc = r.readInt();
            lastCode = rc;
            if(rc != 0) busy=false;
            if (rc == 0) {
                plays++;
                state = "已下发";
            } else if (rc == -1001) {
                state = "服务端无此客户端(-1001)";
            } else {
                state = "播报返回 " + rc;
            }
            note("startSpeak rc=" + rc + " text=" + t);
        } catch (Throwable e) {
            busy=false;
            state = "播报异常: " + e.getClass().getSimpleName();
            note("speak 异常 " + e);
        } finally {
            r.recycle();
            d.recycle();
        }
    }

    public void stop() {
        busy=false;
        try {
            transactSimple(TX_STOP, true);
        } catch (Throwable ignored) {
        }
    }

    /** setParam(listener, key, value) —— 参数语义未知，留作实测调音色/音量 */
    public int setParam(int key, int value) {
        Parcel d = Parcel.obtain(), r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESC);
            d.writeStrongBinder(listener);
            d.writeInt(key);
            d.writeInt(value);
            remote.transact(TX_SETPARAM, d, r, 0);
            r.readException();
            return r.readInt();
        } catch (Throwable t) {
            return -1;
        } finally {
            r.recycle();
            d.recycle();
        }
    }

    private void transactSimple(int code, boolean withListener) throws RemoteException {
        IBinder b = remote;
        if (b == null) return;
        Parcel d = Parcel.obtain(), r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESC);
            if (withListener) d.writeStrongBinder(listener);
            b.transact(code, d, r, 0);
            r.readException();
        } finally {
            r.recycle();
            d.recycle();
        }
    }

    // ---------------- 回调 Binder ----------------

    private final class Listener extends Binder {
        @Override public String getInterfaceDescriptor() {
            return LDESC;
        }

        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            if (code == 5) {                     // getClientId() → long
                data.enforceInterface(LDESC);
                if (reply != null) {
                    reply.writeNoException();
                    reply.writeLong(CLIENT_ID);
                }
                note("回调 getClientId → " + CLIENT_ID);
                return true;
            }
            // 其余回调按协议读参，避免残留数据
            try {
                data.enforceInterface(LDESC);
            } catch (Throwable ignored) {
            }
            switch (code) {
                case 4: {                        // onProgressReturn(int,int)
                    int a = data.readInt(), b2 = data.readInt();
                    note("回调 进度 " + a + "/" + b2);
                    break;
                }
                case 6: {                        // onTtsInited(boolean,int)
                    int ok = data.readInt();
                    int err = data.readInt();
                    inited = ok != 0;
                    state = inited ? "引擎就绪" : ("引擎初始化失败(" + err + ")");
                    note("回调 onTtsInited ok=" + (ok != 0) + " err=" + err);
                    break;
                }
                case 1:
                    note("回调 开始播放");
                    break;
                case 2:
                    busy=false;
                    state = "播报完成";
                    note("回调 播放完成");
                    break;
                case 3:
                    busy=false;
                    note("回调 被打断");
                    break;
                case 7:
                    note("回调 暂停");
                    break;
                case 8:
                    note("回调 恢复");
                    break;
                default:
                    note("回调 code=" + code);
                    break;
            }
            if (reply != null) {
                try {
                    reply.writeNoException();
                } catch (Throwable ignored) {
                }
            }
            return true;
        }
    }
}
