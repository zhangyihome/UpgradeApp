package com.zhangyi.app.upgrade.impl;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.zhangyi.app.upgrade.CheckUpgradeCallback;
import com.zhangyi.app.upgrade.CustomHeaderProvider;
import com.zhangyi.app.upgrade.UpgradeData;
import com.zhangyi.app.upgrade.UpgradeSDK;
import com.zhangyi.app.upgrade.VersionLoader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

public class DefaultVersionLoader implements VersionLoader {


    public static final String TAG = "version_loader";

    private static final String NET_WIFI = "WIFI";
    private static final String NET_MOBILE = "MOBILE";
    private static final String NET_UNKNOWN = "UNKNOWN";
    private static final String NET_NONE = "NONE";

    private static final Charset UTF_8 = Charset.forName("utf-8");
    //默认的检查新版本接口
    public static final String defaultLoadLastVersionPath = "/api/aiot-estate/app/common/appVersionCheck";

    public final String host;

    //      { label: '全部', value: 0 },
//      { label: '金山云爱居', value: 1 },
//      { label: '金山云栖小居', value: 2 },
//      { label: '中控屏', value: 3 },
//      { label: 'AIHOUSE-APP', value: 4 },
//      { label: '邮储MDM', value: 5 },
    public final int APP_TYPE;
    //检查新版本接口
    public final String loadLastVersionPath;
    //默认的获取下载安装包地址的接口，如果更新接口没有返回下载地址，则用这个地址获取下载安装包的链接
    private final String getDownloadPathUrl;

    private String appVersionName;
    private String packageName;
    private String uid;

    private ConnectivityManager mCm;
    private CustomHeaderProvider mCustomHeader;


    private final Handler MAIN_H = new Handler(Looper.getMainLooper());

    public DefaultVersionLoader(String host, int type, String downloadPath, String appVersionName,
                                String packageName) {
        this(host, type, defaultLoadLastVersionPath, downloadPath, null, null);
    }

    public DefaultVersionLoader(String host,
                                int type,
                                String loadLastVersionPath,
                                String getDownloadPathUrl,
                                String appVersionName,
                                String packageName) {
        this.host = host;
        this.APP_TYPE = type;
        this.loadLastVersionPath = loadLastVersionPath;
        this.getDownloadPathUrl = getDownloadPathUrl;
        this.appVersionName = appVersionName;
        this.packageName = packageName;
        Context context = UpgradeSDK.getInstance().getContext();
        mCm = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (this.packageName == null || this.appVersionName == null) {
            try {
                PackageInfo packageInfo = context.getApplicationContext()
                        .getPackageManager()
                        .getPackageInfo(context.getPackageName(), 0);
                this.appVersionName = packageInfo.versionName;
                this.packageName = packageInfo.packageName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void setAppUID(String uid) {
        this.uid = uid;
    }

    public void setCustomHeaderProvider(CustomHeaderProvider provider) {
        mCustomHeader = provider;
    }


    @Override
    public void loadVersionInfo(CheckUpgradeCallback callback) {
        String netType;
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            netType = getConnectedTypeForM();
        } else {
            netType = getConnectedType();
        }
        Map<String, String> customHeaders;
        if (mCustomHeader != null) {
            customHeaders = mCustomHeader.getCustomHeader();
        } else {
            customHeaders = null;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataOutputStream dos = null;
                HttpURLConnection connection = null;
                InputStream is = null;
                try {
                    URL url = new URL(host + loadLastVersionPath);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(15 * 1000);
                    connection.setReadTimeout(15 * 1000);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("ACCOUNT_TYPE", "ksc");
                    if (customHeaders != null) {
                        for (Map.Entry<String, String> kv : customHeaders.entrySet()) {
                            connection.setRequestProperty(kv.getKey(), kv.getValue());
                        }
                    }

                    connection.setDoOutput(true);
                    connection.setInstanceFollowRedirects(true);


                    byte[] params = getParams();
                    connection.connect();

                    dos = new DataOutputStream(connection.getOutputStream());
                    dos.write(params);
                    dos.flush();

                    if (connection.getResponseCode() == 200) {
                        // 获取返回的数据
                        is = connection.getInputStream();
                        String result = readServerData(is);
                        LogPrint.printLog(Log.INFO, TAG, "load version message:" + result);
                        JSONObject js = new JSONObject(result);
                        if (isServerRespSucceed(js)) {
                            JSONObject data = js.optJSONObject("data");
                            if (data == null) {
                                callbackFailed(true, "获取数据失败");
                            } else {
                                handleServerData(data);
                            }
                        } else {
                            String msg = js.optString("message");
                            callbackFailed(false, msg);
                        }
                    } else {
                        String msg = connection.getResponseMessage();
                        callbackFailed(true, msg);
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    callbackFailed(true, e.getMessage());
                } finally {
                    if (dos != null) {
                        try {
                            dos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
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
                }
            }

            private void handleServerData(JSONObject js) {
                // 升级模式，0不升级、1推荐升级、2强制升级
                String upgradeMode = js.isNull("upgradeMode") ? null : js.optString("upgradeMode");
                // 升级弹框，0打开升级开关、1关闭升级开关
                String popout = js.isNull("popout") ? null : js.optString("popout");
                // app版本，e.g. 1.2.3
                String appVersion = js.isNull("appVersion") ? null : js.optString("appVersion");
                // apk下载地址 or appstore下载页地址
                String aplDownloadUrl = js.isNull("url") ? null : js.optString("url");
                // app更新信息
                String appUpgradeInfo = js.isNull("info") ? null : js.optString("info");
                // app 大小
                String pkgSize = js.isNull("pkgSize") ? null : js.optString("pkgSize");

                boolean hasNewVersion = "1".equals(upgradeMode) || "2".equals(upgradeMode);
                boolean forceUpgrade = "2".equals(upgradeMode);
                boolean showDialog = "0".equals(popout);

                if (aplDownloadUrl == null || aplDownloadUrl.isEmpty()) {
                    DownloadUrlData data = loadApkDownloadUrl(customHeaders);
                    if (!data.isSucceed()) {
                        callbackFailed(!data.requestSucceed, data.message);
                        return;
                    }
                    aplDownloadUrl = data.url;
                }
                if (aplDownloadUrl == null || aplDownloadUrl.isEmpty()) {
                    callbackFailed(true, "获取下载链接失败");
                    return;
                }

                UpgradeData data = new UpgradeData(appUpgradeInfo,
                        hasNewVersion,
                        forceUpgrade,
                        showDialog,
                        appVersion,
                        pkgSize,
                        aplDownloadUrl);
                callbackSucceedData(data);
            }

            private void callbackFailed(boolean requestFailed, String msg) {
                MAIN_H.post(new Runnable() {
                    @Override
                    public void run() {
                        if (requestFailed) {
                            callback.onRequestFailed(msg);
                        } else {
                            callback.onFailed(msg);
                        }
                    }
                });
            }

            private void callbackSucceedData(UpgradeData data) {
                MAIN_H.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSucceed(data);
                    }
                });
            }

            private byte[] getParams() throws JSONException {
                JSONObject js = new JSONObject();
                js.put("netWorkType", netType);
                js.put("packageName", packageName);
                js.put("phoneModel", Build.MODEL);
                js.put("manufacture", Build.MANUFACTURER);
                js.put("optSysVer", Build.VERSION.RELEASE);
                js.put("app", APP_TYPE);
                js.put("optSys", 1);
                js.put("appVersion", appVersionName);
                if (uid != null) {
                    js.put("appUid", uid);
                }
                return js.toString().getBytes(UTF_8);
            }
        }).start();
    }


    private String getConnectedType() {
        NetworkInfo net = mCm.getActiveNetworkInfo();
        if (net != null) {
            switch (net.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    return NET_WIFI;
                case ConnectivityManager.TYPE_MOBILE:
                    return NET_MOBILE;
                default:
                    return NET_UNKNOWN;
            }
        }
        return NET_NONE;
    }

    private String getConnectedTypeForM() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Network network = mCm.getActiveNetwork();
            if (null == network) {
                return NET_NONE;
            }
            NetworkCapabilities capabilities = mCm.getNetworkCapabilities(network);
            if (null == capabilities) {
                return NET_NONE;
            }
            boolean isCell = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            boolean isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            if (isCell) {
                return NET_MOBILE;
            }
            if (isWifi) {
                return NET_WIFI;
            }
            return NET_UNKNOWN;
        }
        throw new RuntimeException("sdk int not support:" + Build.VERSION.SDK_INT);
    }
    //根据getDownloadPathUrl获取下载安装包的地址
    private DownloadUrlData loadApkDownloadUrl(Map<String, String> customHeader) {
        DownloadUrlData data = new DownloadUrlData();
        InputStream is = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(host + getDownloadPathUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("ACCOUNT_TYPE", "ksc");
            if (customHeader != null) {
                for (Map.Entry<String, String> kv : customHeader.entrySet()) {
                    connection.setRequestProperty(kv.getKey(), kv.getValue());
                }
            }
            connection.connect();
            is = connection.getInputStream();
            String result = readServerData(is);
            LogPrint.printLog(Log.INFO, TAG, "load url message:" + result);
            JSONObject js = new JSONObject(result);
            data.requestSucceed = true;
            if (isServerRespSucceed(js)) {
                data.code = 200;
                data.url = js.optString("data");
            } else {
                data.code = js.optInt("code");

                data.message = js.isNull("message") ? "未知错误" : js.optString("message");
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            data.code = -1;
            data.message = e.getMessage();
            data.requestSucceed = false;
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
        }
        return data;
    }

    private boolean isServerRespSucceed(JSONObject js) {
        if (js == null) return false;
        int code = js.optInt("code");
        return code == 200;
    }


    private String readServerData(InputStream is) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            is.close();
            byte[] byteArray = bos.toByteArray();
            return new String(byteArray, UTF_8);
        }
    }

    static class DownloadUrlData {
        boolean requestSucceed;
        int code;
        String message;
        String url;

        boolean isSucceed() {
            return requestSucceed && code == 200;
        }
    }


}
