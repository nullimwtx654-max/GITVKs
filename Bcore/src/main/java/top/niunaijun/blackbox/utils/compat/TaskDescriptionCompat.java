package top.niunaijun.blackbox.utils.compat;

import android.app.ActivityManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.utils.DrawableUtils;

public class TaskDescriptionCompat {
    public static ActivityManager.TaskDescription fix(ActivityManager.TaskDescription td) {
        if (td == null) {
            return null;
        }
        String label = td.getLabel();
        Bitmap icon = td.getIcon();

        if (label != null && !label.startsWith("[B") && icon != null) {
            return td;
        }

        CharSequence appLabel = getApplicationLabel();
        if (appLabel != null && appLabel.length() > 0) {
            label = getTaskDescriptionLabel(BlackBoxCore.getUserId(), appLabel);
        } else if (label != null) {
            label = label.replaceFirst("^\\[B\\d+\\]", "");
        }

        if (icon == null) {
            Drawable drawable = getApplicationIcon();
            if (drawable != null) {
                icon = DrawableUtils.drawableToBitmap(drawable, 96, 96);
            }
        }

        td = new ActivityManager.TaskDescription(label, icon, td.getPrimaryColor());
        return td;
    }

    public static String getTaskDescriptionLabel(int userId, CharSequence label) {
        if (label == null) {
            return "";
        }
        String clean = label.toString().replaceFirst("^\\[B\\d+\\]", "");
        if (userId > 0) {
            return clean + " (" + userId + ")";
        }
        return clean;
    }

    public static CharSequence getApplicationLabel() {
        try {
            String pkg = BlackBoxCore.getAppPackageName();
            if (pkg != null && !pkg.isEmpty()) {
                PackageManager pm = BlackBoxCore.getPackageManager();
                ApplicationInfo ai = BlackBoxCore.getBPackageManager().getApplicationInfo(pkg, 0, BlackBoxCore.getUserId());
                if (ai != null && pm != null) {
                    CharSequence l = ai.loadLabel(pm);
                    if (l != null && l.length() > 0) {
                        return l;
                    }
                }
                if (pm != null) {
                    return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0));
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static Drawable getApplicationIcon() {
        try {
            String pkg = BlackBoxCore.getAppPackageName();
            if (pkg != null && !pkg.isEmpty()) {
                PackageManager pm = BlackBoxCore.getPackageManager();
                ApplicationInfo ai = BlackBoxCore.getBPackageManager().getApplicationInfo(pkg, 0, BlackBoxCore.getUserId());
                if (ai != null && pm != null) {
                    Drawable d = ai.loadIcon(pm);
                    if (d != null) {
                        return d;
                    }
                }
                if (pm != null) {
                    return pm.getApplicationIcon(pkg);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
