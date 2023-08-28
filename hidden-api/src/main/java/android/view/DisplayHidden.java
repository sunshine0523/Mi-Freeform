package android.view;

import android.os.Parcel;
import android.os.Parcelable;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(Display.class)
public class DisplayHidden {
    public static final int TYPE_OVERLAY = 4;

    public static final int STATE_ON = 2;

    public static final class Mode implements Parcelable {
        public static final Mode[] EMPTY_ARRAY = new Mode[0];
        private Mode(Parcel in) {

        }

        public static final Creator<Mode> CREATOR = new Creator<Mode>() {
            @Override
            public Mode createFromParcel(Parcel in) {
                return new Mode(in);
            }

            @Override
            public Mode[] newArray(int size) {
                return new Mode[size];
            }
        };

        @Override
        public void writeToParcel(Parcel dest, int flags) {

        }

        @Override
        public int describeContents() {
            return 0;
        }

        public int getPhysicalWidth() {
            throw new RuntimeException("Stub!");
        }

        public int getPhysicalHeight() {
            throw new RuntimeException("Stub!");
        }

        public int getModeId() {
            throw new RuntimeException("Stub!");
        }
    }
}
