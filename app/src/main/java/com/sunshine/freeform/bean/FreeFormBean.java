package com.sunshine.freeform.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author sunshine
 * @date 2021/3/12
 */
public class FreeFormBean implements Parcelable {

    private String packageName;

    private String command;

    private boolean compatibleMode;

    public FreeFormBean(String packageName, String command, boolean compatibleMode) {
        this.packageName = packageName;
        this.command = command;
        this.compatibleMode = compatibleMode;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isCompatibleMode() {
        return compatibleMode;
    }

    public void setCompatibleMode(boolean compatibleMode) {
        this.compatibleMode = compatibleMode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.packageName);
        dest.writeString(this.command);
        dest.writeByte(this.compatibleMode ? (byte) 1 : (byte) 0);
    }

    public void readFromParcel(Parcel source) {
        this.packageName = source.readString();
        this.command = source.readString();
        this.compatibleMode = source.readByte() != 0;
    }

    protected FreeFormBean(Parcel in) {
        this.packageName = in.readString();
        this.command = in.readString();
        this.compatibleMode = in.readByte() != 0;
    }

    public static final Creator<FreeFormBean> CREATOR = new Creator<FreeFormBean>() {
        @Override
        public FreeFormBean createFromParcel(Parcel source) {
            return new FreeFormBean(source);
        }

        @Override
        public FreeFormBean[] newArray(int size) {
            return new FreeFormBean[size];
        }
    };
}
