package com.zhangyi.app.upgrade.impl;

import com.zhangyi.app.upgrade.Logger;

public class LogPrint {

    private static Logger sLogger;

    public static void setLogger(Logger logger) {
        sLogger = logger;
    }

    static void printLog(int priority, String tag, String message) {
        if (sLogger != null) {
            sLogger.printLog(priority, tag, message);
        }
    }
}
