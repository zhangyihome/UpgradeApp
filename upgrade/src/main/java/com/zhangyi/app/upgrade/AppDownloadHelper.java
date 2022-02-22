package com.zhangyi.app.upgrade;

import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;

public interface AppDownloadHelper {

    enum DownloadFailedCode {
        URL_ERROR, NET_ERROR
    }

    interface DownloadCallback {

        @WorkerThread
        boolean needRefreshCacheFile();

        @MainThread
        void onProgress(int progress);

        @MainThread
        void onSucceed(String filePath);

        @MainThread
        void onFailed(DownloadFailedCode message);
    }

    void startDownload(String url, DownloadCallback callback);
}
