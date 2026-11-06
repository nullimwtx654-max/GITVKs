package top.niunaijun.blackbox.utils;

import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;

/**
 * Multi-process startup & runtime error log capturer for Clone Box and all cloned apps.
 * Persists startup milestones, handled/unhandled exceptions, Slog.e events, and process logcat
 * errors into a shared host file so users can inspect startup or runtime failures in-app.
 */
public final class ErrorLogCapturer {
    private static final String LOG_FILE_NAME = "clonebox_error_logs.txt";
    private static final long MAX_LOG_BYTES = 512 * 1024L; // 512 KB cap
    private static final AtomicBoolean sInitialized = new AtomicBoolean(false);
    private static final AtomicBoolean sLogcatStarted = new AtomicBoolean(false);
    private static final AtomicBoolean sWriting = new AtomicBoolean(false);
    private static volatile File sLogFile;

    private ErrorLogCapturer() {
    }

    public static void init(Context context) {
        if (context == null) return;
        try {
            File filesDir = context.getFilesDir();
            if (filesDir != null) {
                if (!filesDir.exists()) {
                    filesDir.mkdirs();
                }
                sLogFile = new File(filesDir, LOG_FILE_NAME);
            }
        } catch (Throwable ignored) {
        }

        if (sInitialized.compareAndSet(false, true)) {
            installUncaughtExceptionHook();
            logStartup("PROCESS_INIT", "Process initialized (SDK=" + Build.VERSION.SDK_INT
                    + ", release=" + Build.VERSION.RELEASE
                    + ", model=" + Build.MODEL + ")");
            startLogcatErrorWatcher();
        }
    }

    private static File resolveLogFile() {
        if (sLogFile != null) {
            return sLogFile;
        }
        try {
            Context ctx = BlackBoxCore.getContext();
            if (ctx != null && ctx.getFilesDir() != null) {
                File dir = ctx.getFilesDir();
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                sLogFile = new File(dir, LOG_FILE_NAME);
                return sLogFile;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void installUncaughtExceptionHook() {
        try {
            final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
            if (previous != null && previous.getClass().getName().contains("ErrorLogCapturer")) {
                return;
            }
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                logCrash("UNCAUGHT_EXCEPTION", "Thread=" + (thread != null ? thread.getName() : "unknown"), throwable);
                if (previous != null) {
                    previous.uncaughtException(thread, throwable);
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static void startLogcatErrorWatcher() {
        if (!sLogcatStarted.compareAndSet(false, true)) {
            return;
        }
        Thread watcher = new Thread(() -> {
            java.lang.Process proc = null;
            try {
                int myPid = Process.myPid();
                proc = new ProcessBuilder("logcat", "-v", "time", "*:E").redirectErrorStream(true).start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    String pidToken = "(" + myPid + ")";
                    String pidTokenPadded = "( " + myPid + ")";
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("BBinder_init")
                                || line.contains("VivoJsonResourceManager")
                                || line.contains("call to OpenGL ES API with no current context")
                                || line.contains("com.facebook.lite.provider.InstallReferrerProvider")
                                || line.contains("Failed to find provider info for com.google.android.gms.chimera")) {
                            continue;
                        }
                        if (line.contains(pidToken)
                                || line.contains(pidTokenPadded)
                                || line.contains("AndroidRuntime")
                                || line.contains("FATAL EXCEPTION")
                                || line.contains("Fatal signal")
                                || line.contains("GmsAuth")
                                || line.contains("ActivityStack")
                                || line.contains("BActivityThread")) {
                            appendRawLine("[LOGCAT] " + line);
                        }
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                if (proc != null) {
                    try {
                        proc.destroy();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }, "CloneBox-ErrorLogWatcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    private static String buildContextPrefix(String category) {
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
        int pid = Process.myPid();
        String pkg = "host";
        String proc = "main";
        int userId = 0;
        try {
            if (BActivityThread.isThreadInit()) {
                String appPkg = BActivityThread.getAppPackageName();
                String appProc = BActivityThread.getAppProcessName();
                if (appPkg != null && !appPkg.isEmpty()) pkg = appPkg;
                if (appProc != null && !appProc.isEmpty()) proc = appProc;
                userId = BActivityThread.getUserId();
            }
        } catch (Throwable ignored) {
        }
        return String.format(Locale.US, "[%s] [%s] [pid=%d user=%d pkg=%s proc=%s] ",
                ts, category, pid, userId, pkg, proc);
    }

    public static void logStartup(String stage, String detail) {
        appendRawLine(buildContextPrefix("STARTUP:" + stage) + detail);
    }

    public static void logError(String tag, String msg, Throwable tr) {
        if (tag != null && tag.contains("ErrorLogCapturer")) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(buildContextPrefix("ERROR:" + (tag != null ? tag : "LOG")));
        if (msg != null) {
            sb.append(msg);
        }
        if (tr != null) {
            sb.append("\n").append(formatThrowable(tr));
        }
        appendRawLine(sb.toString());
    }

    public static void logCrash(String stage, String msg, Throwable tr) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==================== CRASH / FATAL ERROR ====================\n");
        sb.append(buildContextPrefix(stage));
        if (msg != null) {
            sb.append(msg);
        }
        if (tr != null) {
            sb.append("\n").append(formatThrowable(tr));
        }
        sb.append("\n=============================================================");
        appendRawLine(sb.toString());
    }

    private static String formatThrowable(Throwable tr) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            tr.printStackTrace(pw);
            pw.flush();
            return sw.toString().trim();
        } catch (Throwable e) {
            return Log.getStackTraceString(tr);
        }
    }

    private static synchronized void appendRawLine(String text) {
        if (text == null || text.isEmpty()) return;
        if (!sWriting.compareAndSet(false, true)) {
            return;
        }
        try {
            File file = resolveLogFile();
            if (file == null) return;
            rotateIfNeeded(file);
            try (FileOutputStream fos = new FileOutputStream(file, true)) {
                fos.write((text + "\n").getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
        } catch (Throwable ignored) {
        } finally {
            sWriting.set(false);
        }
    }

    private static void rotateIfNeeded(File file) {
        try {
            if (file.exists() && file.length() > MAX_LOG_BYTES) {
                byte[] all = new byte[(int) file.length()];
                try (FileInputStream fis = new FileInputStream(file)) {
                    int read = fis.read(all);
                    if (read > 0) {
                        int keep = (int) (MAX_LOG_BYTES / 2);
                        int offset = Math.max(0, read - keep);
                        try (FileOutputStream fos = new FileOutputStream(file, false)) {
                            fos.write("--- [Log truncated to latest 256 KB] ---\n".getBytes(StandardCharsets.UTF_8));
                            fos.write(all, offset, read - offset);
                            fos.flush();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static String readLogs() {
        try {
            File file = resolveLogFile();
            if (file == null || !file.exists() || file.length() == 0) {
                return "No startup or runtime errors captured yet.\n\n"
                        + "Device: " + Build.MANUFACTURER + " " + Build.MODEL
                        + " (Android " + Build.VERSION.RELEASE + ", API " + Build.VERSION.SDK_INT + ")\n"
                        + "Status: Error Log Capturer is active and monitoring all virtual processes (:p0..:p99).";
            }
            byte[] buf = new byte[(int) Math.min(file.length(), MAX_LOG_BYTES)];
            try (FileInputStream fis = new FileInputStream(file)) {
                int n = fis.read(buf);
                if (n <= 0) {
                    return "No startup or runtime errors captured yet.";
                }
                return new String(buf, 0, n, StandardCharsets.UTF_8);
            }
        } catch (Throwable t) {
            return "Failed to read error logs: " + t.getMessage();
        }
    }

    public static void clearLogs() {
        try {
            File file = resolveLogFile();
            if (file != null && file.exists()) {
                try (FileOutputStream fos = new FileOutputStream(file, false)) {
                    fos.write(new byte[0]);
                    fos.flush();
                }
            }
            logStartup("LOG_CLEARED", "Error log history cleared by user");
        } catch (Throwable ignored) {
        }
    }
}
