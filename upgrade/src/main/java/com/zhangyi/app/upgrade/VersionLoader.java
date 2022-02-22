package com.zhangyi.app.upgrade;

public interface VersionLoader {

    void loadVersionInfo(CheckUpgradeCallback callback);
}
