package com.zhangyi.app.upgrade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.zhangyi.app.upgrade.impl.DefaultAppDownloadHelper;
import com.zhangyi.app.upgrade.impl.LogPrint;

import java.io.File;

public class UpgradeSDK implements UpgradeAPI {

    private volatile static UpgradeAPI sInstance;

    private Context mAppContext;
    private VersionLoader mVersionLoader;
    private InterfaceCreator mInterfaceCreator;
    private String mDownLoadSavePath="";

    public static UpgradeAPI getInstance() {
        if (sInstance == null) {
            synchronized (UpgradeSDK.class) {
                if (sInstance == null) {
                    sInstance = new UpgradeSDK();
                }
            }
        }
        return sInstance;
    }

    @Override
    public void init(Context appContext, Logger logger) {
        mAppContext = appContext;
        LogPrint.setLogger(logger);
    }

    @Override
    public void setDownLoadSavePath(String savePath){
        mDownLoadSavePath=savePath;
    }

    @Override
    public void checkUpgrade(boolean autoShowDialog, CheckUpgradeCallback callback) {
        mVersionLoader.loadVersionInfo(new CheckUpgradeCallback() {
            @Override
            public void onSucceed(UpgradeData data) {
                if (callback != null) {
                    callback.onSucceed(data);
                }
                if (autoShowDialog) {
                    if (data != null) {
                        if (data.haveNewVersion) {
                            // 跳转登陆对话框Activity
                            showDialog(data);
                        }
                    }
                }
            }

            @Override
            public void onFailed(String message) {
                if (callback != null) {
                    callback.onFailed(message);
                }
            }

            @Override
            public void onRequestFailed(String message) {
                if (callback != null) {
                    callback.onRequestFailed(message);
                }
            }
        });
    }

    @Override
    public void setVersionLoader(VersionLoader loader) {
        mVersionLoader = loader;
    }

    @Override
    public void setInterfaceCreator(InterfaceCreator creator) {
        mInterfaceCreator = creator;
    }

    @Override
    public InterfaceCreator getInterfaceCreator() {
        return mInterfaceCreator;
    }

    @Override
    public Context getContext() {
        return mAppContext;
    }

    private void showDialog(UpgradeData d) {
        Intent it = new Intent(mAppContext, UpgradeDialogActivity.class);
        it.putExtra(UpgradeDialogActivity.ARG_UPGRADE_DATA, d);
        it.putExtra(UpgradeDialogActivity.ARG_SAVE_PATH, mDownLoadSavePath);
        if (!(mAppContext instanceof Activity)) {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        mAppContext.startActivity(it);
    }

    public AppDownloadHelper getAppDownloadHelper(){
        String distPath=mDownLoadSavePath;
        if(TextUtils.isEmpty(mDownLoadSavePath)){
            distPath=mAppContext.getCacheDir().getAbsolutePath();
        }
        String distApkFile = new File(distPath, "upgrade.apk").getAbsolutePath();
        return new DefaultAppDownloadHelper(distApkFile);
    }


}
