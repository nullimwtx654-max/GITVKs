package top.niunaijun.blackbox.core.env;

import android.content.ComponentName;

import java.util.ArrayList;
import java.util.List;

import top.niunaijun.blackbox.BlackBoxCore;


public class AppSystemEnv {
    private static final List<String> sSystemPackages = new ArrayList<>();
    private static final List<String> sSuPackages = new ArrayList<>();
    private static final List<String> sPreInstallPackages = new ArrayList<>();

    static {
        sSystemPackages.add("android");
        sSystemPackages.add("com.google.android.webview");
        sSystemPackages.add("com.google.android.webview.dev");
        sSystemPackages.add("com.google.android.webview.beta");
        sSystemPackages.add("com.google.android.webview.canary");
        sSystemPackages.add("com.android.webview");
        sSystemPackages.add("com.android.camera");
        sSystemPackages.add("com.android.talkback");
        sSystemPackages.add("com.miui.gallery");
        sSystemPackages.add("com.google.android.inputmethod.latin");
        sSystemPackages.add("com.huawei.webview");
        sSystemPackages.add("com.coloros.safecenter");

        sSuPackages.add("com.noshufou.android.su");
        sSuPackages.add("com.noshufou.android.su.elite");
        sSuPackages.add("eu.chainfire.supersu");
        sSuPackages.add("eu.chainfire.supersu.pro");
        sSuPackages.add("com.koushikdutta.superuser");
        sSuPackages.add("com.thirdparty.superuser");
        sSuPackages.add("com.yellowes.su");
        sSuPackages.add("com.topjohnwu.magisk");
        sSuPackages.add("io.github.vvb2060.magisk");
        sSuPackages.add("io.github.huskydg.magisk");
        sSuPackages.add("me.weishu.kernelsu");
        sSuPackages.add("me.bmax.apatch");
        sSuPackages.add("de.robv.android.xposed.installer");
        sSuPackages.add("org.meowcat.edxposed.manager");
        sSuPackages.add("org.lsposed.manager");
        sSuPackages.add("top.canyie.dreamland.manager");
        sSuPackages.add("io.va.exposed");
        sSuPackages.add("io.virtualapp");
        sSuPackages.add("com.lody.virtual");
        sSuPackages.add("com.excelliance.dualaid");
        sSuPackages.add("com.lbe.parallel");
        sSuPackages.add("com.parallel.space.lite");
        sSuPackages.add("com.dual.dualspace");
        sSuPackages.add("com.dualspace.multispace.android");
        sSuPackages.add("com.ludashi.superboost");
        sSuPackages.add("com.gbox.android");
        sSuPackages.add("com.vmos.pro");
        sSuPackages.add("top.niunaijun.blackbox");
        sSuPackages.add("com.saurik.substrate");
        sSuPackages.add("com.tsng.hidemyapplist");
        sSuPackages.add("com.chelpus.lackypatcher");
        sSuPackages.add("bin.mt.plus");
        sSuPackages.add("org.frida.server");
    }

    public static boolean isOpenPackage(String packageName) {
        if (packageName == null) return false;
        if (packageName.equals(BlackBoxCore.getHostPkg())) return true;
        return sSystemPackages.contains(packageName)
                || packageName.equals("com.android.chrome")
                || packageName.equals("com.google.android.apps.chrome")
                || packageName.equals("org.chromium.webview_shell")
                || packageName.equals("com.android.htmlviewer");
    }

    public static boolean isOpenPackage(ComponentName componentName) {
        return componentName != null && isOpenPackage(componentName.getPackageName());
    }

    public static boolean isBlackPackage(String packageName) {
        if (packageName == null) return false;
        return sSuPackages.contains(packageName);
    }

    public static List<String> getPreInstallPackages() {
        return sPreInstallPackages;
    }
}
