package top.niunaijun.blackbox.fake.service;

import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Build;
import android.os.IInterface;
import android.view.Display;
import android.view.DisplayCutout;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;

import black.android.hardware.display.BRDisplayManagerGlobal;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.GmsCore;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.Slog;


public class IDisplayManagerProxy extends ClassInvocationStub {
    public static final String TAG = "IDisplayManagerProxy";

    public IDisplayManagerProxy() {
    }

    @Override
    protected Object getWho() {
        return BRDisplayManagerGlobal.get(BRDisplayManagerGlobal.get().getInstance()).mDm();
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        Object dmg = BRDisplayManagerGlobal.get().getInstance();
        BRDisplayManagerGlobal.get(dmg)._set_mDm(getProxyInvocation());
        try {
            Field cacheField = dmg.getClass().getDeclaredField("mDisplayInfoCache");
            cacheField.setAccessible(true);
            Object cache = cacheField.get(dmg);
            if (cache != null) {
                Method clearMethod = cache.getClass().getMethod("clear");
                clearMethod.invoke(cache);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean isBadEnv() {
        Object dmg = BRDisplayManagerGlobal.get().getInstance();
        IInterface mDm = BRDisplayManagerGlobal.get(dmg).mDm();
        return mDm != getProxyInvocation();
    }

    @ProxyMethod("createVirtualDisplay")
    public static class CreateVirtualDisplay extends MethodHook {

        @Override
        protected String getMethodName() {
            return "createVirtualDisplay";
        }

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getDisplayInfo")
    public static class GetDisplayInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object displayInfo = method.invoke(who, args);
            if (displayInfo == null) {
                return null;
            }
            try {
                String pkg = BActivityThread.getAppPackageName();
                if (BlackBoxCore.get().isSpoofDeviceSpecs()
                        && pkg != null
                        && !pkg.isEmpty()
                        && !pkg.equals(BlackBoxCore.getHostPkg())
                        && !GmsCore.isGoogleAppOrService(pkg)) {
                    applyExperimentalDisplaySpoof(displayInfo);
                }
            } catch (Throwable t) {
                Slog.w(TAG, "GetDisplayInfo spoof warning: " + t.getMessage());
            }
            return displayInfo;
        }

        private static int getIntField(Object obj, String name, int fallback) {
            try {
                Field f = obj.getClass().getDeclaredField(name);
                f.setAccessible(true);
                return f.getInt(obj);
            } catch (Throwable ignored) {
                return fallback;
            }
        }

        private static void setFieldIfPresent(Object obj, String name, Object val) {
            try {
                Field f = obj.getClass().getDeclaredField(name);
                f.setAccessible(true);
                f.set(obj, val);
            } catch (Throwable ignored) {
            }
        }

        private static Display.Mode createDisplayMode(int modeId, int width, int height, float refreshRate, float altRate) {
            try {
                for (Constructor<?> ctor : Display.Mode.class.getDeclaredConstructors()) {
                    ctor.setAccessible(true);
                    Class<?>[] pts = ctor.getParameterTypes();
                    if (pts.length == 4
                            && pts[0] == int.class && pts[1] == int.class
                            && pts[2] == int.class && pts[3] == float.class) {
                        return (Display.Mode) ctor.newInstance(modeId, width, height, refreshRate);
                    }
                    if (pts.length == 6
                            && pts[0] == int.class && pts[1] == int.class
                            && pts[2] == int.class && pts[3] == float.class
                            && pts[4] == float[].class && pts[5] == int[].class) {
                        return (Display.Mode) ctor.newInstance(
                                modeId, width, height, refreshRate, new float[]{altRate}, new int[0]);
                    }
                    if (pts.length == 7
                            && pts[0] == int.class && pts[1] == int.class
                            && pts[2] == int.class && pts[3] == float.class
                            && pts[4] == float.class
                            && pts[5] == float[].class && pts[6] == int[].class) {
                        return (Display.Mode) ctor.newInstance(
                                modeId, width, height, refreshRate, refreshRate, new float[]{altRate}, new int[0]);
                    }
                    if (pts.length == 8
                            && pts[0] == int.class && pts[1] == int.class
                            && pts[2] == int.class && pts[3] == float.class
                            && pts[4] == float.class && pts[5] == boolean.class
                            && pts[6] == float[].class && pts[7] == int[].class) {
                        return (Display.Mode) ctor.newInstance(
                                modeId, width, height, refreshRate, refreshRate, false, new float[]{altRate}, new int[0]);
                    }
                }
            } catch (Throwable ignored) {
            }
            return null;
        }

        @SuppressWarnings("NewApi")
        private static DisplayCutout createPunchHoleCutout(int width) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                return null;
            }
            int holeSize = 44;
            int left = Math.max(0, (width / 2) - (holeSize / 2));
            Rect boundTop = new Rect(left, 0, left + holeSize, holeSize);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    return new DisplayCutout(
                            Insets.of(0, holeSize, 0, 0),
                            null,
                            boundTop,
                            null,
                            null
                    );
                } else {
                    return new DisplayCutout(
                            new Rect(0, holeSize, 0, 0),
                            Collections.singletonList(boundTop)
                    );
                }
            } catch (Throwable ignored) {
                return null;
            }
        }

        private static void applyExperimentalDisplaySpoof(Object displayInfo) {
            int width = getIntField(displayInfo, "logicalWidth", 1080);
            int height = getIntField(displayInfo, "logicalHeight", 2400);
            if (width <= 0) width = 1080;
            if (height <= 0) height = 2400;

            // 1. Report 120Hz supported display capability while running at 60Hz active mode
            Display.Mode mode60 = createDisplayMode(1, width, height, 60.0f, 120.0f);
            Display.Mode mode120 = createDisplayMode(2, width, height, 120.0f, 60.0f);
            if (mode60 != null && mode120 != null) {
                Display.Mode[] modes = new Display.Mode[]{mode60, mode120};
                setFieldIfPresent(displayInfo, "supportedModes", modes);
                setFieldIfPresent(displayInfo, "appsSupportedModes", modes);
                setFieldIfPresent(displayInfo, "modeId", 1);
                setFieldIfPresent(displayInfo, "defaultModeId", 1);
            }
            setFieldIfPresent(displayInfo, "refreshRateOverride", 60.0f);
            setFieldIfPresent(displayInfo, "renderFrameRate", 60.0f);
            setFieldIfPresent(displayInfo, "appVsyncOffsetNanos", 1000000L);
            setFieldIfPresent(displayInfo, "presentationDeadlineNanos", 16666666L);

            // 2. Report punch-hole DisplayCutout
            DisplayCutout punchHole = createPunchHoleCutout(width);
            if (punchHole != null) {
                setFieldIfPresent(displayInfo, "displayCutout", punchHole);
            }
        }
    }
}
