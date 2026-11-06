package top.niunaijun.blackbox.fake.service;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import black.android.app.ActivityThreadActivityClientRecordContext;
import black.android.app.BRActivityClient;
import black.android.app.BRActivityClientActivityClientControllerSingleton;
import black.android.app.BRActivityManagerNative;
import black.android.app.BRActivityThread;
import black.android.app.BRActivityThreadActivityClientRecord;
import black.android.app.BRActivityThreadCreateServiceData;
import black.android.app.BRActivityThreadH;
import black.android.app.BRIActivityManager;
import black.android.app.servertransaction.BRClientTransaction;
import black.android.app.servertransaction.BRLaunchActivityItem;
import black.android.app.servertransaction.LaunchActivityItemContext;
import black.android.os.BRHandler;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.IInjectHook;
import top.niunaijun.blackbox.proxy.ProxyManifest;
import top.niunaijun.blackbox.proxy.record.ProxyActivityRecord;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.BuildCompat;

public class HCallbackProxy implements IInjectHook, Handler.Callback {
    public static final String TAG = "HCallbackStub";
    private Handler.Callback mOtherCallback;
    private final AtomicBoolean mBeing = new AtomicBoolean(false);

    private Handler.Callback getHCallback() {
        return BRHandler.get(getH()).mCallback();
    }

    private Handler getH() {
        Object currentActivityThread = BlackBoxCore.mainThread();
        return BRActivityThread.get(currentActivityThread).mH();
    }

    @Override
    public void injectHook() {
        if (!BlackBoxCore.get().isBlackProcess()) {
            return;
        }
        mOtherCallback = getHCallback();
        if (mOtherCallback != null && (mOtherCallback == this || mOtherCallback.getClass().getName().equals(this.getClass().getName()))) {
            mOtherCallback = null;
        }
        BRHandler.get(getH())._set_mCallback(this);
    }

    @Override
    public boolean isBadEnv() {
        if (!BlackBoxCore.get().isBlackProcess()) {
            return false;
        }
        Handler.Callback hCallback = getHCallback();
        return hCallback != null && hCallback != this;
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (!mBeing.getAndSet(true)) {
            try {
                if (BuildCompat.isPie()) {
                    if (msg.what == BRActivityThreadH.get().EXECUTE_TRANSACTION()) {
                        if (handleLaunchActivity(msg.obj)) {
                            getH().sendMessageAtFrontOfQueue(Message.obtain(msg));
                            return true;
                        }
                    }
                } else {
                    if (msg.what == BRActivityThreadH.get().LAUNCH_ACTIVITY()) {
                        if (handleLaunchActivity(msg.obj)) {
                            getH().sendMessageAtFrontOfQueue(Message.obtain(msg));
                            return true;
                        }
                    }
                }
                if (msg.what == BRActivityThreadH.get().CREATE_SERVICE()) {
                    return handleCreateService(msg.obj);
                }
                if (mOtherCallback != null) {
                    return mOtherCallback.handleMessage(msg);
                }
                return false;
            } finally {
                mBeing.set(false);
            }
        }
        return false;
    }

    private Object getLaunchActivityItem(Object clientTransaction) {
        if (clientTransaction == null) {
            return null;
        }
        Class<?> launchItemClass = BRLaunchActivityItem.getRealClass();
        String launchItemName = launchItemClass != null ? launchItemClass.getName() : "android.app.servertransaction.LaunchActivityItem";

        try {
            List<Object> mActivityCallbacks = BRClientTransaction.get(clientTransaction).mActivityCallbacks();
            if (mActivityCallbacks != null) {
                for (Object obj : mActivityCallbacks) {
                    if (obj != null && launchItemName.equals(obj.getClass().getCanonicalName())) {
                        return obj;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            Method getItems = clientTransaction.getClass().getDeclaredMethod("getTransactionItems");
            getItems.setAccessible(true);
            Object itemsObj = getItems.invoke(clientTransaction);
            if (itemsObj instanceof List) {
                for (Object obj : (List<?>) itemsObj) {
                    if (obj != null && launchItemName.equals(obj.getClass().getCanonicalName())) {
                        return obj;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            Field f = clientTransaction.getClass().getDeclaredField("mTransactionItems");
            f.setAccessible(true);
            Object itemsObj = f.get(clientTransaction);
            if (itemsObj instanceof List) {
                for (Object obj : (List<?>) itemsObj) {
                    if (obj != null && launchItemName.equals(obj.getClass().getCanonicalName())) {
                        return obj;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private IBinder extractActivityToken(Object clientTransaction, Object launchActivityItem) {
        IBinder token = null;
        try {
            token = BRClientTransaction.get(clientTransaction).mActivityToken();
        } catch (Throwable ignored) {
        }
        if (token == null && launchActivityItem != null) {
            try {
                Method m = launchActivityItem.getClass().getMethod("getActivityToken");
                m.setAccessible(true);
                token = (IBinder) m.invoke(launchActivityItem);
            } catch (Throwable ignored) {
            }
        }
        if (token == null && launchActivityItem != null) {
            try {
                Field f = launchActivityItem.getClass().getDeclaredField("mActivityToken");
                f.setAccessible(true);
                token = (IBinder) f.get(launchActivityItem);
            } catch (Throwable ignored) {
            }
        }
        return token;
    }

    private static void setLaunchActivityFields(Object r, Intent targetIntent, ActivityInfo activityInfo) {
        try {
            LaunchActivityItemContext ctx = BRLaunchActivityItem.get(r);
            ctx._set_mIntent(targetIntent);
            ctx._set_mInfo(activityInfo);
        } catch (Throwable ignored) {
        }
        try {
            Field fIntent = r.getClass().getDeclaredField("mIntent");
            fIntent.setAccessible(true);
            fIntent.set(r, targetIntent);
        } catch (Throwable ignored) {
        }
        try {
            Field fInfo = r.getClass().getDeclaredField("mInfo");
            fInfo.setAccessible(true);
            fInfo.set(r, activityInfo);
        } catch (Throwable ignored) {
        }
    }

    private boolean handleLaunchActivity(Object client) {
        Object r;
        if (BuildCompat.isPie()) {
            r = getLaunchActivityItem(client);
        } else {
            r = client;
        }
        if (r == null)
            return false;

        Intent intent = null;
        IBinder token = null;
        if (BuildCompat.isPie()) {
            try {
                intent = BRLaunchActivityItem.get(r).mIntent();
            } catch (Throwable ignored) {
            }
            if (intent == null) {
                try {
                    Field fIntent = r.getClass().getDeclaredField("mIntent");
                    fIntent.setAccessible(true);
                    intent = (Intent) fIntent.get(r);
                } catch (Throwable ignored) {
                }
            }
            token = extractActivityToken(client, r);
        } else {
            ActivityThreadActivityClientRecordContext clientRecordContext = BRActivityThreadActivityClientRecord.get(r);
            intent = clientRecordContext.intent();
            token = clientRecordContext.token();
        }

        if (intent == null)
            return false;

        ProxyActivityRecord stubRecord = ProxyActivityRecord.create(intent);
        ActivityInfo activityInfo = stubRecord.mActivityInfo;
        if (activityInfo != null) {
            if (BActivityThread.getAppConfig() == null) {
                BlackBoxCore.getBActivityManager().restartProcess(activityInfo.packageName, activityInfo.processName, stubRecord.mUserId);

                Intent launchIntent = stubRecord.mTarget;
                if (launchIntent == null) {
                    launchIntent = BlackBoxCore.getBPackageManager().getLaunchIntentForPackage(activityInfo.packageName, stubRecord.mUserId);
                }
                if (BActivityThread.getAppConfig() == null) {
                    intent.setExtrasClassLoader(this.getClass().getClassLoader());
                    ProxyActivityRecord.saveStub(intent, launchIntent, stubRecord.mActivityInfo, stubRecord.mActivityRecord, stubRecord.mUserId);
                    if (BuildCompat.isPie()) {
                        setLaunchActivityFields(r, intent, activityInfo);
                    } else {
                        ActivityThreadActivityClientRecordContext clientRecordContext = BRActivityThreadActivityClientRecord.get(r);
                        clientRecordContext._set_intent(intent);
                        clientRecordContext._set_activityInfo(activityInfo);
                    }
                    return true;
                }
            }

            if (!BActivityThread.currentActivityThread().isInit()) {
                BActivityThread.currentActivityThread().bindApplication(activityInfo.packageName,
                        activityInfo.processName);
                if (!BActivityThread.currentActivityThread().isInit()) {
                    return true;
                }
            }

            try {
                int taskId = BRIActivityManager.get(BRActivityManagerNative.get().getDefault()).getTaskForActivity(token, false);
                BlackBoxCore.getBActivityManager().onActivityCreated(taskId, token, stubRecord.mActivityRecord);
            } catch (Throwable t) {
                Slog.w(TAG, "onActivityCreated warning: " + t.getMessage());
            }

            if (stubRecord.mTarget != null && BActivityThread.getApplication() != null) {
                try {
                    stubRecord.mTarget.setExtrasClassLoader(BActivityThread.getApplication().getClassLoader());
                } catch (Throwable ignored) {
                }
            }

            if (BuildCompat.isPie()) {
                setLaunchActivityFields(r, stubRecord.mTarget, activityInfo);
                if (BuildCompat.isS()) {
                    try {
                        Object record = BRActivityThread.get(BlackBoxCore.mainThread()).getLaunchingActivity(token);
                        if (record != null) {
                            ActivityThreadActivityClientRecordContext clientRecordContext = BRActivityThreadActivityClientRecord.get(record);
                            clientRecordContext._set_intent(stubRecord.mTarget);
                            clientRecordContext._set_activityInfo(activityInfo);
                            clientRecordContext._set_packageInfo(BActivityThread.currentActivityThread().getPackageInfo());
                        }
                    } catch (Throwable ignored) {
                    }
                    checkActivityClient();
                }
            } else {
                ActivityThreadActivityClientRecordContext clientRecordContext = BRActivityThreadActivityClientRecord.get(r);
                clientRecordContext._set_intent(stubRecord.mTarget);
                clientRecordContext._set_activityInfo(activityInfo);
                try {
                    clientRecordContext._set_packageInfo(BActivityThread.currentActivityThread().getPackageInfo());
                } catch (Throwable ignored) {
                }
            }
            top.niunaijun.blackbox.utils.ErrorLogCapturer.logStartup("ACTIVITY_LAUNCH",
                    "Launching activity " + activityInfo.name + " in package " + activityInfo.packageName);
        }
        return false;
    }

    private boolean handleCreateService(Object data) {
        if (BActivityThread.getAppConfig() != null) {
            String appPackageName = BActivityThread.getAppPackageName();
            assert appPackageName != null;

            ServiceInfo serviceInfo = BRActivityThreadCreateServiceData.get(data).info();
            if (!serviceInfo.name.equals(ProxyManifest.getProxyService(BActivityThread.getAppPid()))
                    && !serviceInfo.name.equals(ProxyManifest.getProxyJobService(BActivityThread.getAppPid()))) {
                Slog.d(TAG, "handleCreateService: " + data);
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(appPackageName, serviceInfo.name));
                BlackBoxCore.getBActivityManager().startService(intent, null, false, BActivityThread.getUserId());
                return true;
            }
        }
        return false;
    }

    private void checkActivityClient() {
        try {
            Object activityClientController = BRActivityClient.get().getActivityClientController();
            if (!(activityClientController instanceof Proxy)) {
                IActivityClientProxy iActivityClientProxy = new IActivityClientProxy(activityClientController);
                iActivityClientProxy.onlyProxy(true);
                iActivityClientProxy.injectHook();
                Object instance = BRActivityClient.get().getInstance();
                Object o = BRActivityClient.get(instance).INTERFACE_SINGLETON();
                BRActivityClientActivityClientControllerSingleton.get(o)._set_mKnownInstance(iActivityClientProxy.getProxyInvocation());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
