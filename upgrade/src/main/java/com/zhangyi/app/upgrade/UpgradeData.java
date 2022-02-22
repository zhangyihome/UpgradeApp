package com.zhangyi.app.upgrade;

import android.os.Parcel;
import android.os.Parcelable;

public class UpgradeData implements Parcelable {

    public final String message;
    public final boolean haveNewVersion;
    public final boolean forceUpgrade;
    public final boolean showDialog;
    public final String newVersionName;
    public final String downloadSize;
    public final String downloadUrl;

    public UpgradeData(String message,
                       boolean haveNewVersion,
                       boolean forceUpgrade,
                       boolean showDialog,
                       String newVersionName,
                       String downloadSize,
                       String downloadUrl) {
        this.message = message;
        this.haveNewVersion = haveNewVersion;
        this.forceUpgrade = forceUpgrade;
        this.showDialog = showDialog;
        this.newVersionName = newVersionName;
        this.downloadSize = downloadSize;
        this.downloadUrl = downloadUrl;
    }


    protected UpgradeData(Parcel in) {
        message = in.readString();
        haveNewVersion = in.readByte() != 0;
        forceUpgrade = in.readByte() != 0;
        showDialog = in.readByte() != 0;
        newVersionName = in.readString();
        downloadSize = in.readString();
        downloadUrl = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(message);
        dest.writeByte((byte) (haveNewVersion ? 1 : 0));
        dest.writeByte((byte) (forceUpgrade ? 1 : 0));
        dest.writeByte((byte) (showDialog ? 1 : 0));
        dest.writeString(newVersionName);
        dest.writeString(downloadSize);
        dest.writeString(downloadUrl);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<UpgradeData> CREATOR = new Creator<UpgradeData>() {
        @Override
        public UpgradeData createFromParcel(Parcel in) {
            return new UpgradeData(in);
        }

        @Override
        public UpgradeData[] newArray(int size) {
            return new UpgradeData[size];
        }
    };
}
