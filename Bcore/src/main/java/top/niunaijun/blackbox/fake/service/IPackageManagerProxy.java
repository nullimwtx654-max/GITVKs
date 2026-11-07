package top.niunaijun.blackbox.fake.service;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import black.android.app.BRActivityThread;
import black.android.app.BRContextImpl;
import black.android.content.pm.BRPackageManager;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.GmsCore;
import top.niunaijun.blackbox.core.env.AppSystemEnv;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.fake.service.base.PkgMethodProxy;
import top.niunaijun.blackbox.fake.service.base.ValueMethodProxy;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.Reflector;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.BuildCompat;
import top.niunaijun.blackbox.utils.compat.ParceledListSliceCompat;


public class IPackageManagerProxy extends BinderInvocationStub {
    public static final String TAG = "PackageManagerStub";

    public IPackageManagerProxy() {
        super(BRActivityThread.get().sPackageManager().asBinder());
    }

    @Override
    protected Object getWho() {
        return BRActivityThread.get().sPackageManager();
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        BRActivityThread.get()._set_sPackageManager(proxyInvocation);
        replaceSystemService("package");
        Object systemContext = BRActivityThread.get(BlackBoxCore.mainThread()).getSystemContext();
        BRContextImpl.get(systemContext).getPackageManager();
        PackageManager packageManager = BRContextImpl.get(systemContext).mPackageManager();
        if (packageManager != null) {
            BRPackageManager.get().disableApplicationInfoCache();
            IPermissionManagerProxy.disablePermissionCaches();
            try {
                Reflector.on("android.app.ApplicationPackageManager")
                        .field("mPM")
                        .set(packageManager, proxyInvocation);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
        addMethodHook(new ValueMethodProxy("addOnPermissionsChangeListener", 0));
        addMethodHook(new ValueMethodProxy("removeOnPermissionsChangeListener", 0));
        addMethodHook(new ValueMethodProxy("notifyPackageUse", 0));
        addMethodHook(new SimpleAudioPermissionHook());
        addMethodHook(new CheckSelfPermission());
        addMethodHook(new ShouldShowRequestPermissionRationale());
        addMethodHook(new RequestPermissions());
        addMethodHook(new SetSplashScreenTheme());
        addMethodHook(new XiaomiSecurityBypass());
    }

    @ProxyMethod("resolveIntent")
    public static class ResolveIntent extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = (Intent) args[0];
            String resolvedType = (String) args[1];
            int flags = MethodParameterUtils.toInt(args[2]);
            ResolveInfo resolveInfo = BlackBoxCore.getBPackageManager().resolveIntent(intent, resolvedType, flags, BlackBoxCore.getUserId());
            if (resolveInfo != null) {
                return resolveInfo;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("resolveService")
    public static class ResolveService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = (Intent) args[0];
            String resolvedType = (String) args[1];
            int flags = MethodParameterUtils.toInt(args[2]);
            ResolveInfo resolveInfo = BlackBoxCore.getBPackageManager().resolveService(intent, flags, resolvedType, BlackBoxCore.getUserId());
            if (resolveInfo != null) {
                return resolveInfo;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("queryIntentActivities")
    public static class QueryIntentActivities extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = MethodParameterUtils.getFirstParam(args, Intent.class);
            String resolvedType = MethodParameterUtils.getFirstParam(args, String.class);
            Integer flagsObj = MethodParameterUtils.getFirstParam(args, Integer.class);
            int flags = flagsObj != null ? flagsObj : MethodParameterUtils.toInt(args[2]);
            List<ResolveInfo> resolves = BlackBoxCore.getBPackageManager().queryIntentActivities(intent, flags, resolvedType, BActivityThread.getUserId());
            if (resolves != null && !resolves.isEmpty()) {
                if (BuildCompat.isN()) {
                    return ParceledListSliceCompat.create(resolves);
                }
                return resolves;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("queryIntentServices")
    public static class QueryIntentServices extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = MethodParameterUtils.getFirstParam(args, Intent.class);
            String resolvedType = MethodParameterUtils.getFirstParam(args, String.class);
            Integer flagsObj = MethodParameterUtils.getFirstParam(args, Integer.class);
            int flags = flagsObj != null ? flagsObj : MethodParameterUtils.toInt(args[2]);
            List<ResolveInfo> resolves = BlackBoxCore.getBPackageManager().queryIntentServices(intent, flags, BActivityThread.getUserId());
            if (resolves != null && !resolves.isEmpty()) {
                if (BuildCompat.isN()) {
                    return ParceledListSliceCompat.create(resolves);
                }
                return resolves;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setComponentEnabledSetting")
    public static class SetComponentEnabledSetting extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }

    @ProxyMethod("checkSignatures")
    public static class CheckSignatures extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length >= 2 && args[0] instanceof String && args[1] instanceof String) {
                String pkg1 = (String) args[0];
                String pkg2 = (String) args[1];
                if (Objects.equals(pkg1, pkg2)) {
                    return PackageManager.SIGNATURE_MATCH;
                }
                if (GmsCore.isGoogleAppOrService(pkg1) && GmsCore.isGoogleAppOrService(pkg2)) {
                    return PackageManager.SIGNATURE_MATCH;
                }
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable ignored) {
                return PackageManager.SIGNATURE_MATCH;
            }
        }
    }

    @ProxyMethod("checkUidSignatures")
    public static class CheckUidSignatures extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return PackageManager.SIGNATURE_MATCH;
        }
    }

    @ProxyMethod("getPermissionInfo")
    public static class GetPermissionInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String name = args != null && args.length > 0 && args[0] instanceof String ? (String) args[0] : null;
            try {
                Object res = method.invoke(who, args);
                if (res != null) {
                    return res;
                }
            } catch (Throwable ignored) {
            }
            if (name != null) {
                PermissionInfo pi = new PermissionInfo();
                pi.name = name;
                pi.packageName = name.startsWith("org.microg.") || name.startsWith("com.google.") ? GmsCore.GMS_PKG : "android";
                pi.nonLocalizedLabel = name;
                pi.protectionLevel = PermissionInfo.PROTECTION_NORMAL;
                return pi;
            }
            return null;
        }
    }

    @ProxyMethod("getPackageInfo")
    public static class GetPackageInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String packageName = (String) args[0];
            int flags = MethodParameterUtils.toInt(args[1]);

            if (AppSystemEnv.isBlackPackage(packageName)) {
                return null;
            }
            if (Objects.equals(packageName, BlackBoxCore.getHostPkg())) {
                return method.invoke(who, args);
            }

            PackageInfo packageInfo = BlackBoxCore.getBPackageManager().getPackageInfo(packageName, flags, BlackBoxCore.getUserId());
            if (packageInfo != null) {
                applyGoogleSignatureIfNeeded(packageInfo);
                if (packageInfo.requestedPermissions != null && packageInfo.requestedPermissionsFlags != null) {
                    for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {
                        packageInfo.requestedPermissionsFlags[i] |= PackageInfo.REQUESTED_PERMISSION_GRANTED;
                    }
                }
                return packageInfo;
            }

            if (GmsCore.VENDING_PKG.equals(packageName)
                    || GmsCore.GSF_PKG.equals(packageName)
                    || GmsCore.GMS_PKG.equals(packageName)) {
                return createFallbackGooglePackageInfo(packageName);
            }

            if (AppSystemEnv.isOpenPackage(packageName)) {
                return method.invoke(who, args);
            }
            return null;
        }

        private void applyGoogleSignatureIfNeeded(PackageInfo pi) {
            if (pi == null) return;
            boolean spoof = GmsCore.isGoogleAppOrService(pi.packageName);
            if (!spoof && pi.applicationInfo != null && pi.applicationInfo.metaData != null) {
                spoof = pi.applicationInfo.metaData.containsKey("fake-signature");
            }
            if (spoof) {
                pi.signatures = GmsCore.getGoogleSignatures();
                if (BuildCompat.isPie()) {
                    pi.signingInfo = GmsCore.createGoogleSigningInfo();
                }
            }
        }

        @SuppressWarnings("NewApi")
        private PackageInfo createFallbackGooglePackageInfo(String packageName) {
            PackageInfo packageInfo = new PackageInfo();
            packageInfo.packageName = packageName;
            if (GmsCore.VENDING_PKG.equals(packageName)) {
                packageInfo.versionName = "40.2.26-29";
                packageInfo.versionCode = 84022600;
            } else {
                packageInfo.versionName = "25.24.32";
                packageInfo.versionCode = 252432000;
            }
            if (BuildCompat.isPie()) {
                packageInfo.setLongVersionCode(packageInfo.versionCode);
            }

            ApplicationInfo hostApp = BlackBoxCore.getContext().getApplicationInfo();
            ApplicationInfo appInfo = new ApplicationInfo(hostApp);
            appInfo.packageName = packageName;
            appInfo.processName = packageName;
            appInfo.name = GmsCore.VENDING_PKG.equals(packageName) ? "Google Play Store" : "Google Play services";
            appInfo.flags = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_HAS_CODE;
            appInfo.enabled = true;
            String extracted = GmsCore.extractBundledApkIfNeeded(packageName);
            if (extracted != null && !extracted.isEmpty()) {
                appInfo.sourceDir = extracted;
                appInfo.publicSourceDir = extracted;
            }
            packageInfo.applicationInfo = appInfo;

            packageInfo.signatures = GmsCore.getGoogleSignatures();
            if (BuildCompat.isPie()) {
                packageInfo.signingInfo = GmsCore.createGoogleSigningInfo();
            }
            return packageInfo;
        }
    }

    @ProxyMethod("getPackageUid")
    public static class GetPackageUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getProviderInfo")
    public static class GetProviderInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            int flags = MethodParameterUtils.toInt(args[1]);
            ProviderInfo providerInfo = BlackBoxCore.getBPackageManager().getProviderInfo(componentName, flags, BlackBoxCore.getUserId());
            if (providerInfo != null)
                return providerInfo;
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getReceiverInfo")
    public static class GetReceiverInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            int flags = MethodParameterUtils.toInt(args[1]);
            ActivityInfo receiverInfo = BlackBoxCore.getBPackageManager().getReceiverInfo(componentName, flags, BlackBoxCore.getUserId());
            if (receiverInfo != null)
                return receiverInfo;
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getActivityInfo")
    public static class GetActivityInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            int flags = MethodParameterUtils.toInt(args[1]);
            ActivityInfo activityInfo = BlackBoxCore.getBPackageManager().getActivityInfo(componentName, flags, BlackBoxCore.getUserId());
            if (activityInfo != null)
                return activityInfo;
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getServiceInfo")
    public static class GetServiceInfo extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            int flags = MethodParameterUtils.toInt(args[1]);
            ServiceInfo serviceInfo = BlackBoxCore.getBPackageManager().getServiceInfo(componentName, flags, BlackBoxCore.getUserId());
            if (serviceInfo != null)
                return serviceInfo;
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getInstalledApplications")
    public static class GetInstalledApplications extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int flags = MethodParameterUtils.toInt(args[0]);
            List<ApplicationInfo> installedApplications = BlackBoxCore.getBPackageManager().getInstalledApplications(flags, BlackBoxCore.getUserId());
            if (installedApplications != null) {
                String hostPkg = BlackBoxCore.getHostPkg();
                installedApplications.removeIf(ai -> ai == null || Objects.equals(ai.packageName, hostPkg) || AppSystemEnv.isBlackPackage(ai.packageName));
            }
            return ParceledListSliceCompat.create(installedApplications);
        }
    }

    @ProxyMethod("getInstalledPackages")
    public static class GetInstalledPackages extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int flags = MethodParameterUtils.toInt(args[0]);
            List<PackageInfo> installedPackages = BlackBoxCore.getBPackageManager().getInstalledPackages(flags, BlackBoxCore.getUserId());
            if (installedPackages != null) {
                String hostPkg = BlackBoxCore.getHostPkg();
                installedPackages.removeIf(pi -> pi == null || Objects.equals(pi.packageName, hostPkg) || AppSystemEnv.isBlackPackage(pi.packageName));
            }
            return ParceledListSliceCompat.create(installedPackages);
        }
    }

    @ProxyMethod("getApplicationInfo")
    public static class GetApplicationInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String packageName = (String) args[0];
            int flags = MethodParameterUtils.toInt(args[1]);

            if (AppSystemEnv.isBlackPackage(packageName)) {
                return null;
            }
            if (Objects.equals(packageName, BlackBoxCore.getHostPkg())) {
                return method.invoke(who, args);
            }

            ApplicationInfo applicationInfo = BlackBoxCore.getBPackageManager().getApplicationInfo(packageName, flags, BlackBoxCore.getUserId());
            if (applicationInfo != null) {
                return applicationInfo;
            }
            if (GmsCore.VENDING_PKG.equals(packageName)
                    || GmsCore.GSF_PKG.equals(packageName)
                    || GmsCore.GMS_PKG.equals(packageName)) {
                ApplicationInfo hostApp = BlackBoxCore.getContext().getApplicationInfo();
                ApplicationInfo ai = new ApplicationInfo(hostApp);
                ai.packageName = packageName;
                ai.processName = packageName;
                ai.flags = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_HAS_CODE;
                ai.enabled = true;
                String extracted = GmsCore.extractBundledApkIfNeeded(packageName);
                if (extracted != null && !extracted.isEmpty()) {
                    ai.sourceDir = extracted;
                    ai.publicSourceDir = extracted;
                }
                return ai;
            }
            if (AppSystemEnv.isOpenPackage(packageName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("queryContentProviders")
    public static class QueryContentProviders extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int flags = MethodParameterUtils.toInt(args[2]);
            List<ProviderInfo> providers = BlackBoxCore.getBPackageManager().
                    queryContentProviders(BlackBoxCore.getAppProcessName(), BlackBoxCore.getBUid(), flags, BlackBoxCore.getUserId());
            return ParceledListSliceCompat.create(providers);
        }
    }

    @ProxyMethod("queryIntentReceivers")
    public static class QueryBroadcastReceivers extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = MethodParameterUtils.getFirstParam(args, Intent.class);
            String type = MethodParameterUtils.getFirstParam(args, String.class);
            Integer flagsObj = MethodParameterUtils.getFirstParam(args, Integer.class);
            int flags = flagsObj != null ? flagsObj : (args != null && args.length > 2 ? MethodParameterUtils.toInt(args[2]) : 0);
            List<ResolveInfo> resolves = BlackBoxCore.getBPackageManager().queryBroadcastReceivers(intent, flags, type, BActivityThread.getUserId());
            Slog.d(TAG, "queryIntentReceivers: " + resolves);

            if (BuildCompat.isN()) {
                return ParceledListSliceCompat.create(resolves);
            }
            return resolves;
        }
    }

    @ProxyMethod("resolveContentProvider")
    public static class ResolveContentProvider extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String authority = (String) args[0];
            int flags = MethodParameterUtils.toInt(args[1]);
            ProviderInfo providerInfo = BlackBoxCore.getBPackageManager().resolveContentProvider(authority, flags, BActivityThread.getUserId());
            if (providerInfo == null) {
                return method.invoke(who, args);
            }
            return providerInfo;
        }
    }

    @ProxyMethod("canRequestPackageInstalls")
    public static class CanRequestPackageInstalls extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getPackagesForUid")
    public static class GetPackagesForUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int origUid = (Integer) args[0];
            int uid = origUid;
            if (uid == BlackBoxCore.getHostUid()) {
                args[0] = BActivityThread.getBUid();
                uid = (int) args[0];
            }
            String[] packagesForUid = BlackBoxCore.getBPackageManager().getPackagesForUid(uid);
            if (origUid == BlackBoxCore.getHostUid()) {
                String currentPkg = BActivityThread.getAppPackageName();
                if (GmsCore.isGoogleAppOrService(currentPkg)) {
                    java.util.LinkedHashSet<String> allPkgs = new java.util.LinkedHashSet<>();
                    if (currentPkg != null && !currentPkg.isEmpty()) {
                        allPkgs.add(currentPkg);
                    }
                    if (packagesForUid != null) {
                        for (String p : packagesForUid) {
                            if (p != null && !p.isEmpty()) allPkgs.add(p);
                        }
                    }
                    try {
                        List<PackageInfo> installed = BlackBoxCore.getBPackageManager().getInstalledPackages(0, BActivityThread.getUserId());
                        if (installed != null) {
                            for (PackageInfo pi : installed) {
                                if (pi != null && pi.packageName != null) {
                                    allPkgs.add(pi.packageName);
                                }
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                    allPkgs.add(GmsCore.GMS_PKG);
                    allPkgs.add(GmsCore.VENDING_PKG);
                    packagesForUid = allPkgs.toArray(new String[0]);
                } else if (currentPkg != null && !currentPkg.isEmpty()) {
                    packagesForUid = new String[]{currentPkg};
                }
            }
            Slog.d(TAG, args[0] + " , " + BActivityThread.getAppProcessName() + " GetPackagesForUid: " + Arrays.toString(packagesForUid));
            return packagesForUid;
        }
    }

    @ProxyMethod("getInstallerPackageName")
    public static class GetInstallerPackageName extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return "com.android.vending";
        }
    }

    @ProxyMethod("getSharedLibraries")
    public static class GetSharedLibraries extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return ParceledListSliceCompat.create(new ArrayList<>());
        }
    }

    @ProxyMethod("getComponentEnabledSetting")
    public static class getComponentEnabledSetting extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            String packageName = componentName.getPackageName();
            ApplicationInfo applicationInfo = BlackBoxCore.getBPackageManager().getApplicationInfo(packageName, 0, BActivityThread.getUserId());
            if (applicationInfo != null) {
                return PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
            }
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            return PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
        }
    }

    @ProxyMethod("checkPermission")
    public static class SimpleAudioPermissionHook extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return PackageManager.PERMISSION_GRANTED;
        }
    }

    @ProxyMethod("checkUidPermission")
    public static class CheckUidPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return PackageManager.PERMISSION_GRANTED;
        }
    }

    @ProxyMethod("checkSelfPermission")
    public static class CheckSelfPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return PackageManager.PERMISSION_GRANTED;
        }
    }

    @ProxyMethod("shouldShowRequestPermissionRationale")
    public static class ShouldShowRequestPermissionRationale extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String permission = (String) args[0];
            if (isAutoGrantedPermission(permission)) {
                return false;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("requestPermissions")
    public static class RequestPermissions extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return method.invoke(who, args);
        }
    }

    private static boolean isAutoGrantedPermission(String permission) {
        if (permission == null) return false;
        if (isAudioPermission(permission)
                || isStorageOrMediaPermission(permission)
                || isNotificationOrXiaomiPermission(permission)
                || isGmsOrAccountPermission(permission)) {
            return true;
        }
        return false;
    }

    private static boolean isGmsOrAccountPermission(String permission) {
        if (permission == null) return false;
        return permission.equals("android.permission.FAKE_PACKAGE_SIGNATURE")
                || permission.equals("org.microg.gms.PROVISION")
                || permission.equals("org.microg.gms.EXTENDED_ACCESS")
                || permission.equals(android.Manifest.permission.ACCOUNT_MANAGER)
                || permission.equals(android.Manifest.permission.GET_ACCOUNTS)
                || permission.equals(android.Manifest.permission.GET_ACCOUNTS_PRIVILEGED)
                || permission.equals("android.permission.AUTHENTICATE_ACCOUNTS")
                || permission.equals("android.permission.USE_CREDENTIALS")
                || permission.equals("android.permission.MANAGE_ACCOUNTS")
                || permission.equals("android.permission.INTERACT_ACROSS_USERS")
                || permission.equals("android.permission.INTERACT_ACROSS_USERS_FULL")
                || permission.equals("android.permission.MANAGE_USERS")
                || permission.equals(android.Manifest.permission.READ_PHONE_STATE)
                || permission.equals("android.permission.READ_PRIVILEGED_PHONE_STATE")
                || permission.equals(android.Manifest.permission.RECEIVE_SMS)
                || permission.equals(android.Manifest.permission.ACCESS_FINE_LOCATION)
                || permission.equals(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                || permission.equals("android.permission.ACCESS_BACKGROUND_LOCATION")
                || permission.equals(android.Manifest.permission.INTERNET)
                || permission.equals(android.Manifest.permission.ACCESS_NETWORK_STATE)
                || permission.equals(android.Manifest.permission.WAKE_LOCK)
                || permission.startsWith("com.google.android.")
                || permission.startsWith("org.microg.gms.");
    }

    private static boolean isStorageOrMediaPermission(String permission) {
        if (permission == null) return false;
        if (permission.equals(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                || permission.equals(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return true;
        }
        if (permission.equals(android.Manifest.permission.READ_MEDIA_AUDIO)
                || permission.equals(android.Manifest.permission.READ_MEDIA_VIDEO)
                || permission.equals(android.Manifest.permission.READ_MEDIA_IMAGES)
                || permission.equals("android.permission.READ_MEDIA_VISUAL")
                || permission.equals("android.permission.READ_MEDIA_AURAL")
                || permission.equals(android.Manifest.permission.ACCESS_MEDIA_LOCATION)) {
            return true;
        }
        if (permission.equals("android.permission.READ_MEDIA_AUDIO_USER_SELECTED")
                || permission.equals("android.permission.READ_MEDIA_VIDEO_USER_SELECTED")
                || permission.equals("android.permission.READ_MEDIA_IMAGES_USER_SELECTED")
                || permission.equals("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
                || permission.equals("android.permission.READ_MEDIA_AURAL_USER_SELECTED")) {
            return true;
        }
        return false;
    }

    private static boolean isAudioPermission(String permission) {
        if (permission == null) return false;
        return permission.equals(android.Manifest.permission.RECORD_AUDIO)
                || permission.equals(android.Manifest.permission.CAPTURE_AUDIO_OUTPUT)
                || permission.equals(android.Manifest.permission.MODIFY_AUDIO_SETTINGS)
                || permission.equals("android.permission.FOREGROUND_SERVICE_MICROPHONE")
                || permission.equals("android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION")
                || permission.equals("android.permission.FOREGROUND_SERVICE_CAMERA")
                || permission.equals("android.permission.FOREGROUND_SERVICE_LOCATION")
                || permission.equals("android.permission.FOREGROUND_SERVICE_HEALTH")
                || permission.equals("android.permission.FOREGROUND_SERVICE_DATA_SYNC")
                || permission.equals("android.permission.FOREGROUND_SERVICE_SPECIAL_USE")
                || permission.equals("android.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED")
                || permission.equals("android.permission.FOREGROUND_SERVICE_PHONE_CALL")
                || permission.equals("android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE");
    }

    private static boolean isNotificationOrXiaomiPermission(String permission) {
        if (permission == null) return false;
        if (permission.equals("android.permission.POST_NOTIFICATIONS")) {
            return true;
        }
        if (permission.equals("miui.permission.USE_INTERNAL_GENERAL_API") ||
            permission.equals("miui.permission.OPTIMIZE_POWER") ||
            permission.equals("miui.permission.RUN_IN_BACKGROUND") ||
            permission.equals("miui.permission.POST_NOTIFICATIONS") ||
            permission.equals("miui.permission.AUTO_START") ||
            permission.equals("miui.permission.BACKGROUND_POPUP_WINDOW") ||
            permission.equals("miui.permission.SHOW_WHEN_LOCKED") ||
            permission.equals("miui.permission.TURN_SCREEN_ON")) {
            return true;
        }
        return false;
    }

    @ProxyMethod("setSplashScreenTheme")
    public static class SetSplashScreenTheme extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            boolean isXiaomi = BuildCompat.isMIUI() ||
                              Build.MANUFACTURER.toLowerCase().contains("xiaomi") ||
                              Build.BRAND.toLowerCase().contains("xiaomi") ||
                              Build.DISPLAY.toLowerCase().contains("hyperos");
            if (isXiaomi) {
                return null;
            }
            try {
                return method.invoke(who, args);
            } catch (SecurityException e) {
                return null;
            } catch (Exception e) {
                if (e.getCause() instanceof SecurityException) {
                    return null;
                }
                throw e;
            }
        }
    }

    public static class XiaomiSecurityBypass extends MethodHook {
        private static final String[] XIAOMI_SECURITY_METHODS = {
            "setApplicationEnabledSetting",
            "setComponentEnabledSetting",
            "setInstallLocation",
            "setInstallerPackageName",
            "setPackageStoppedState",
            "setSystemAppState",
            "setApplicationCategoryHint",
            "setApplicationHiddenSettingAsUser",
            "setBlockUninstallForUser",
            "setDefaultBrowserPackageNameAsUser",
            "setDistractingPackageRestrictionsAsUser",
            "setPackagesSuspendedAsUser",
            "setUpdateAvailable",
            "setRequiredForSystemUser",
            "setSystemAppHiddenUntilInstalled",
            "setHarmfulAppWarningEnabled",
            "setKeepUninstalledPackages",
            "verifyIntentFilter",
            "verifyPendingInstall",
            "extendVerificationTimeout",
            "setDefaultHomeActivity",
            "resetApplicationPreferences",
            "clearApplicationProfileData",
            "clearApplicationUserData",
            "deleteApplicationCacheFiles",
            "deleteApplicationCacheFilesAsUser",
            "freeStorageAndNotify",
            "freeStorage",
            "movePackage",
            "movePackageToSd",
            "movePrimaryStorage"
        };

        @Override
        public boolean isEnable() {
            return BuildCompat.isMIUI() ||
                   Build.MANUFACTURER.toLowerCase().contains("xiaomi") ||
                   Build.BRAND.toLowerCase().contains("xiaomi") ||
                   Build.DISPLAY.toLowerCase().contains("hyperos");
        }

        @Override
        public String getMethodName() {
            return null;
        }

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            for (String securityMethod : XIAOMI_SECURITY_METHODS) {
                if (securityMethod.equals(methodName)) {
                    if (method.getReturnType() == void.class) {
                        return null;
                    } else if (method.getReturnType() == boolean.class) {
                        return true;
                    } else if (method.getReturnType() == int.class) {
                        return 0;
                    } else {
                        return null;
                    }
                }
            }
            try {
                return method.invoke(who, args);
            } catch (SecurityException e) {
                if (method.getReturnType() == boolean.class) {
                    return false;
                } else if (method.getReturnType() == int.class) {
                    return -1;
                } else {
                    return null;
                }
            } catch (Exception e) {
                if (e.getCause() instanceof SecurityException) {
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    } else if (method.getReturnType() == int.class) {
                        return -1;
                    } else {
                        return null;
                    }
                }
                throw e;
            }
        }
    }
}
