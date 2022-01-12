package com.sunshine.freeform.bean;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * @author sunshine
 * @date 2021/3/14
 */
public class KeyEventBean implements Parcelable, Serializable {

    private static final long serialVersionUID = -7522468671570779952L;
    private int action;

    private int displayId;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.action);
        dest.writeInt(this.displayId);
    }

    public void readFromParcel(Parcel source) {
        this.action = source.readInt();
        this.displayId = source.readInt();
    }

    public KeyEventBean(int action, int displayId) {
        this.action = action;
        this.displayId = displayId;
    }

    protected KeyEventBean(Parcel in) {
        this.action = in.readInt();
        this.displayId = in.readInt();
    }

    public static final Creator<KeyEventBean> CREATOR = new Creator<KeyEventBean>() {
        @Override
        public KeyEventBean createFromParcel(Parcel source) {
            return new KeyEventBean(source);
        }

        @Override
        public KeyEventBean[] newArray(int size) {
            return new KeyEventBean[size];
        }
    };

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public int getDisplayId() {
        return displayId;
    }

    public void setDisplayId(int displayId) {
        this.displayId = displayId;
    }
}
