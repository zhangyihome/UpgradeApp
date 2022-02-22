package com.zhangyi.app.upgrade;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;

public interface InterfaceCreator {

    View createDialogView(UpgradeData data,
                          LayoutInflater inflater,
                          Context context,
                          DialogInterface dialog,
                          AppDownloadHelper downloadHelper,
                          AppInstallHelper installHelper);
}
