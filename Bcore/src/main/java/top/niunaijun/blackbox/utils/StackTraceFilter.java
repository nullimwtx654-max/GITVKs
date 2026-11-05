package top.niunaijun.blackbox.utils;

public class StackTraceFilter {
    private static boolean sInstalled = false;

    static {
        install();
    }

    public static synchronized void install() {
        if (sInstalled) {
            return;
        }
        sInstalled = true;
        try {
            final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                try {
                    sanitizeThrowable(e);
                } catch (Throwable ignored) {
                }
                if (previous != null) {
                    previous.uncaughtException(t, e);
                }
            });
        } catch (Throwable ignored) {
        }
    }

    public static void sanitizeThrowable(Throwable t) {
        Throwable curr = t;
        int depth = 0;
        while (curr != null && depth < 10) {
            StackTraceElement[] original = curr.getStackTrace();
            if (original != null) {
                curr.setStackTrace(filterStackTrace(original));
            }
            curr = curr.getCause();
            depth++;
        }
    }

    public static StackTraceElement[] filterStackTrace(StackTraceElement[] stack) {
        if (stack == null) {
            return new StackTraceElement[0];
        }
        return java.util.Arrays.stream(stack)
                .filter(element -> element != null && !isSuspicious(element.getClassName()))
                .toArray(StackTraceElement[]::new);
    }

    public static boolean isSuspicious(String className) {
        if (className == null) return false;
        String lower = className.toLowerCase();
        return lower.contains("xposed")
                || lower.contains("lsposed")
                || lower.contains("edxposed")
                || lower.contains("epic")
                || lower.contains("sandhook")
                || lower.contains("dobby")
                || lower.contains("pine")
                || lower.contains("whale")
                || lower.contains("virtual")
                || lower.contains("blackbox")
                || lower.contains("niunaijun")
                || lower.contains("magisk")
                || lower.contains("zygisk")
                || lower.contains("riru")
                || lower.contains("frida")
                || lower.contains("substrate")
                || lower.contains("hook");
    }
}
