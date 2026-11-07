package top.niunaijun.blackbox.fake.delegate;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import black.android.app.BRActivity;
import black.android.app.BRActivityThread;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.HookManager;
import top.niunaijun.blackbox.fake.hook.IInjectHook;
import top.niunaijun.blackbox.fake.service.HCallbackProxy;
import top.niunaijun.blackbox.fake.service.IActivityClientProxy;
import top.niunaijun.blackbox.utils.HackAppUtils;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.ActivityCompat;
import top.niunaijun.blackbox.utils.compat.ActivityManagerCompat;
import top.niunaijun.blackbox.utils.compat.ContextCompat;

public final class AppInstrumentation extends BaseInstrumentationDelegate implements IInjectHook {

    private static final String TAG = AppInstrumentation.class.getSimpleName();

    private static AppInstrumentation sAppInstrumentation;

    public static AppInstrumentation get() {
        if (sAppInstrumentation == null) {
            synchronized (AppInstrumentation.class) {
                if (sAppInstrumentation == null) {
                    sAppInstrumentation = new AppInstrumentation();
                }
            }
        }
        return sAppInstrumentation;
    }

    public AppInstrumentation() {
    }

    @Override
    public void injectHook() {
        try {
            if (!BlackBoxCore.get().isBlackProcess()) {
                return;
            }
            Instrumentation mInstrumentation = getCurrInstrumentation();
            if (mInstrumentation == this || checkInstrumentation(mInstrumentation))
                return;
            mBaseInstrumentation = (Instrumentation) mInstrumentation;
            BRActivityThread.get(BlackBoxCore.mainThread())._set_mInstrumentation(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Instrumentation getCurrInstrumentation() {
        Object currentActivityThread = BlackBoxCore.mainThread();
        return BRActivityThread.get(currentActivityThread).mInstrumentation();
    }

    @Override
    public boolean isBadEnv() {
        if (!BlackBoxCore.get().isBlackProcess()) {
            return false;
        }
        return !checkInstrumentation(getCurrInstrumentation());
    }

    private boolean checkInstrumentation(Instrumentation instrumentation) {
        if (instrumentation instanceof AppInstrumentation) {
            return true;
        }
        Class<?> clazz = instrumentation.getClass();
        if (Instrumentation.class.equals(clazz)) {
            return false;
        }
        do {
            assert clazz != null;
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (Instrumentation.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    try {
                        Object obj = field.get(instrumentation);
                        if ((obj instanceof AppInstrumentation)) {
                            return true;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        } while (!Instrumentation.class.equals(clazz));
        return false;
    }

    private void checkHCallback() {
        if (BlackBoxCore.get().isBlackProcess()) {
            HookManager.get().checkEnv(HCallbackProxy.class);
        }
    }

    private void checkActivity(Activity activity) {
        if (!BlackBoxCore.get().isBlackProcess()) {
            return;
        }
        Log.d(TAG, "callActivityOnCreate: " + activity.getClass().getName());
        HackAppUtils.enableQQLogOutput(activity.getPackageName(), activity.getClassLoader());
        checkHCallback();
        HookManager.get().checkEnv(IActivityClientProxy.class);
        ActivityInfo info = BRActivity.get(activity).mActivityInfo();
        ContextCompat.fix(activity);

        // Apply activity or application theme BEFORE ActivityCompat.fix(activity) calls obtainStyledAttributes
        // and BEFORE super.onCreate runs AppCompatDelegateImpl checks (fixes MainSettingsActivity crash).
        int themeId = 0;
        if (info != null) {
            themeId = info.getThemeResource();
            if (themeId == 0 && info.theme != 0) {
                themeId = info.theme;
            }
            if (themeId == 0 && info.applicationInfo != null && info.applicationInfo.theme != 0) {
                themeId = info.applicationInfo.theme;
            }
        }
        if (themeId == 0 && activity.getApplicationInfo() != null && activity.getApplicationInfo().theme != 0) {
            themeId = activity.getApplicationInfo().theme;
        }
        if (themeId != 0) {
            try {
                activity.setTheme(themeId);
            } catch (Throwable ignored) {
            }
            try {
                activity.getTheme().applyStyle(themeId, true);
            } catch (Throwable ignored) {
            }
        }

        ActivityCompat.fix(activity);
        HookManager.get().checkEnv(top.niunaijun.blackbox.fake.service.IDisplayManagerProxy.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try {
                String pkg = activity.getPackageName();
                if (BlackBoxCore.get().isSpoofDeviceSpecs()
                        && pkg != null
                        && !pkg.equals(BlackBoxCore.getHostPkg())
                        && !top.niunaijun.blackbox.core.GmsCore.isGoogleAppOrService(pkg)
                        && activity.getWindow() != null) {
                    android.view.WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
                    lp.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                    activity.getWindow().setAttributes(lp);
                }
            } catch (Throwable ignored) {
            }
        }
        if (info != null) {
            ActivityManagerCompat.setActivityOrientation(activity, info.screenOrientation);
        }
    }

    private static void configureMicroGWebView(WebView webView) {
        if (webView == null) return;
        try {
            WebSettings settings = webView.getSettings();
            if (settings != null) {
                settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                settings.setDomStorageEnabled(true);
                settings.setDatabaseEnabled(true);
                settings.setJavaScriptEnabled(true);
                String ua = settings.getUserAgentString();
                if (ua != null && (ua.contains("Android null") || ua.contains("null Build/null"))) {
                    String fixedUa = ua
                            .replace("Android null", "Android " + Build.VERSION.RELEASE)
                            .replace("null Build/null", Build.MODEL + " Build/" + Build.ID);
                    settings.setUserAgentString(fixedUa);
                }
            }
            webView.clearCache(true);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Ensures microG's LoginActivity:
     * 1. Has CheckinPreferences enabled before start() runs,
     * 2. Uses WebSettings.LOAD_NO_CACHE + DOM storage so EmbeddedSetup never fails with net::ERR_CACHE_MISS,
     * 3. Automatically retries with no-cache headers if an error page is ever encountered, and
     * 4. Makes the WebView visible once accounts.google.com has loaded the sign-in screen.
     */
    private void assistMicroGLoginActivity(final Activity activity) {
        if (activity == null) return;
        String className = activity.getClass().getName();
        if (!"org.microg.gms.auth.login.LoginActivity".equals(className)) {
            return;
        }
        try {
            top.niunaijun.blackbox.core.GmsCore.ensureMicroGConfigured(BActivityThread.getUserId());
            try {
                Class<?> checkinPrefsClass = Class.forName(
                        "org.microg.gms.checkin.CheckinPreferences",
                        false,
                        activity.getClassLoader()
                );
                Method setEnabled = checkinPrefsClass.getDeclaredMethod("setEnabled", Context.class, boolean.class);
                setEnabled.setAccessible(true);
                setEnabled.invoke(null, activity, true);
            } catch (Throwable ignored) {
            }

            try {
                Field webViewField = activity.getClass().getDeclaredField("webView");
                webViewField.setAccessible(true);
                WebView webView = (WebView) webViewField.get(activity);
                configureMicroGWebView(webView);
            } catch (Throwable ignored) {
            }

            final Handler handler = new Handler(Looper.getMainLooper());
            final Runnable[] watcher = new Runnable[1];
            watcher[0] = new Runnable() {
                private int ticks = 0;
                private int cacheMissRetries = 0;
                private String lastAttemptedUrl = null;

                @Override
                public void run() {
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        return;
                    }
                    ticks++;
                    try {
                        Field webViewField = activity.getClass().getDeclaredField("webView");
                        webViewField.setAccessible(true);
                        WebView webView = (WebView) webViewField.get(activity);
                        if (webView != null) {
                            WebSettings settings = webView.getSettings();
                            if (settings != null && settings.getCacheMode() != WebSettings.LOAD_NO_CACHE) {
                                configureMicroGWebView(webView);
                            }

                            String url = webView.getUrl();
                            String title = webView.getTitle();
                            int progress = webView.getProgress();

                            if (url != null && url.contains("accounts.google.com")) {
                                lastAttemptedUrl = url;
                            }

                            boolean isErrorPage = (url != null && url.startsWith("chrome-error://"))
                                    || (title != null && (title.contains("Webpage not available") || title.contains("ERR_CACHE_MISS")));

                            if (isErrorPage && cacheMissRetries < 4) {
                                cacheMissRetries++;
                                webView.setVisibility(View.INVISIBLE);
                                configureMicroGWebView(webView);
                                Map<String, String> noCacheHeaders = new HashMap<>();
                                noCacheHeaders.put("Cache-Control", "no-cache, no-store, must-revalidate");
                                noCacheHeaders.put("Pragma", "no-cache");
                                if (lastAttemptedUrl != null && lastAttemptedUrl.contains("accounts.google.com")) {
                                    webView.loadUrl(lastAttemptedUrl, noCacheHeaders);
                                } else {
                                    try {
                                        Method loadLoginPage = activity.getClass().getDeclaredMethod("loadLoginPage");
                                        loadLoginPage.setAccessible(true);
                                        loadLoginPage.invoke(activity);
                                    } catch (Throwable ignored) {
                                    }
                                }
                            } else if (!isErrorPage
                                    && webView.getVisibility() != View.VISIBLE
                                    && url != null
                                    && url.contains("accounts.google.com")
                                    && progress >= 80) {
                                String cookies = null;
                                try {
                                    cookies = CookieManager.getInstance().getCookie(url);
                                } catch (Throwable ignored) {
                                }
                                boolean hasOauthToken = cookies != null && cookies.contains("oauth_token=");
                                if (!hasOauthToken && !url.contains("#close")) {
                                    Slog.d(TAG, "Making microG LoginActivity WebView visible for URL: " + url);
                                    webView.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                    if (ticks < 180) {
                        handler.postDelayed(this, 500);
                    }
                }
            };
            handler.postDelayed(watcher[0], 400);
        } catch (Throwable t) {
            Slog.w(TAG, "assistMicroGLoginActivity error: " + t.getMessage());
        }
    }

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (BlackBoxCore.get().isBlackProcess()) {
            ContextCompat.fix(context);
        }
        return super.newApplication(cl, className, context);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
        checkActivity(activity);
        super.callActivityOnCreate(activity, icicle, persistentState);
        assistMicroGLoginActivity(activity);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        checkActivity(activity);
        super.callActivityOnCreate(activity, icicle);
        assistMicroGLoginActivity(activity);
    }

    @Override
    public void callApplicationOnCreate(Application app) {
        checkHCallback();
        super.callApplicationOnCreate(app);
    }

    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return super.newActivity(cl, className, intent);
        } catch (ClassNotFoundException e) {
            return mBaseInstrumentation.newActivity(cl, className, intent);
        }
    }
}
