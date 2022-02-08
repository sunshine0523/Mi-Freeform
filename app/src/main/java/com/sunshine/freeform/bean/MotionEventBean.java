package com.sunshine.freeform.bean;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * @author sunshine
 * @date 2021/3/14
 */
public class MotionEventBean implements Parcelable, Serializable {

    private static final long serialVersionUID = -7373576824258678549L;

    //
    private int action;

    private float[] xArray;

    private float[] yArray;

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
        dest.writeInt(this.displayId);
    }

    public void readFromParcel(Parcel source) {
        this.action = source.readInt();
        this.xArray = source.createFloatArray();
        this.yArray = source.createFloatArray();
        this.displayId = source.readInt();
    }

    public MotionEventBean(int action, float[] xArray, float[] yArray, int displayId) {
        this.action = action;
        this.xArray = xArray;
        this.yArray = yArray;
        this.displayId = displayId;
    }

    protected MotionEventBean(Parcel in) {
        this.action = in.readInt();
        this.xArray = in.createFloatArray();
        this.yArray = in.createFloatArray();
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

    public int getDisplayId() {
        return displayId;
    }

    public void setDisplayId(int displayId) {
        this.displayId = displayId;
    }


}
