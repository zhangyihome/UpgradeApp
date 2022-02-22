package com.zhangyi.app.upgrade;

public interface AppInstallHelper {

    enum InstallResult {
        SUCCEED_TO_INSTALL, FILE_NOT_AVAILABLE, NO_INSTALL_PERMISSION
    }

    interface InstallCallback {
        /**
         * 安装完成
         * @param result
         */
        void installFinish(InstallResult result);
    }

    void installApk(String apkPath, String fileProviderId, InstallCallback callback);
}
