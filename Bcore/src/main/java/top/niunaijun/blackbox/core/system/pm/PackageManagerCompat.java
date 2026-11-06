package top.niunaijun.blackbox.core.system.pm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import black.android.content.pm.BRApplicationInfoL;
import black.android.content.pm.BRApplicationInfoN;
import black.android.content.res.BRAssetManager;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.GmsCore;
import top.niunaijun.blackbox.core.env.AppSystemEnv;
import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.entity.pm.InstallOption;
import top.niunaijun.blackbox.utils.ArrayUtils;
import top.niunaijun.blackbox.utils.FileUtils;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.BuildCompat;


@SuppressLint("SdCardPath")
public class PackageManagerCompat {
    private static final String TAG = "PackageManagerCompat";

    private static Signature[] getSpoofedSignatures(BPackage p) {
        if (p == null) {
            return null;
        }
        Signature[] spoofed = GmsCore.getSpoofedSignatures(p.packageName);
        if (spoofed != null && spoofed.length > 0) {
            return spoofed;
        }
        if (p.mAppMetaData != null) {
            String fakeSig = p.mAppMetaData.getString("fake-signature");
            if (fakeSig != null && !fakeSig.isEmpty()) {
                try {
                    return new Signature[]{new Signature(fakeSig)};
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    public static PackageInfo generatePackageInfo(BPackageSettings ps, int flags, BPackageUserState state, int userId) {
        if (ps == null) {
            return null;
        }
        BPackage p = ps.pkg;
        if (p != null) {
            PackageInfo packageInfo = null;
            try {
                packageInfo = generatePackageInfo(p, flags, 0, 0, state, userId);
            } catch (Throwable t) {
                Slog.e(TAG, "generatePackageInfo failed for " + p.packageName, t);
            }
            return packageInfo;
        }
        return null;
    }

    public static PackageInfo generatePackageInfo(BPackage p, int flags, long firstInstallTime, long lastUpdateTime, BPackageUserState state, int userId) {
        if (!checkUseInstalledOrHidden(flags, state, p.applicationInfo)) {
            return null;
        }

        PackageInfo pi = new PackageInfo();
        pi.packageName = p.packageName;
        pi.versionCode = p.mVersionCode;
        pi.versionName = p.mVersionName;
        pi.sharedUserId = p.mSharedUserId;
        pi.sharedUserLabel = p.mSharedUserLabel;
        ApplicationInfo sharedAppInfo = generateApplicationInfo(p, flags, state, userId);
        pi.applicationInfo = sharedAppInfo;

        pi.firstInstallTime = firstInstallTime;
        pi.lastUpdateTime = lastUpdateTime;
        if (p.reqFeatures != null && !p.reqFeatures.isEmpty()) {
            FeatureInfo[] features = new FeatureInfo[p.reqFeatures.size()];
            p.reqFeatures.toArray(features);
            pi.reqFeatures = features;
        }

        if ((flags & PackageManager.GET_GIDS) != 0) {
            pi.gids = new int[]{};
        }
        if ((flags & PackageManager.GET_CONFIGURATIONS) != 0) {
            int N = p.configPreferences != null ? p.configPreferences.size() : 0;
            if (N > 0) {
                pi.configPreferences = new ConfigurationInfo[N];
                p.configPreferences.toArray(pi.configPreferences);
            }
            N = p.reqFeatures != null ? p.reqFeatures.size() : 0;
            if (N > 0) {
                pi.reqFeatures = new FeatureInfo[N];
                p.reqFeatures.toArray(pi.reqFeatures);
            }
        }
        // Create a lightweight ApplicationInfo without duplicate app-level metaData Bundle for child components
        // so packages with dozens of providers/services (like microG GmsCore) never exceed the 1MB Binder Parcel limit.
        ApplicationInfo componentAppInfo = sharedAppInfo;
        if (sharedAppInfo != null && sharedAppInfo.metaData != null) {
            componentAppInfo = new ApplicationInfo(sharedAppInfo);
            componentAppInfo.metaData = null;
        }

        if ((flags & PackageManager.GET_ACTIVITIES) != 0 && p.activities != null) {
            pi.activities = null;
            final int N = p.activities.size();
            if (N > 0) {
                int num = 0;
                final ActivityInfo[] res = new ActivityInfo[N];
                for (int i = 0; i < N; i++) {
                    final BPackage.Activity a = p.activities.get(i);
                    ActivityInfo ai = generateActivityInfo(a, flags, state, userId);
                    if (ai != null) {
                        ai.applicationInfo = componentAppInfo;
                        res[num++] = ai;
                    }
                }
                pi.activities = ArrayUtils.trimToSize(res, num);
            }
        }
        if ((flags & PackageManager.GET_RECEIVERS) != 0 && p.receivers != null) {
            pi.receivers = null;
            final int N = p.receivers.size();
            if (N > 0) {
                int num = 0;
                final ActivityInfo[] res = new ActivityInfo[N];
                for (int i = 0; i < N; i++) {
                    final BPackage.Activity a = p.receivers.get(i);
                    ActivityInfo ai = generateActivityInfo(a, flags, state, userId);
                    if (ai != null) {
                        ai.applicationInfo = componentAppInfo;
                        res[num++] = ai;
                    }
                }
                pi.receivers = ArrayUtils.trimToSize(res, num);
            }
        }
        if ((flags & PackageManager.GET_SERVICES) != 0 && p.services != null) {
            pi.services = null;
            final int N = p.services.size();
            if (N > 0) {
                int num = 0;
                final ServiceInfo[] res = new ServiceInfo[N];
                for (int i = 0; i < N; i++) {
                    final BPackage.Service s = p.services.get(i);
                    ServiceInfo si = generateServiceInfo(s, flags, state, userId);
                    if (si != null) {
                        si.applicationInfo = componentAppInfo;
                        res[num++] = si;
                    }
                }
                pi.services = ArrayUtils.trimToSize(res, num);
            }
        }
        if ((flags & PackageManager.GET_PROVIDERS) != 0 && p.providers != null) {
            pi.providers = null;
            final int N = p.providers.size();
            if (N > 0) {
                int num = 0;
                final ProviderInfo[] res = new ProviderInfo[N];
                for (int i = 0; i < N; i++) {
                    final BPackage.Provider pr = p.providers.get(i);
                    ProviderInfo providerInfo = generateProviderInfo(pr, flags, state, userId);
                    if (providerInfo != null) {
                        providerInfo.applicationInfo = componentAppInfo;
                        res[num++] = providerInfo;
                    }
                }
                pi.providers = ArrayUtils.trimToSize(res, num);
            }
        }
        if ((flags & PackageManager.GET_INSTRUMENTATION) != 0 && p.instrumentation != null) {
            pi.instrumentation = null;
            int N = p.instrumentation.size();
            if (N > 0) {
                pi.instrumentation = new InstrumentationInfo[N];
                for (int i = 0; i < N; i++) {
                    pi.instrumentation[i] = generateInstrumentationInfo(
                            p.instrumentation.get(i), flags);
                }
            }
        }
        if ((flags & PackageManager.GET_PERMISSIONS) != 0) {
            pi.permissions = null;
            int N = p.permissions != null ? p.permissions.size() : 0;
            if (N > 0) {
                pi.permissions = new PermissionInfo[N];
                for (int i = 0; i < N; i++) {
                    pi.permissions[i] = generatePermissionInfo(p.permissions.get(i), flags);
                }
            }
            pi.requestedPermissions = null;
            N = p.requestedPermissions != null ? p.requestedPermissions.size() : 0;
            if (N > 0) {
                pi.requestedPermissions = new String[N];
                pi.requestedPermissionsFlags = new int[N];
                for (int i = 0; i < N; i++) {
                    final String perm = p.requestedPermissions.get(i);
                    pi.requestedPermissions[i] = perm;
                    pi.requestedPermissionsFlags[i] = PackageInfo.REQUESTED_PERMISSION_GRANTED;
                }
            }
        }

        Signature[] spoofedSigs = getSpoofedSignatures(p);
        if (spoofedSigs != null) {
            if ((flags & PackageManager.GET_SIGNATURES) != 0 || GmsCore.isGoogleAppOrService(p.packageName)) {
                pi.signatures = spoofedSigs;
            }
            if (BuildCompat.isPie() && (flags & PackageManager.GET_SIGNING_CERTIFICATES) != 0) {
                pi.signingInfo = GmsCore.createSigningInfo(spoofedSigs);
            }
        } else {
            if ((flags & PackageManager.GET_SIGNATURES) != 0) {
                if (p.mSignatures != null && p.mSignatures.length > 0) {
                    pi.signatures = p.mSignatures;
                } else if (p.mSigningDetails != null && p.mSigningDetails.signatures != null) {
                    pi.signatures = p.mSigningDetails.signatures;
                }
            }
            if (BuildCompat.isPie() && (flags & PackageManager.GET_SIGNING_CERTIFICATES) != 0) {
                Signature[] sigs = p.mSigningDetails != null ? p.mSigningDetails.signatures : p.mSignatures;
                if (sigs != null && sigs.length > 0) {
                    pi.signingInfo = GmsCore.createSigningInfo(sigs);
                }
            }
        }
        return pi;
    }

    public static ActivityInfo generateActivityInfo(BPackage.Activity a, int flags, BPackageUserState state, int userId) {
        if (!checkUseInstalledOrHidden(flags, state, a.info.applicationInfo)) {
            return null;
        }

        ActivityInfo ai = new ActivityInfo(a.info);
        ai.metaData = a.metaData;
        ai.processName = BPackageManagerService.fixProcessName(ai.packageName, ai.processName);
        if (GmsCore.isGoogleAppOrService(ai.packageName)) {
            ai.processName = ai.packageName;
            ai.exported = true;
            ai.permission = null;
        }
        ai.applicationInfo = generateApplicationInfo(a.owner, flags, state, userId);
        if (ai.theme == 0 && ai.applicationInfo != null && ai.applicationInfo.theme != 0) {
            ai.theme = ai.applicationInfo.theme;
        }
        return ai;
    }

    public static ServiceInfo generateServiceInfo(BPackage.Service s, int flags, BPackageUserState state, int userId) {
        if (!checkUseInstalledOrHidden(flags, state, s.info.applicationInfo)) {
            return null;
        }

        ServiceInfo si = new ServiceInfo(s.info);
        si.metaData = s.metaData;
        si.processName = BPackageManagerService.fixProcessName(si.packageName, si.processName);
        if (GmsCore.isGoogleAppOrService(si.packageName)) {
            si.processName = si.packageName;
            si.exported = true;
            si.permission = null;
        }
        si.applicationInfo = generateApplicationInfo(s.owner, flags, state, userId);
        return si;
    }

    public static ProviderInfo generateProviderInfo(BPackage.Provider p, int flags, BPackageUserState state, int userId) {
        if (!checkUseInstalledOrHidden(flags, state, p.info.applicationInfo)) {
            return null;
        }

        ProviderInfo pi = new ProviderInfo(p.info);
        if (pi.authority == null)
            return null;
        pi.metaData = p.metaData;
        pi.processName = BPackageManagerService.fixProcessName(pi.packageName, pi.processName);
        pi.readPermission = null;
        pi.writePermission = null;
        if (GmsCore.isGoogleAppOrService(pi.packageName)) {
            pi.processName = pi.packageName;
            pi.exported = true;
            pi.multiprocess = true;
            pi.grantUriPermissions = true;
            pi.forceUriPermissions = false;
        }
        if ((flags & PackageManager.GET_URI_PERMISSION_PATTERNS) == 0) {
            pi.uriPermissionPatterns = null;
        }
        pi.applicationInfo = generateApplicationInfo(p.owner, flags, state, userId);
        return pi;
    }

    public static PermissionInfo generatePermissionInfo(
            BPackage.Permission p, int flags) {
        if (p == null) return null;
        if ((flags & PackageManager.GET_META_DATA) == 0) {
            return p.info;
        }
        PermissionInfo pi = new PermissionInfo(p.info);
        pi.metaData = p.metaData;
        return pi;
    }

    public static InstrumentationInfo generateInstrumentationInfo(
            BPackage.Instrumentation i, int flags) {
        if (i == null) return null;
        if ((flags & PackageManager.GET_META_DATA) == 0) {
            return i.info;
        }
        InstrumentationInfo ii = new InstrumentationInfo(i.info);
        ii.metaData = i.metaData;
        return ii;
    }

    private static String resolveValidSourceDir(BPackage p, ApplicationInfo baseApplication) {
        if (p == null) {
            return baseApplication != null ? baseApplication.sourceDir : null;
        }
        // 1. Check p.baseCodePath if it points to an existing file
        if (p.baseCodePath != null && !p.baseCodePath.isEmpty() && new File(p.baseCodePath).exists()) {
            return p.baseCodePath;
        }
        // 2. Check Clone Box's virtual base.apk directory
        File baseApk = BEnvironment.getBaseApkDir(p.packageName);
        if (baseApk != null && baseApk.exists() && baseApk.length() > 1024) {
            p.baseCodePath = baseApk.getAbsolutePath();
            return p.baseCodePath;
        }
        // 3. Check p.applicationInfo.sourceDir
        if (p.applicationInfo != null && p.applicationInfo.sourceDir != null
                && !p.applicationInfo.sourceDir.isEmpty()
                && new File(p.applicationInfo.sourceDir).exists()) {
            p.baseCodePath = p.applicationInfo.sourceDir;
            return p.baseCodePath;
        }
        // 4. If bundled microG package, extract asset directly to base.apk
        if (GmsCore.isBundledMicroGPackage(p.packageName)) {
            String extracted = GmsCore.extractBundledApkIfNeeded(p.packageName);
            if (extracted != null && !extracted.isEmpty()) {
                p.baseCodePath = extracted;
                return extracted;
            }
        }
        // 5. Check host PackageManager
        try {
            ApplicationInfo hostAi = BlackBoxCore.getPackageManager().getApplicationInfo(p.packageName, 0);
            if (hostAi != null && hostAi.sourceDir != null && !hostAi.sourceDir.isEmpty()) {
                p.baseCodePath = hostAi.sourceDir;
                return hostAi.sourceDir;
            }
        } catch (Throwable ignored) {
        }
        // 6. Ultimate fallback so LoadedApk.makePaths never sees a null sourceDir
        if (p.baseCodePath != null && !p.baseCodePath.isEmpty()) {
            return p.baseCodePath;
        }
        return baseApplication != null ? baseApplication.sourceDir : "";
    }

    public static ApplicationInfo generateApplicationInfo(BPackage p, int flags, BPackageUserState state, int userId) {
        if (!checkUseInstalledOrHidden(flags, state, p.applicationInfo)) {
            return null;
        }
        ApplicationInfo baseApplication;
        try {
            baseApplication = BlackBoxCore.getContext().getApplicationInfo();
        } catch (Exception e) {
            return null;
        }
        String sourceDir = resolveValidSourceDir(p, baseApplication);
        if (p.applicationInfo == null) {
            PackageInfo archiveInfo = sourceDir != null ? BlackBoxCore.getPackageManager().getPackageArchiveInfo(sourceDir, 0) : null;
            if (archiveInfo != null && archiveInfo.applicationInfo != null) {
                p.applicationInfo = archiveInfo.applicationInfo;
            } else {
                p.applicationInfo = new ApplicationInfo();
                p.applicationInfo.packageName = p.packageName;
            }
        }
        ApplicationInfo ai = new ApplicationInfo(p.applicationInfo);
        if ((flags & PackageManager.GET_META_DATA) != 0 && p.mAppMetaData != null) {
            ai.metaData = p.mAppMetaData;
        }
        ai.dataDir = BEnvironment.getDataDir(ai.packageName, userId).getAbsolutePath();
        if (p.installOption == null || !p.installOption.isFlag(InstallOption.FLAG_SYSTEM)) {
            ai.nativeLibraryDir = BEnvironment.getAppLibDir(ai.packageName).getAbsolutePath();
            ai.splitSourceDirs = null;
            ai.splitPublicSourceDirs = null;
        }
        ai.processName = BPackageManagerService.fixProcessName(p.packageName, ai.packageName);
        ai.publicSourceDir = sourceDir;
        ai.sourceDir = sourceDir;
        ai.uid = BlackBoxCore.getHostUid();
        ai.flags |= ApplicationInfo.FLAG_HAS_CODE;

        if (GmsCore.isGoogleAppOrService(p.packageName)) {
            ai.flags |= ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        }

        if (BuildCompat.isL()) {
            BRApplicationInfoL.get(ai)._set_primaryCpuAbi(Build.CPU_ABI);
            if (baseApplication != null) {
                BRApplicationInfoL.get(ai)._set_scanPublicSourceDir(BRApplicationInfoL.get(baseApplication).scanPublicSourceDir());
                BRApplicationInfoL.get(ai)._set_scanSourceDir(BRApplicationInfoL.get(baseApplication).scanSourceDir());
            }
        }
        if (BuildCompat.isN()) {
            ai.deviceProtectedDataDir = BEnvironment.getDeDataDir(p.packageName, userId).getAbsolutePath();

            if (BRApplicationInfoN.get(ai)._check_deviceEncryptedDataDir() != null) {
                BRApplicationInfoN.get(ai)._set_deviceEncryptedDataDir(ai.deviceProtectedDataDir);
            }
            if (BRApplicationInfoN.get(ai)._check_credentialEncryptedDataDir() != null) {
                BRApplicationInfoN.get(ai)._set_credentialEncryptedDataDir(ai.dataDir);
            }
            if (BRApplicationInfoN.get(ai)._check_deviceProtectedDataDir() != null) {
                BRApplicationInfoN.get(ai)._set_deviceProtectedDataDir(ai.deviceProtectedDataDir);
            }
            if (BRApplicationInfoN.get(ai)._check_credentialProtectedDataDir() != null) {
                BRApplicationInfoN.get(ai)._set_credentialProtectedDataDir(ai.dataDir);
            }
        }
        fixJar(ai);
        return ai;
    }

    private static boolean checkUseInstalledOrHidden(int flags, BPackageUserState state,
                                                     ApplicationInfo appInfo) {
        if (appInfo != null && AppSystemEnv.isBlackPackage(appInfo.packageName))
            return false;
        if (!state.installed || state.hidden) {
            return false;
        }
        return true;
    }

    private static void fixJar(ApplicationInfo info) {
        String APACHE_LEGACY_JAR = "/system/framework/org.apache.http.legacy.boot.jar";
        String APACHE_LEGACY_JAR_Q = "/system/framework/org.apache.http.legacy.jar";
        Set<String> sharedLibraryFileList = new HashSet<>();
        if (BuildCompat.isQ()) {
            if (!FileUtils.isExist(APACHE_LEGACY_JAR_Q)) {
                sharedLibraryFileList.add(APACHE_LEGACY_JAR);
            } else {
                sharedLibraryFileList.add(APACHE_LEGACY_JAR_Q);
            }
        } else {
            sharedLibraryFileList.add(APACHE_LEGACY_JAR);
        }
        info.sharedLibraryFiles = sharedLibraryFileList.toArray(new String[]{});
    }

    public static Resources getResources(Context context, ApplicationInfo appInfo) {
        BPackageSettings ps = BPackageManagerService.get().getBPackageSetting(appInfo.packageName);
        if (ps != null) {
            AssetManager assets = BRAssetManager.get()._new();
            String codePath = resolveValidSourceDir(ps.pkg, appInfo);
            if (codePath != null) {
                BRAssetManager.get(assets).addAssetPath(codePath);
            }
            Resources hostRes = context.getResources();
            return new Resources(assets, hostRes.getDisplayMetrics(), hostRes.getConfiguration());
        }
        return null;
    }
}
