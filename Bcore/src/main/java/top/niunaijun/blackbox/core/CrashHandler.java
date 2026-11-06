package top.niunaijun.blackbox.core;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.utils.ErrorLogCapturer;


public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    public static void create() {
        new CrashHandler();
    }

    public CrashHandler() {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            ErrorLogCapturer.logCrash("CRASH_HANDLER",
                    "Uncaught exception on thread " + (t != null ? t.getName() : "unknown"), e);
        } catch (Throwable ignored) {
        }
        if (BlackBoxCore.get().getExceptionHandler() != null) {
            BlackBoxCore.get().getExceptionHandler().uncaughtException(t, e);
        }
        if (mDefaultHandler != null && mDefaultHandler != this) {
            mDefaultHandler.uncaughtException(t, e);
        }
    }
}
