package com.sunshine.freeform.bean;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author sunshine
 * @date 2021/3/14
 */
public class MotionEventBean implements Parcelable, Serializable {

    private static final long serialVersionUID = -7373576824258678549L;
    private int action;

    private float[] xArray;

    private float[] yArray;

    private int flags;

    private int source;

    private int displayId;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.action);
        dest.writeFloatArray(this.xArray);
        dest.writeFloatArray(this.yArray);
        dest.writeInt(this.flags);
        dest.writeInt(this.source);
        dest.writeInt(this.displayId);
    }

    public void readFromParcel(Parcel source) {
        this.action = source.readInt();
        this.xArray = source.createFloatArray();
        this.yArray = source.createFloatArray();
        this.flags = source.readInt();
        this.source = source.readInt();
        this.displayId = source.readInt();
    }

    public MotionEventBean() {

    }

    protected MotionEventBean(Parcel in) {
        this.action = in.readInt();
        this.xArray = in.createFloatArray();
        this.yArray = in.createFloatArray();
        this.flags = in.readInt();
        this.source = in.readInt();
        this.displayId = in.readInt();
    }

    public static final Creator<MotionEventBean> CREATOR = new Creator<MotionEventBean>() {
        @Override
        public MotionEventBean createFromParcel(Parcel source) {
            return new MotionEventBean(source);
        }

        @Override
        public MotionEventBean[] newArray(int size) {
            return new MotionEventBean[size];
        }
    };

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public float[] getXArray() {
        return xArray;
    }

    public void setXArray(float[] xArray) {
        this.xArray = xArray;
    }

    public float[] getYArray() {
        return yArray;
    }

    public void setYArray(float[] yArray) {
        this.yArray = yArray;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getDisplayId() {
        return displayId;
    }

    public void setDisplayId(int displayId) {
        this.displayId = displayId;
    }

    @Override
    public String toString() {
        return "MotionEventBean{" +
                "action=" + action +
                ", xArray=" + Arrays.toString(xArray) +
                ", yArray=" + Arrays.toString(yArray) +
                ", flags=" + flags +
                ", source=" + source +
                ", displayId=" + displayId +
                '}';
    }
}
