package com.zhangyi.demo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zhangyi.app.upgrade.AppDownloadHelper;
import com.zhangyi.app.upgrade.AppInstallHelper;
import com.zhangyi.app.upgrade.CheckUpgradeCallback;
import com.zhangyi.app.upgrade.CustomHeaderProvider;
import com.zhangyi.app.upgrade.impl.DefaultVersionLoader;
import com.zhangyi.app.upgrade.InterfaceCreator;
import com.zhangyi.app.upgrade.Logger;
import com.zhangyi.app.upgrade.UpgradeData;
import com.zhangyi.app.upgrade.UpgradeSDK;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UpgradeSDK.getInstance().init(getApplicationContext(), new Logger() {
            @Override
            public void printLog(int priority, String tag, String message) {
                android.util.Log.println(priority, tag, message);
            }
        });

//        DefaultVersionLoader loader = new DefaultVersionLoader("http://test.gaea.ksyun.com",
//                3, "/api/aiot-estate/app/download/lastAppUpdateUrlForPad",
//                "1.1", "com.ksyun.aiiot.controlcenter");
        DefaultVersionLoader loader = new DefaultVersionLoader("http://test.gaea.ksyun.com",
                5, "/api/aiot-node/mdm/latest/version",null,
                "0.0.0", "com.mi.b.mdm.broker.psbc");
        loader.setCustomHeaderProvider(new CustomHeaderProvider() {
            @Override
            public Map<String, String> getCustomHeader() {
                Map<String, String> headers = new HashMap<>();
//                headers.put("AIOT_TOKEN", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250aW51aXR5Q291bnQiOjEsInVpZCI6IjM1NCIsInVzZXJUYWciOiJrczE0ODA1Njg2MjUiLCJsb2dpbk5hbWUiOiIxNjM4MzQ0MDY2Njc2IiwiY29tbXVuaXR5IjoiaG9sZF9teV9iZWVyIiwidHlwZSI6ImFwcCIsImlhdCI6MTYzODM0NDA2NiwiYWNjb3VudCI6IjE1MjAxMDQ1MTA4In0.DK351Ihq4V2uZCDou91GnAyb0Gi47P3CIof3uytNIT0");
//                headers.put("ACCOUNT_TYPE", "ksc");
//                headers.put("platform", "1");
//                headers.put("deviceType", "1");
                return headers;
            }
        });
        UpgradeSDK.getInstance().setVersionLoader(loader);
        UpgradeSDK.getInstance().setInterfaceCreator(new InterfaceCreator() {

            String filePathCache;

            @Override
            public View createDialogView(UpgradeData data,
                                         LayoutInflater inflater,
                                         Context context,
                                         DialogInterface dialog,
                                         AppDownloadHelper downloadHelper,
                                         AppInstallHelper installHelper) {
                View v = inflater.inflate(R.layout.dlg_view, null, false);
                TextView tvMsg = v.findViewById(R.id.tv_message);
                tvMsg.setText(data.message);
                TextView tvCancel = v.findViewById(R.id.tv_cancel);
                if (data.forceUpgrade) {
                    tvCancel.setVisibility(View.GONE);
                }
                ProgressBar pb = v.findViewById(R.id.pb);

                TextView tvConfirm = v.findViewById(R.id.tv_confirm);
                tvConfirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        downloadHelper.startDownload(data.downloadUrl,
                                new AppDownloadHelper.DownloadCallback() {

                                    @Override
                                    public boolean needRefreshCacheFile() {
                                        return filePathCache == null;
                                    }

                                    @Override
                                    public void onProgress(int progress) {
                                        pb.setProgress(progress);
                                    }

                                    @Override
                                    public void onSucceed(String filePath) {
                                        filePathCache = filePath;
                                        installHelper.installApk(filePath, "com.zhiyuan.test.fileprovider",
                                                new AppInstallHelper.InstallCallback() {
                                                    @Override
                                                    public void installFinish(AppInstallHelper.InstallResult succeed) {

                                                    }
                                                });
                                    }

                                    @Override
                                    public void onFailed(AppDownloadHelper.DownloadFailedCode message) {

                                    }
                                });
                    }
                });
                return v;
            }
        });
        UpgradeSDK.getInstance().checkUpgrade(true, new CheckUpgradeCallback() {
            @Override
            public void onSucceed(UpgradeData data) {
                Log.d("zhangyi","===onSucceed======="+data.downloadUrl);

                UpgradeSDK.getInstance().getAppDownloadHelper().startDownload(data.downloadUrl, new AppDownloadHelper.DownloadCallback() {
                    @Override
                    public boolean needRefreshCacheFile() {
                        return false;
                    }

                    @Override
                    public void onProgress(int progress) {

                    }

                    @Override
                    public void onSucceed(String filePath) {
                        Log.d("zhangyi","===onSucceed======="+filePath);
                    }

                    @Override
                    public void onFailed(AppDownloadHelper.DownloadFailedCode message) {

                    }
                });
            }

            @Override
            public void onFailed(String message) {

            }

            @Override
            public void onRequestFailed(String message) {

            }
        });
    }

    public static  void installApk(Context context,String downloadApk) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File file = new File(downloadApk);
        Log.i("installApk","安装路径=="+downloadApk);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri apkUri = FileProvider.getUriForFile(context, UpgradeUtils.PACKAGE_NAME+".fileprovider", file);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri = Uri.fromFile(file);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
        }
        context.startActivity(intent);

    }
}