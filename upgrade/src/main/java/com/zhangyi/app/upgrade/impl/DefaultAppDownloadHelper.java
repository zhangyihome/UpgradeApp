package com.zhangyi.app.upgrade.impl;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.zhangyi.app.upgrade.AppDownloadHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DefaultAppDownloadHelper implements AppDownloadHelper {
    private final String distFile;

    public DefaultAppDownloadHelper(String distFile) {
        this.distFile = distFile;
    }

    @Override
    public void startDownload(String url, DownloadCallback callback) {
        new DownloadThread(url, distFile, callback);
    }


    static class DownloadThread extends Thread {
        private final int MSG_FAILED = 1;
        private final int MSG_PROGRESS = 2;
        private final int MSG_SUCCEED = 3;

        private final Handler MAIN_POSTER;

        private final AppDownloadHelper.DownloadCallback callback;
        private final String url, distFile;
        private boolean looperPrepareByMe;

        /**
         * @param url 下载url
         * @param distFile 目标文件，如果已经存在会删除旧文件
         * @param callback 回掉
         */
        DownloadThread(String url,
                       String distFile,
                       DownloadCallback callback) {
            this.callback = callback;
            this.url = url;
            this.distFile = distFile;

            if (Looper.myLooper() == null) {
                Looper.prepare();
                looperPrepareByMe = true;
            } else {
                looperPrepareByMe = false;
            }

            MAIN_POSTER = new Handler(Looper.myLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    if (callback == null) return;
                    switch (msg.what) {
                        case MSG_FAILED:
                            callback.onFailed((DownloadFailedCode) msg.obj);
                            quitLooper();
                            break;
                        case MSG_PROGRESS:
                            callback.onProgress((Integer) msg.obj);
                            break;
                        case MSG_SUCCEED:
                            callback.onSucceed((String) msg.obj);
                            quitLooper();
                            break;
                        default:
                            break;
                    }
                }
            };
            start();
            if (looperPrepareByMe) {
                Looper.loop();
            }
        }

        @Override
        public void run() {
            HttpURLConnection connection = null;
            FileOutputStream fos = null;
            InputStream is = null;

            File file = new File(distFile);
            long currentDistFileSize = -1;
            if (file.exists()) {
                currentDistFileSize = file.length();
            }
            try {
                URL u = new URL(url);
                connection = (HttpURLConnection) u.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(false);
                connection.setDoInput(true);
                connection.setConnectTimeout(40000);
                connection.setReadTimeout(40000);
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Accept", "*/*");
                connection.connect();
                long totalLength = 0;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    totalLength = connection.getContentLengthLong();
                } else {
                    totalLength = connection.getContentLength();
                }
                boolean needDownload = true;
                if (currentDistFileSize != -1 && currentDistFileSize == totalLength) {
                    // 已经存在了大小相同文件
                    if (callback != null) {
                        needDownload = callback.needRefreshCacheFile();
                    }
                }

                is = connection.getInputStream();
                if (needDownload) {
                    fos = new FileOutputStream(distFile);
                    byte[] buffer = new byte[1024];

                    int readLength;
                    int totalRead = 0;
                    int lastPercent = 0;
                    while ((readLength = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, readLength);
                        totalRead += readLength;
                        int percent = (int)((100) * ((float) totalRead / (float) totalLength));
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            callbackProgress(lastPercent);
                        }
                    }
                }
                callbackSucceed();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                callbackFailed(DownloadFailedCode.URL_ERROR);
            } catch (IOException e) {
                e.printStackTrace();
                callbackFailed(DownloadFailedCode.NET_ERROR);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void callbackFailed(DownloadFailedCode code) {
            if (callback == null) return;
            Message msg = Message.obtain();
            msg.what = MSG_FAILED;
            msg.obj = code;
            MAIN_POSTER.sendMessage(msg);
        }

        private void callbackProgress(int progress) {
            if (callback == null) return;
            Message msg = Message.obtain();
            msg.what = MSG_PROGRESS;
            msg.obj = progress;
            MAIN_POSTER.sendMessage(msg);
        }

        private void callbackSucceed() {
            if (callback == null) return;
            Message msg = Message.obtain();
            msg.what = MSG_SUCCEED;
            msg.obj = distFile;
            MAIN_POSTER.sendMessage(msg);
        }

        private void quitLooper() {
            if (looperPrepareByMe && MAIN_POSTER.getLooper() != null) {
                if (MAIN_POSTER.getLooper() != Looper.getMainLooper()) {
                    MAIN_POSTER.getLooper().quit();
                }
            }
        }
    }
}
