package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.content.pm.PackageManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import black.android.app.BRActivityThread;
import black.android.os.BRServiceManager;
import black.android.permission.BRIPermissionManagerStub;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.fake.service.base.PkgMethodProxy;
import top.niunaijun.blackbox.fake.service.base.ValueMethodProxy;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.Reflector;
import top.niunaijun.blackbox.utils.compat.BuildCompat;


public class IPermissionManagerProxy extends BinderInvocationStub {
    public static final String TAG = "IPermissionManagerProxy";

    private static final String P = "permissionmgr";

    public IPermissionManagerProxy() {
        super(BRServiceManager.get().getService(P));
    }

    @Override
    protected Object getWho() {
        return BRIPermissionManagerStub.get().asInterface(BRServiceManager.get().getService(P));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("permissionmgr");
        try {
            BRActivityThread.get()._set_sPermissionManager(proxyInvocation);
        } catch (Throwable ignored) {
        }
        disablePermissionCaches();
        try {
            Context ctx = BlackBoxCore.getContext();
            if (ctx != null) {
                Object permMgr = ctx.getSystemService("permission");
                if (permMgr != null) {
                    Reflector.with(permMgr).field("mPermissionManager").set(proxyInvocation);
                }
                PackageManager pm = ctx.getPackageManager();
                if (pm != null) {
                    try {
                        Reflector.with(pm).field("mPermissionManager").set(proxyInvocation);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void disablePermissionCaches() {
        try {
            Class<?> pmClass = Class.forName("android.permission.PermissionManager");
            for (String methodName : new String[]{
                    "disablePermissionCache",
                    "disablePackageNamePermissionCache"
            }) {
                try {
                    Method m = pmClass.getDeclaredMethod(methodName);
                    m.setAccessible(true);
                    m.invoke(null);
                } catch (Throwable ignored) {
                }
            }
            for (String fieldName : new String[]{
                    "sPermissionCache",
                    "sPackageNamePermissionCache"
            }) {
                try {
                    Field f = pmClass.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object cache = f.get(null);
                    if (cache != null) {
                        Method disable = cache.getClass().getMethod("disableForCurrentProcess");
                        disable.setAccessible(true);
                        disable.invoke(cache);
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if (name.startsWith("check") && name.contains("Permission")) {
            return PackageManager.PERMISSION_GRANTED;
        }
        MethodParameterUtils.replaceAllAppPkg(args);
        return super.invoke(proxy, method, args);
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
        addMethodHook(new ValueMethodProxy("addPermissionAsync", true));
        addMethodHook(new ValueMethodProxy("addPermission", true));
        addMethodHook(new ValueMethodProxy("performDexOpt", true));
        addMethodHook(new ValueMethodProxy("performDexOptIfNeeded", false));
        addMethodHook(new ValueMethodProxy("performDexOptSecondary", true));
        addMethodHook(new ValueMethodProxy("addOnPermissionsChangeListener", 0));
        addMethodHook(new ValueMethodProxy("removeOnPermissionsChangeListener", 0));
        addMethodHook(new ValueMethodProxy("checkDeviceIdentifierAccess", true));
        addMethodHook(new PkgMethodProxy("shouldShowRequestPermissionRationale"));
        if (BuildCompat.isOreo()) {
            addMethodHook(new ValueMethodProxy("notifyDexLoad", 0));
            addMethodHook(new ValueMethodProxy("notifyPackageUse", 0));
            addMethodHook(new ValueMethodProxy("setInstantAppCookie", false));
            addMethodHook(new ValueMethodProxy("isInstantApp", false));
        }
    }

    @ProxyMethod("checkPermission")
    public static class CheckPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return PackageManager.PERMISSION_GRANTED;
        }
    }

    @ProxyMethod("checkUidPermission")
    public static class CheckUidPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
