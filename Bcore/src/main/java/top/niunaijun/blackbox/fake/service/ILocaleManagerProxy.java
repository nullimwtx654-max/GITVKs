package top.niunaijun.blackbox.fake.service;

import android.os.Build;
import android.os.IBinder;
import android.os.LocaleList;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.Slog;

public class ILocaleManagerProxy extends BinderInvocationStub {
    public static final String TAG = "ILocaleManagerProxy";
    public static final String LOCALE_SERVICE = "locale";

    public ILocaleManagerProxy() {
        super(BRServiceManager.get().getService(LOCALE_SERVICE));
    }

    @Override
    protected Object getWho() {
        try {
            IBinder binder = BRServiceManager.get().getService(LOCALE_SERVICE);
            if (binder == null) {
                return null;
            }
            Class<?> stubClass = Class.forName("android.app.ILocaleManager$Stub");
            Method asInterface = stubClass.getMethod("asInterface", IBinder.class);
            return asInterface.invoke(null, binder);
        } catch (Throwable t) {
            Slog.w(TAG, "Failed to resolve ILocaleManager$Stub: " + t.getMessage());
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(LOCALE_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            MethodParameterUtils.replaceAllAppPkg(args);
            return super.invoke(proxy, method, args);
        } catch (Throwable t) {
            Slog.w(TAG, "Intercepted exception in ILocaleManager." + method.getName() + ": " + t.getMessage());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && LocaleList.class.isAssignableFrom(method.getReturnType())) {
                return LocaleList.getEmptyLocaleList();
            }
            return null;
        }
    }
}
