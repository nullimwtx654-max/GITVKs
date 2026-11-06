package top.niunaijun.blackbox.fake.delegate;

import android.content.IIntentReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import black.android.app.BRLoadedApkReceiverDispatcherInnerReceiver;
import black.android.content.BRIIntentReceiver;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.proxy.record.ProxyBroadcastRecord;


public class InnerReceiverDelegate extends IIntentReceiver.Stub {
    public static final String TAG = "InnerReceiverDelegate";

    private static final Map<IBinder, InnerReceiverDelegate> sInnerReceiverDelegate = new ConcurrentHashMap<>();
    private final IIntentReceiver mIntentReceiver;

    private InnerReceiverDelegate(IIntentReceiver iIntentReceiver) {
        this.mIntentReceiver = iIntentReceiver;
    }

    public static InnerReceiverDelegate getDelegate(IBinder iBinder) {
        return sInnerReceiverDelegate.get(iBinder);
    }

    public static IIntentReceiver createProxy(IIntentReceiver base) {
        if (base instanceof InnerReceiverDelegate) {
            return base;
        }
        final IBinder iBinder = base.asBinder();
        InnerReceiverDelegate delegate = sInnerReceiverDelegate.get(iBinder);
        if (delegate == null) {
            try {
                iBinder.linkToDeath(new DeathRecipient() {
                    @Override
                    public void binderDied() {
                        sInnerReceiverDelegate.remove(iBinder);
                        iBinder.unlinkToDeath(this, 0);
                    }
                }, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            delegate = new InnerReceiverDelegate(base);
            sInnerReceiverDelegate.put(iBinder, delegate);
        }
        return delegate;
    }

    @Override
    public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
        if (intent == null) {
            return;
        }
        ClassLoader appClassLoader = BlackBoxCore.getApplication() != null
                ? BlackBoxCore.getApplication().getClassLoader()
                : BlackBoxCore.getContext().getClassLoader();
        intent.setExtrasClassLoader(appClassLoader);
        ProxyBroadcastRecord proxyBroadcastRecord = ProxyBroadcastRecord.create(intent);
        Intent perIntent;
        if (proxyBroadcastRecord.mIntent != null) {
            proxyBroadcastRecord.mIntent.setExtrasClassLoader(appClassLoader);
            perIntent = proxyBroadcastRecord.mIntent;
        } else {
            perIntent = intent;
        }
        IIntentReceiver iIntentReceiver = mIntentReceiver;
        if (iIntentReceiver != null) {
            try {
                WeakReference<?> dispatcherRef = BRLoadedApkReceiverDispatcherInnerReceiver.get(iIntentReceiver).mDispatcher();
                if (dispatcherRef != null && dispatcherRef.get() == null) {
                    return;
                }
            } catch (Throwable ignored) {
            }
            BRIIntentReceiver.get(iIntentReceiver).performReceive(perIntent, resultCode, data, extras, ordered, sticky, sendingUser);
        }
    }
}
