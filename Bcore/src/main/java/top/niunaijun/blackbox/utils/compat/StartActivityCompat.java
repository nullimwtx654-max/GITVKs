package top.niunaijun.blackbox.utils.compat;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class StartActivityCompat {
    private static int index = 0;
    private static int appThreadIndex;
    private static int callingPageIndex;
    private static int callingFeatureIdIndex;
    private static int intentIndex;
    private static int resolvedTypeIndex;
    private static int resultToIndex;
    private static int resultWhoIndex;
    private static int requestCodeIndex;
    private static int flagsIndex;
    private static int profilerInfoIndex;
    private static int optionsIndex;

    static {
        if (BuildCompat.isR()) {
            appThreadIndex = index++;
            callingPageIndex = index++;
            callingFeatureIdIndex = index++;
            intentIndex = index++;
            resolvedTypeIndex = index++;
            resultToIndex = index++;
            resultWhoIndex = index++;
            requestCodeIndex = index++;
            flagsIndex = index++;
            profilerInfoIndex = index++;
            optionsIndex = index++;
        } else {
            appThreadIndex = index++;
            callingPageIndex = index++;
            intentIndex = index++;
            resolvedTypeIndex = index++;
            resultToIndex = index++;
            resultWhoIndex = index++;
            requestCodeIndex = index++;
            flagsIndex = index++;
            profilerInfoIndex = index++;
            optionsIndex = index++;
        }
    }

    private static int resolveIntentPos(Object[] args) {
        if (args == null) return -1;
        if (intentIndex < args.length && args[intentIndex] instanceof Intent) {
            return intentIndex;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Intent) {
                return i;
            }
        }
        return -1;
    }

    public static Object getIApplicationThread(Object[] args) {
        if (args == null || args.length <= appThreadIndex) {
            return null;
        }
        return args[appThreadIndex];
    }

    public static String getCallingPackage(Object[] args) {
        if (args == null || args.length <= callingPageIndex) {
            return null;
        }
        return args[callingPageIndex] instanceof String ? (String) args[callingPageIndex] : null;
    }

    public static Intent getIntent(Object[] args) {
        int pos = resolveIntentPos(args);
        if (pos < 0 || pos >= args.length) {
            return null;
        }
        return (Intent) args[pos];
    }

    public static String getResolvedType(Object[] args) {
        int pos = resolveIntentPos(args);
        int idx = pos >= 0 ? pos + 1 : resolvedTypeIndex;
        if (args == null || idx < 0 || idx >= args.length) {
            return null;
        }
        return args[idx] instanceof String ? (String) args[idx] : null;
    }

    public static IBinder getResultTo(Object[] args) {
        int pos = resolveIntentPos(args);
        int idx = pos >= 0 ? pos + 2 : resultToIndex;
        if (args == null || idx < 0 || idx >= args.length) {
            return null;
        }
        return args[idx] instanceof IBinder ? (IBinder) args[idx] : null;
    }

    public static String getResultWho(Object[] args) {
        int pos = resolveIntentPos(args);
        int idx = pos >= 0 ? pos + 3 : resultWhoIndex;
        if (args == null || idx < 0 || idx >= args.length) {
            return null;
        }
        return args[idx] instanceof String ? (String) args[idx] : null;
    }

    public static int getRequestCode(Object[] args) {
        int pos = resolveIntentPos(args);
        int idx = pos >= 0 ? pos + 4 : requestCodeIndex;
        if (args == null || idx < 0 || idx >= args.length) {
            return -1;
        }
        return args[idx] instanceof Integer ? (int) args[idx] : -1;
    }

    public static int getFlags(Object[] args) {
        int pos = resolveIntentPos(args);
        int idx = pos >= 0 ? pos + 5 : flagsIndex;
        if (args == null || idx < 0 || idx >= args.length) {
            return 0;
        }
        return args[idx] instanceof Integer ? (int) args[idx] : 0;
    }

    public static Object getProfilerInfo(Object[] args) {
        int pos = resolveIntentPos(args);
        int idx = pos >= 0 ? pos + 6 : profilerInfoIndex;
        if (args == null || idx < 0 || idx >= args.length) {
            return null;
        }
        return args[idx];
    }

    public static Bundle getOptions(Object[] args) {
        int pos = resolveIntentPos(args);
        int idx = pos >= 0 ? pos + 7 : optionsIndex;
        if (args == null || idx < 0 || idx >= args.length) {
            return null;
        }
        return args[idx] instanceof Bundle ? (Bundle) args[idx] : null;
    }

    public static int getAppThreadIndex() {
        return appThreadIndex;
    }

    public static void setAppThreadIndex(int appThreadIndex) {
        StartActivityCompat.appThreadIndex = appThreadIndex;
    }

    public static int getCallingPageIndex() {
        return callingPageIndex;
    }

    public static void setCallingPageIndex(int callingPageIndex) {
        StartActivityCompat.callingPageIndex = callingPageIndex;
    }

    public static int getIntentIndex() {
        return intentIndex;
    }

    public static void setIntentIndex(int intentIndex) {
        StartActivityCompat.intentIndex = intentIndex;
    }

    public static int getResolvedTypeIndex() {
        return resolvedTypeIndex;
    }

    public static void setResolvedTypeIndex(int resolvedTypeIndex) {
        StartActivityCompat.resolvedTypeIndex = resolvedTypeIndex;
    }

    public static int getResultToIndex() {
        return resultToIndex;
    }

    public static void setResultToIndex(int resultToIndex) {
        StartActivityCompat.resultToIndex = resultToIndex;
    }

    public static int getResultWhoIndex() {
        return resultWhoIndex;
    }

    public static void setResultWhoIndex(int resultWhoIndex) {
        StartActivityCompat.resultWhoIndex = resultWhoIndex;
    }

    public static int getRequestCodeIndex() {
        return requestCodeIndex;
    }

    public static void setRequestCodeIndex(int requestCodeIndex) {
        StartActivityCompat.requestCodeIndex = requestCodeIndex;
    }

    public static int getFlagsIndex() {
        return flagsIndex;
    }

    public static void setFlagsIndex(int flagsIndex) {
        StartActivityCompat.flagsIndex = flagsIndex;
    }

    public static int getProfilerInfoIndex() {
        return profilerInfoIndex;
    }

    public static void setProfilerInfoIndex(int profilerInfoIndex) {
        StartActivityCompat.profilerInfoIndex = profilerInfoIndex;
    }

    public static int getOptionsIndex() {
        return optionsIndex;
    }

    public static void setOptionsIndex(int optionsIndex) {
        StartActivityCompat.optionsIndex = optionsIndex;
    }
}
