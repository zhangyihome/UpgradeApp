package com.zhangyi.app.upgrade.impl;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.zhangyi.app.upgrade.AppInstallHelper;
import com.zhangyi.app.upgrade.UpgradeDialogActivity;

import java.io.File;

public class DefaultAppInstallHelper implements AppInstallHelper {

    private final UpgradeDialogActivity context;

    public DefaultAppInstallHelper(UpgradeDialogActivity context) {
        this.context = context;
    }

    @Override
    public void installApk(String apkPath, String fileProviderId, InstallCallback callback) {
        if (apkPath == null) {
            if (callback != null) {
                callback.installFinish(InstallResult.FILE_NOT_AVAILABLE);
            }
            return;
        }
        File file = new File(apkPath);
        if (!file.exists()) {
            if (callback != null) {
                callback.installFinish(InstallResult.FILE_NOT_AVAILABLE);
            }
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            boolean canInstallPackage = context.getPackageManager().canRequestPackageInstalls();
            if (!canInstallPackage) {
                // 不能安装应用
                context.requestInstallPackagePermission(new UpgradeDialogActivity.OnPermissionResult() {
                    @Override
                    public void onFinish(boolean succeed) {
                        if (succeed) {
                            toInstallPackage(file, fileProviderId, callback);
                        } else {
                            startInstallPermissionSettingActivity();
                            if (callback != null) {
                                callback.installFinish(InstallResult.NO_INSTALL_PERMISSION);
                            }
                        }
                    }
                });
            } else {
                toInstallPackage(file, fileProviderId, callback);
            }
        } else {
            toInstallPackage(file, fileProviderId, callback);
        }
    }

    /**
     * 安装app，务必先请求权限
     *
     * @param appFile 下载app的文件
     */
    private void toInstallPackage(File appFile, String fileProviderID, InstallCallback callback) {
        if (appFile == null) {
            if (callback != null) {
                callback.installFinish(InstallResult.FILE_NOT_AVAILABLE);
            }
            return;
        }
        if (!appFile.exists()) {
            if (callback != null) {
                callback.installFinish(InstallResult.FILE_NOT_AVAILABLE);
            }
            return;
        }
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 7.0 以上

            Uri uri = FileProvider.getUriForFile(context,
                    fileProviderID, appFile);
            context.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.setDataAndType(uri, "application/vnd.android.package-archive");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 8.0以上
                boolean canInstall = context.getPackageManager().canRequestPackageInstalls();
                if (canInstall) {
                    context.startActivity(install);
                } else {
                    Toast.makeText(context, "无安装权限，打开安装权限后重试",
                            Toast.LENGTH_SHORT).show();
                    if (callback != null) {
                        callback.installFinish(InstallResult.NO_INSTALL_PERMISSION);
                    }
                    return;
                }
            } else {
                context.startActivity(install);
            }
        } else {
            install.setDataAndType(Uri.fromFile(appFile),
                    "application/vnd.android.package-archive");
            context.startActivity(install);
        }
        if (callback != null) {
            callback.installFinish(InstallResult.SUCCEED_TO_INSTALL);
        }
    }

    private void startInstallPermissionSettingActivity() {
        Intent intent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivityForResult(intent, 1);
        }
    }

}
