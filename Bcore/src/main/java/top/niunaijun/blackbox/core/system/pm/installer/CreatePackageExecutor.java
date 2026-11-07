package top.niunaijun.blackbox.core.system.pm.installer;

import java.io.File;

import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.core.system.pm.BPackageSettings;
import top.niunaijun.blackbox.entity.pm.InstallOption;
import top.niunaijun.blackbox.utils.FileUtils;


public class CreatePackageExecutor implements Executor {

    @Override
    public int exec(BPackageSettings ps, InstallOption option, int userId) {
        File appDir = BEnvironment.getAppDir(ps.pkg.packageName);
        String baseCodePath = ps.pkg.baseCodePath;
        boolean baseInsideAppDir = baseCodePath != null && baseCodePath.startsWith(appDir.getAbsolutePath());
        if (!baseInsideAppDir) {
            File baseApk = BEnvironment.getBaseApkDir(ps.pkg.packageName);
            if (baseApk.exists()) {
                baseApk.setWritable(true, true);
            }
            FileUtils.deleteDir(appDir);
        }

        FileUtils.mkdirs(appDir);
        FileUtils.mkdirs(BEnvironment.getAppLibDir(ps.pkg.packageName));
        return 0;
    }
}
