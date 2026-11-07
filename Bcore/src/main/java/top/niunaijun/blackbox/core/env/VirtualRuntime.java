package top.niunaijun.blackbox.core.env;

import android.content.pm.ApplicationInfo;
import android.os.Build;

import java.lang.reflect.Field;

import black.android.ddm.BRDdmHandleAppName;
import black.android.os.BRProcess;

public class VirtualRuntime {

    private static String sInitialPackageName;
    private static String sProcessName;

    public static String getProcessName() {
        return sProcessName;
    }

    public static String getInitialPackageName() {
        return sInitialPackageName;
    }

    public static void setupRuntime(String processName, ApplicationInfo appInfo) {
        if (sProcessName != null) {
            return;
        }
        sInitialPackageName = appInfo.packageName;
        sProcessName = processName;
        BRProcess.get().setArgV0(processName);
        BRDdmHandleAppName.get().setAppName(processName, 0);
        spoofCleanBuildFields();
    }

    private static void spoofCleanBuildFields() {
        try {
            Field tagsField = Build.class.getDeclaredField("TAGS");
            tagsField.setAccessible(true);
            tagsField.set(null, "release-keys");
        } catch (Throwable ignored) {
        }
        try {
            Field typeField = Build.class.getDeclaredField("TYPE");
            typeField.setAccessible(true);
            typeField.set(null, "user");
        } catch (Throwable ignored) {
        }
    }
}
