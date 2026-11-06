package top.niunaijun.blackbox.utils.compat;

import android.os.Build;

public class BuildCompat {

    public static int getPreviewSDKInt() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                return Build.VERSION.PREVIEW_SDK_INT;
            } catch (Throwable e) {
                // ignore
            }
        }
        return 0;
    }

    // Android 15 (Vanilla Ice Cream, API 35+)
    public static boolean isV() {
        return Build.VERSION.SDK_INT >= 35 || (Build.VERSION.SDK_INT >= 34 && getPreviewSDKInt() >= 1);
    }

    // Android 14 (Upside Down Cake, API 34+)
    public static boolean isU() {
        return Build.VERSION.SDK_INT >= 34 || (Build.VERSION.SDK_INT >= 33 && getPreviewSDKInt() >= 1);
    }

    // Android 13 (Tiramisu, API 33+)
    public static boolean isTiramisu() {
        return Build.VERSION.SDK_INT >= 33 || (Build.VERSION.SDK_INT >= 32 && getPreviewSDKInt() >= 1);
    }

    // Android 12L (API 32+)
    public static boolean isS_V2() {
        return Build.VERSION.SDK_INT >= 32 || (Build.VERSION.SDK_INT >= 31 && getPreviewSDKInt() >= 1);
    }

    // Android 12 (S, API 31+)
    public static boolean isS() {
        return Build.VERSION.SDK_INT >= 31 || (Build.VERSION.SDK_INT >= 30 && getPreviewSDKInt() >= 1);
    }

    // Android 11 (R, API 30+)
    public static boolean isR() {
        return Build.VERSION.SDK_INT >= 30 || (Build.VERSION.SDK_INT >= 29 && getPreviewSDKInt() >= 1);
    }

    // Android 10 (Q, API 29+)
    public static boolean isQ() {
        return Build.VERSION.SDK_INT >= 29 || (Build.VERSION.SDK_INT >= 28 && getPreviewSDKInt() >= 1);
    }

    // Android 9 (Pie, API 28+)
    public static boolean isPie() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || (Build.VERSION.SDK_INT >= 27 && getPreviewSDKInt() >= 1);
    }

    // Android 8.0 (Oreo, API 26+)
    public static boolean isOreo() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || (Build.VERSION.SDK_INT >= 25 && getPreviewSDKInt() >= 1);
    }

    // Android 7.1 (Nougat MR1, API 25+)
    public static boolean isN_MR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 || (Build.VERSION.SDK_INT >= 24 && getPreviewSDKInt() >= 1);
    }

    // Android 7.0 (Nougat, API 24+)
    public static boolean isN() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || (Build.VERSION.SDK_INT >= 23 && getPreviewSDKInt() >= 1);
    }

    // Android 6.0 (Marshmallow, API 23+)
    public static boolean isM() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    // Android 5.0 (Lollipop, API 21+)
    public static boolean isL() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean isSamsung() {
        return "samsung".equalsIgnoreCase(Build.BRAND) || "samsung".equalsIgnoreCase(Build.MANUFACTURER);
    }

    public static boolean isEMUI() {
        if (Build.DISPLAY.toUpperCase().startsWith("EMUI")) {
            return true;
        }
        String property = SystemPropertiesCompat.get("ro.build.version.emui");
        return property != null && property.contains("EmotionUI");
    }

    public static boolean isMIUI() {
        return SystemPropertiesCompat.getInt("ro.miui.ui.version.code", 0) > 0;
    }

    public static boolean isFlyme() {
        return Build.DISPLAY.toLowerCase().contains("flyme");
    }

    public static boolean isColorOS() {
        return SystemPropertiesCompat.isExist("ro.build.version.opporom")
                || SystemPropertiesCompat.isExist("ro.rom.different.version");
    }

    public static boolean is360UI() {
        String property = SystemPropertiesCompat.get("ro.build.uiversion");
        return property != null && property.toUpperCase().contains("360UI");
    }

    public static boolean isLetv() {
        return Build.MANUFACTURER.equalsIgnoreCase("Letv");
    }

    public static boolean isVivo() {
        return SystemPropertiesCompat.isExist("ro.vivo.os.build.display.id");
    }

    private static ROMType sRomType;

    public static ROMType getROMType() {
        if (sRomType == null) {
            if (isEMUI()) {
                sRomType = ROMType.EMUI;
            } else if (isMIUI()) {
                sRomType = ROMType.MIUI;
            } else if (isFlyme()) {
                sRomType = ROMType.FLYME;
            } else if (isColorOS()) {
                sRomType = ROMType.COLOR_OS;
            } else if (is360UI()) {
                sRomType = ROMType._360;
            } else if (isLetv()) {
                sRomType = ROMType.LETV;
            } else if (isVivo()) {
                sRomType = ROMType.VIVO;
            } else if (isSamsung()) {
                sRomType = ROMType.SAMSUNG;
            } else {
                sRomType = ROMType.OTHER;
            }
        }
        return sRomType;
    }

    public enum ROMType {
        EMUI,
        MIUI,
        FLYME,
        COLOR_OS,
        LETV,
        VIVO,
        _360,
        SAMSUNG,
        OTHER
    }
}