package com.zhangyi.app.upgrade;

public interface CheckUpgradeCallback {

    void onSucceed(UpgradeData data);

    void onFailed(String message);

    void onRequestFailed(String message);
}
