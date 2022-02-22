package com.zhangyi.app.upgrade;

import android.content.Context;

public interface UpgradeAPI {

    /**
     * 初始化
     * @param appContext 上下文，使用Application context
     * @param logger 输出log对象，null 不会输出log
     */
    void init(Context appContext, Logger logger);

    /**
     * 设置下载文件路径
     * @param savePath
     */
    void setDownLoadSavePath(String savePath);

    /**
     * 检查升级
     * @param autoShowDialog 自动弹出对话框
     * @param callback 回掉
     */
    void checkUpgrade(boolean autoShowDialog, CheckUpgradeCallback callback);

    /**
     * 设置新版本信息加载对象
     * @param loader loader
     */
    void setVersionLoader(VersionLoader loader);

    /**
     * 设置用户交互界面创建对象
     * @param creator 创见对象
     */
    void setInterfaceCreator(InterfaceCreator creator);

    /**
     * 获取界面交互创建对象
     * @return 创建界面对象
     */
    InterfaceCreator getInterfaceCreator();

    /**
     * 获取上下文
     * @return context
     */
    Context getContext();

    /**
     * 获取下载AppDownloadHelper对象
     */
    AppDownloadHelper getAppDownloadHelper();
}
