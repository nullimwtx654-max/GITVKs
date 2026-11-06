package top.niunaijun.blackbox.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import black.android.content.pm.BRPackageParserSigningDetails;
import black.android.content.pm.BRSigningInfo;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.entity.pm.InstallResult;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.BuildCompat;

public class GmsCore {
    private static final String TAG = "GmsCore";

    private static final HashSet<String> GOOGLE_APP = new HashSet<>();
    private static final HashSet<String> GOOGLE_SERVICE = new HashSet<>();
    private static final Map<String, String> BUNDLED_GMS_ASSETS = new LinkedHashMap<>();

    public static final String GMS_PKG = "com.google.android.gms";
    public static final String GSF_PKG = "com.google.android.gsf";
    public static final String VENDING_PKG = "com.android.vending";

    public static final String GOOGLE_FAKE_SIGNATURE_HEX = "308204433082032ba003020102020900c2e08746644a308d300d06092a864886f70d01010405003074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f6964301e170d3038303832313233313333345a170d3336303130373233313333345a3074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f696430820120300d06092a864886f70d01010105000382010d00308201080282010100ab562e00d83ba208ae0a966f124e29da11f2ab56d08f58e2cca91303e9b754d372f640a71b1dcb130967624e4656a7776a92193db2e5bfb724a91e77188b0e6a47a43b33d9609b77183145ccdf7b2e586674c9e1565b1f4c6a5955bff251a63dabf9c55c27222252e875e4f8154a645f897168c0b1bfc612eabf785769bb34aa7984dc7e2ea2764cae8307d8c17154d7ee5f64a51a44a602c249054157dc02cd5f5c0e55fbef8519fbe327f0b1511692c5a06f19d18385f5c4dbc2d6b93f68cc2979c70e18ab93866b3bd5db8999552a0e3b4c99df58fb918bedc182ba35e003c1b4b10dd244a8ee24fffd333872ab5221985edab0fc0d0b145b6aa192858e79020103a381d93081d6301d0603551d0e04160414c77d8cc2211756259a7fd382df6be398e4d786a53081a60603551d2304819e30819b8014c77d8cc2211756259a7fd382df6be398e4d786a5a178a4763074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f6964820900c2e08746644a308d300c0603551d13040530030101ff300d06092a864886f70d010104050003820101006dd252ceef85302c360aaace939bcff2cca904bb5d7a1661f8ae46b2994204d0ff4a68c7ed1a531ec4595a623ce60763b167297a7ae35712c407f208f0cb109429124d7b106219c084ca3eb3f9ad5fb871ef92269a8be28bf16d44c8d9a08e6cb2f005bb3fe2cb96447e868e731076ad45b33f6009ea19c161e62641aa99271dfd5228c5c587875ddb7f452758d661f6cc0cccb7352e424cc4365c523532f7325137593c4ae341f4db41edda0d0b1071a7c440f0fe9ea01cb627ca674369d084bd2fd911ff06cdbf2cfa10dc0f893ae35762919048c7efc64c7144178342f70581c9de573af55b390dd7fdb9418631895d5f759f30112687ff621410c069308a";
    private static Signature sGoogleSignature;

    static {
        BUNDLED_GMS_ASSETS.put(GMS_PKG, "gms/GmsCore.apk");
        BUNDLED_GMS_ASSETS.put(VENDING_PKG, "gms/FakeStore.apk");

        GOOGLE_APP.add(VENDING_PKG);
        GOOGLE_APP.add("com.google.android.play.games");
        GOOGLE_APP.add("com.google.android.wearable.app");
        GOOGLE_APP.add("com.google.android.wearable.app.cn");

        GOOGLE_SERVICE.add(GMS_PKG);
        GOOGLE_SERVICE.add(GSF_PKG);
        GOOGLE_SERVICE.add("com.google.android.gsf.login");
        GOOGLE_SERVICE.add("com.google.android.backuptransport");
        GOOGLE_SERVICE.add("com.google.android.backup");
        GOOGLE_SERVICE.add("com.google.android.configupdater");
        GOOGLE_SERVICE.add("com.google.android.syncadapters.contacts");
        GOOGLE_SERVICE.add("com.google.android.feedback");
        GOOGLE_SERVICE.add("com.google.android.onetimeinitializer");
        GOOGLE_SERVICE.add("com.google.android.partnersetup");
        GOOGLE_SERVICE.add("com.google.android.setupwizard");
        GOOGLE_SERVICE.add("com.google.android.syncadapters.calendar");
    }

    public static synchronized Signature getGoogleSignature() {
        if (sGoogleSignature == null) {
            sGoogleSignature = new Signature(GOOGLE_FAKE_SIGNATURE_HEX);
        }
        return sGoogleSignature;
    }

    public static Signature[] getGoogleSignatures() {
        return new Signature[]{getGoogleSignature()};
    }

    public static Signature[] getSpoofedSignatures(String packageName) {
        if (packageName != null && isGoogleAppOrService(packageName)) {
            return getGoogleSignatures();
        }
        return null;
    }

    public static boolean isBundledMicroGPackage(String packageName) {
        return packageName != null && BUNDLED_GMS_ASSETS.containsKey(packageName);
    }

    public static String extractBundledApkIfNeeded(String packageName) {
        if (packageName == null) return null;
        File baseApk = BEnvironment.getBaseApkDir(packageName);
        if (baseApk != null && baseApk.exists() && baseApk.length() > 1024) {
            return baseApk.getAbsolutePath();
        }
        String assetPath = BUNDLED_GMS_ASSETS.get(packageName);
        if (assetPath == null) return null;
        try {
            if (baseApk != null) {
                File parent = baseApk.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                Context context = BlackBoxCore.getContext();
                if (context != null) {
                    try (InputStream is = context.getAssets().open(assetPath);
                         FileOutputStream fos = new FileOutputStream(baseApk)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = is.read(buf)) != -1) {
                            fos.write(buf, 0, len);
                        }
                        fos.flush();
                    }
                    baseApk.setReadable(true, false);
                    if (baseApk.exists() && baseApk.length() > 1024) {
                        return baseApk.getAbsolutePath();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        File cached = extractAssetToCache(assetPath, packageName + "_bundled.apk");
        return cached != null && cached.exists() ? cached.getAbsolutePath() : null;
    }

    @SuppressWarnings("NewApi")
    public static SigningInfo createGoogleSigningInfo() {
        return createSigningInfo(getGoogleSignatures());
    }

    @SuppressWarnings("NewApi")
    public static SigningInfo createSigningInfo(Signature[] signatures) {
        if (!BuildCompat.isPie() || signatures == null || signatures.length == 0) {
            return null;
        }
        try {
            PackageParser.SigningDetails signingDetails = null;
            try {
                java.lang.reflect.Constructor<PackageParser.SigningDetails> ctor =
                        PackageParser.SigningDetails.class.getDeclaredConstructor(
                                Signature[].class, int.class, Signature[].class);
                ctor.setAccessible(true);
                signingDetails = ctor.newInstance(signatures, 3, signatures);
            } catch (Throwable ignored) {
                try {
                    java.lang.reflect.Constructor<PackageParser.SigningDetails> ctor2 =
                            PackageParser.SigningDetails.class.getDeclaredConstructor(
                                    Signature[].class, int.class);
                    ctor2.setAccessible(true);
                    signingDetails = ctor2.newInstance(signatures, 3);
                } catch (Throwable ignored2) {
                    return null;
                }
            }
            if (signingDetails == null) {
                return null;
            }
            return BRSigningInfo.get()._new(signingDetails);
        } catch (Throwable t) {
            Slog.w(TAG, "Failed to create SigningInfo: " + t.getMessage());
            return null;
        }
    }

    public static boolean isGoogleService(String packageName) {
        return GOOGLE_SERVICE.contains(packageName);
    }

    public static boolean isGoogleAppOrService(String str) {
        return GOOGLE_APP.contains(str) || GOOGLE_SERVICE.contains(str);
    }

    private static boolean hasBundledAsset(String assetPath) {
        try {
            Context context = BlackBoxCore.getContext();
            if (context == null) return false;
            try (InputStream is = context.getAssets().open(assetPath)) {
                return is != null;
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static File extractAssetToCache(String assetPath, String outName) {
        try {
            Context context = BlackBoxCore.getContext();
            if (context == null) return null;
            File outFile = new File(context.getCacheDir(), outName);
            try (InputStream is = context.getAssets().open(assetPath);
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                }
                fos.flush();
            }
            return outFile;
        } catch (Throwable t) {
            Slog.w(TAG, "Bundled asset not found or could not be extracted: " + assetPath + " (" + t.getMessage() + ")");
            return null;
        }
    }

    private static void writePrefsIfMissingCheckin(File prefsFile) {
        try {
            if (prefsFile.exists()) {
                byte[] bytes = new byte[(int) prefsFile.length()];
                try (java.io.FileInputStream fis = new java.io.FileInputStream(prefsFile)) {
                    fis.read(bytes);
                }
                String content = new String(bytes, StandardCharsets.UTF_8);
                if (content.contains("name=\"checkin_enable_service\" value=\"true\"")
                        && content.contains("name=\"auth_manager_visible\" value=\"true\"")) {
                    return;
                }
            }
            String xml = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n"
                    + "<map>\n"
                    + "    <boolean name=\"checkin_enable_service\" value=\"true\" />\n"
                    + "    <boolean name=\"gcm_enable_mcs_service\" value=\"true\" />\n"
                    + "    <boolean name=\"auth_manager_trust_google\" value=\"true\" />\n"
                    + "    <boolean name=\"auth_manager_visible\" value=\"true\" />\n"
                    + "    <boolean name=\"auth_include_android_id\" value=\"true\" />\n"
                    + "    <boolean name=\"safetynet_enabled\" value=\"true\" />\n"
                    + "    <boolean name=\"droidguard_enabled\" value=\"true\" />\n"
                    + "    <boolean name=\"vending_licensing\" value=\"true\" />\n"
                    + "    <boolean name=\"vending_billing\" value=\"true\" />\n"
                    + "    <boolean name=\"vending_asset_delivery\" value=\"true\" />\n"
                    + "    <boolean name=\"vending_split_install\" value=\"true\" />\n"
                    + "    <boolean name=\"game_allow_create_player\" value=\"true\" />\n"
                    + "</map>\n";
            try (FileOutputStream fos = new FileOutputStream(prefsFile)) {
                fos.write(xml.getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
        } catch (Throwable ignored) {
        }
    }

    public static void ensureMicroGConfigured(int userId) {
        preconfigureMicroGSettings(userId);
    }

    private static void preconfigureMicroGSettings(int userId) {
        try {
            String hostPkg = BlackBoxCore.getHostPkg();
            File[] dataDirs = new File[]{
                    BEnvironment.getDataDir(GMS_PKG, userId),
                    BEnvironment.getDeDataDir(GMS_PKG, userId)
            };
            for (File dataDir : dataDirs) {
                if (dataDir == null) continue;
                File sharedPrefsDir = new File(dataDir, "shared_prefs");
                if (!sharedPrefsDir.exists()) {
                    sharedPrefsDir.mkdirs();
                }
                writePrefsIfMissingCheckin(new File(sharedPrefsDir, GMS_PKG + "_preferences.xml"));
                if (hostPkg != null && !hostPkg.isEmpty()) {
                    writePrefsIfMissingCheckin(new File(sharedPrefsDir, hostPkg + "_preferences.xml"));
                }
            }
        } catch (Throwable t) {
            Slog.w(TAG, "Failed to preconfigure microG settings: " + t.getMessage());
        }
    }

    /**
     * Checks whether the given package is properly installed as a standalone bundled microG APK
     * (in BEnvironment.getBaseApkDir(pkgName)), NOT as a broken host FLAG_SYSTEM reference.
     */
    private static boolean isValidBundledMicroGInstall(String pkgName, int userId) {
        try {
            BlackBoxCore blackBoxCore = BlackBoxCore.get();
            if (!blackBoxCore.isInstalled(pkgName, userId)) {
                return false;
            }
            File baseApk = BEnvironment.getBaseApkDir(pkgName);
            if (baseApk == null || !baseApk.exists() || baseApk.length() < 10_000) {
                return false;
            }
            ApplicationInfo ai = BlackBoxCore.getBPackageManager().getApplicationInfo(pkgName, 0, userId);
            if (ai == null || ai.sourceDir == null || !new File(ai.sourceDir).exists()) {
                return false;
            }
            return ai.sourceDir.equals(baseApk.getAbsolutePath());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static InstallResult installBundledMicroG(int userId) {
        BlackBoxCore blackBoxCore = BlackBoxCore.get();
        boolean installedAny = false;
        for (Map.Entry<String, String> entry : BUNDLED_GMS_ASSETS.entrySet()) {
            String pkgName = entry.getKey();
            String assetPath = entry.getValue();
            if (isValidBundledMicroGInstall(pkgName, userId)) {
                installedAny = true;
                continue;
            }
            if (blackBoxCore.isInstalled(pkgName, userId)) {
                Slog.w(TAG, "Detected legacy/system GMS install for " + pkgName + ", replacing with bundled microG APK");
                try {
                    blackBoxCore.uninstallPackage(pkgName);
                } catch (Throwable ignored) {
                }
            }
            File tempApk = extractAssetToCache(assetPath, pkgName + "_bundled.apk");
            if (tempApk != null && tempApk.exists() && tempApk.length() > 0) {
                try {
                    InstallResult res = blackBoxCore.installPackageAsUser(tempApk, userId);
                    if (res != null && res.success) {
                        installedAny = true;
                        Slog.d(TAG, "Installed bundled microG package " + pkgName + " for user " + userId);
                    } else {
                        Slog.w(TAG, "Failed to install bundled microG package " + pkgName + ": " + (res != null ? res.msg : "null"));
                    }
                } finally {
                    tempApk.delete();
                }
            }
        }
        // Remove any legacy host FLAG_SYSTEM Google packages that conflict with microG
        for (String legacyPkg : GOOGLE_SERVICE) {
            if (!BUNDLED_GMS_ASSETS.containsKey(legacyPkg) && blackBoxCore.isInstalled(legacyPkg, userId)) {
                try {
                    blackBoxCore.uninstallPackageAsUser(legacyPkg, userId);
                } catch (Throwable ignored) {
                }
            }
        }
        if (installedAny && blackBoxCore.isInstalled(GMS_PKG, userId)) {
            preconfigureMicroGSettings(userId);
            InstallResult ok = new InstallResult();
            ok.packageName = GMS_PKG;
            return ok;
        }
        return null;
    }

    public static void ensureGmsInstalled(int userId) {
        try {
            if (!isValidBundledMicroGInstall(GMS_PKG, userId)
                    || !isValidBundledMicroGInstall(VENDING_PKG, userId)) {
                installBundledMicroG(userId);
            } else {
                preconfigureMicroGSettings(userId);
            }
        } catch (Throwable t) {
            Slog.w(TAG, "ensureGmsInstalled failed for user " + userId + ": " + t.getMessage());
        }
    }

    private static InstallResult installPackages(Set<String> list, int userId) {
        BlackBoxCore blackBoxCore = BlackBoxCore.get();
        for (String packageName : list) {
            if (blackBoxCore.isInstalled(packageName, userId)) {
                continue;
            }
            try {
                BlackBoxCore.getContext().getPackageManager().getApplicationInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }
            InstallResult installResult = blackBoxCore.installPackageAsUser(packageName, userId);
            if (!installResult.success) {
                return installResult;
            }
        }
        return new InstallResult();
    }

    private static void uninstallPackages(Set<String> list, int userId) {
        BlackBoxCore blackBoxCore = BlackBoxCore.get();
        for (String packageName : list) {
            blackBoxCore.uninstallPackageAsUser(packageName, userId);
        }
    }

    public static InstallResult installGApps(int userId) {
        // 1. Always install standalone bundled microG (GmsCore.apk + FakeStore.apk)
        InstallResult bundledResult = installBundledMicroG(userId);
        if (bundledResult != null && bundledResult.success) {
            return bundledResult;
        }

        // 2. Fallback to host packages ONLY if bundled assets are absent
        Set<String> googleApps = new HashSet<>();
        googleApps.addAll(GOOGLE_SERVICE);
        googleApps.addAll(GOOGLE_APP);

        InstallResult hostResult = installPackages(googleApps, userId);
        if (!hostResult.success) {
            uninstallGApps(userId);
            return hostResult;
        }
        return hostResult;
    }

    public static void uninstallGApps(int userId) {
        uninstallPackages(GOOGLE_SERVICE, userId);
        uninstallPackages(GOOGLE_APP, userId);
    }

    public static void remove(String packageName) {
        GOOGLE_SERVICE.remove(packageName);
        GOOGLE_APP.remove(packageName);
    }

    public static boolean isSupportGms() {
        if (hasBundledAsset("gms/GmsCore.apk")) {
            return true;
        }
        try {
            BlackBoxCore.getPackageManager().getPackageInfo(GMS_PKG, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return true;
    }

    public static boolean isInstalledGoogleService(int userId) {
        return BlackBoxCore.get().isInstalled(GMS_PKG, userId);
    }

    public static boolean launchGoogleLogin(int userId) {
        try {
            ensureGmsInstalled(userId);
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(GMS_PKG, "org.microg.gms.auth.login.LoginActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (BlackBoxCore.getBPackageManager().resolveActivity(intent, 0, null, userId) == null) {
                intent = new Intent("com.google.android.gms.auth.login.LOGIN");
                intent.setPackage(GMS_PKG);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            BlackBoxCore.get().startActivity(intent, userId);
            return true;
        } catch (Throwable t) {
            Slog.e(TAG, "Failed to launch Google Login for user " + userId, t);
            return false;
        }
    }

    public static boolean launchMicroGSettings(int userId) {
        try {
            ensureGmsInstalled(userId);
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(GMS_PKG, "org.microg.gms.ui.MainSettingsActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (BlackBoxCore.getBPackageManager().resolveActivity(intent, 0, null, userId) == null) {
                Intent launchIntent = BlackBoxCore.getBPackageManager().getLaunchIntentForPackage(GMS_PKG, userId);
                if (launchIntent != null) {
                    intent = launchIntent;
                }
            }
            BlackBoxCore.get().startActivity(intent, userId);
            return true;
        } catch (Throwable t) {
            Slog.e(TAG, "Failed to launch microG Settings for user " + userId, t);
            return false;
        }
    }
}
