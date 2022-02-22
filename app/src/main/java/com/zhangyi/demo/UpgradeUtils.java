package com.zhangyi.demo;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.zhangyi.app.upgrade.AppDownloadHelper;
import com.zhangyi.app.upgrade.CheckUpgradeCallback;
import com.zhangyi.app.upgrade.Logger;
import com.zhangyi.app.upgrade.UpgradeData;
import com.zhangyi.app.upgrade.UpgradeSDK;
import com.zhangyi.app.upgrade.impl.DefaultVersionLoader;

import java.io.File;

/**
 * 更新下载安装包工具类
 */
public class UpgradeUtils {
    private static final String TAG = "UpgradeUtils";
    private static final String HOST = "http://test.gaea.ksyun.com";
    private static final String CHECK_UPGRADE_PATH = "/api/aiot-node/mdm/latest/version";
    public static final String PACKAGE_NAME = "com.mi.b.mdm.broker.psbc";
    public static final String APP_INSTALL_SUCCESS = "com.mi.b.mdm.broker.psbc.APP_INSTALL_SUCCESS";
    private static volatile boolean taskRunning = false;
    private static final String DOWNLOAD_PATH= Environment.getDownloadCacheDirectory() + "/mdm/download";

    public static void init(Context context) {
        UpgradeSDK.getInstance().init(context, new Logger() {
            @Override
            public void printLog(int i, String s, String s1) {
               Log.d("UpgradeSDK",s+"  =>  "+s1);
            }
        });
        DefaultVersionLoader loader = new DefaultVersionLoader(HOST,
                5, CHECK_UPGRADE_PATH, null,
                "2.0.0", PACKAGE_NAME);
        File downloadDir = new File(DOWNLOAD_PATH);
        if (!downloadDir.exists()) downloadDir.mkdirs();
        UpgradeSDK.getInstance().setDownLoadSavePath(downloadDir.getAbsolutePath() + "/");
        UpgradeSDK.getInstance().setVersionLoader(loader);

    }

    public static void checkUpgrade() {
        if (taskRunning) {
            Log.w(TAG,"CheckUpgrade Task is already running!");
            return;
        }
        taskRunning = true;
        UpgradeSDK.getInstance().checkUpgrade(false, new CheckUpgradeCallback() {
            @Override
            public void onSucceed(UpgradeData upgradeData) {
                Log.i(TAG,"Check Upgrade Version Succeed! Download Url => " + upgradeData.downloadUrl);
                startDownloadApp(upgradeData);
            }

            @Override
            public void onFailed(String s) {
                Log.e(TAG,"Check Upgrade Version Error! Msg => " + s);
                taskRunning = false;
            }

            @Override
            public void onRequestFailed(String s) {
                Log.e(TAG,"Check Upgrade Error! Msg => " + s);
                taskRunning = false;
            }
        });
    }

    /**
     * 开始执行下载逻辑
     *
     * @param upgradeData 升级实体对象
     */
    private static void startDownloadApp(UpgradeData upgradeData) {
        UpgradeSDK.getInstance().getAppDownloadHelper().startDownload(upgradeData.downloadUrl, new AppDownloadHelper.DownloadCallback() {
            @Override
            public boolean needRefreshCacheFile() {
                return true;
            }

            @Override
            public void onProgress(int i) {
                Log.i(TAG, "Downloading progress => " + i);
            }

            @Override
            public void onSucceed(String path) {
                Log.i(TAG,"Upgrade Apk download succeed!    path => " + path);
                Context context = UpgradeSDK.getInstance().getContext();

                Intent intent = new Intent(UpgradeUtils.APP_INSTALL_SUCCESS);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//                安装应用
//                ApplicationManager.getInstance().installPackageWithPendingIntent(path, pendingIntent);

                taskRunning = false;
                Log.w(TAG,"App Upgrade Send Delay Broadcast");
            }

            @Override
            public void onFailed(AppDownloadHelper.DownloadFailedCode downloadFailedCode) {
                Log.e(TAG,"Upgrade Apk download Failed!  Msg => " + downloadFailedCode.toString());
                taskRunning = false;
            }
        });
    }
}
