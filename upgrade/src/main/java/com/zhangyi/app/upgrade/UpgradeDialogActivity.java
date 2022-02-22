package com.zhangyi.app.upgrade;


import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RelativeLayout;

import androidx.core.app.ActivityCompat;

import com.zhangyi.app.upgrade.impl.DefaultAppDownloadHelper;
import com.zhangyi.app.upgrade.impl.DefaultAppInstallHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UpgradeDialogActivity extends Activity implements DialogInterface {

    public interface OnPermissionResult {
        void onFinish(boolean succeed);
    }

    static final String ARG_UPGRADE_DATA = "arg_upd";
    static final String ARG_SAVE_PATH = "arg_save_path";

    private final int REQ_INSTALL_PKG = 1;

    private List<OnPermissionResult> callbackRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        UpgradeData upd = getIntent().getParcelableExtra(ARG_UPGRADE_DATA);
        String mDownLoadSavePath = getIntent().getStringExtra(ARG_SAVE_PATH);
        if (upd == null || !upd.haveNewVersion) {
            finish();
            return;
        }
        String distPath=mDownLoadSavePath;
        if(TextUtils.isEmpty(distPath)){
            distPath=getCacheDir().getAbsolutePath();
        }
        String distApkFile = new File(distPath, "upgrade.apk").getAbsolutePath();
        View v = UpgradeSDK.getInstance().getInterfaceCreator().createDialogView(
                upd,
                getLayoutInflater(),
                this,
                this,
                new DefaultAppDownloadHelper(distApkFile),
                new DefaultAppInstallHelper(this));
        RelativeLayout rl = new RelativeLayout(this);
        rl.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        setContentView(rl);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        RelativeLayout.LayoutParams pm = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pm.addRule(RelativeLayout.CENTER_IN_PARENT);
        rl.addView(v, pm);
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));



    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_INSTALL_PKG) {
            if (callbackRef == null || callbackRef.isEmpty()) return;
            boolean succeed = (grantResults != null && grantResults.length >= 1) &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED;
            for (OnPermissionResult ca : callbackRef) {
                ca.onFinish(succeed);
            }
            callbackRef.clear();
            callbackRef = null;
        }
    }

    public void requestInstallPackagePermission(OnPermissionResult callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (callbackRef == null) {
                callbackRef = new ArrayList<>();
            }
            callbackRef.add(callback);
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.REQUEST_INSTALL_PACKAGES},
                    REQ_INSTALL_PKG);
        } else {
            callback.onFinish(true);
        }
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void cancel() {
        finish();
    }

    @Override
    public void dismiss() {
        finish();
    }
}