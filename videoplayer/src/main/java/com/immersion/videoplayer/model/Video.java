package com.immersion.videoplayer.model;

import android.os.Parcelable;

/**
 * Data Object which contains Video file information
 */
public class Video implements Parcelable {
    public String title;

    public String location;

    public String thumbnail;

    public String haptFile;

    public boolean tactileEnabled;

    public int videoType;

    public int elapsedTime = 0;

    public Video() {
    }

    protected Video(android.os.Parcel in) {
        title = in.readString();
        location = in.readString();
        thumbnail = in.readString();
        haptFile = in.readString();
        tactileEnabled = in.readByte() != 0;
        videoType = in.readInt();
        elapsedTime = in.readInt();
    }

    public static final Creator<Video> CREATOR = new Creator<Video>() {
        @Override
        public Video createFromParcel(android.os.Parcel in) {
            return new Video(in);
        }

        @Override
        public Video[] newArray(int size) {
            return new Video[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(android.os.Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(location);
        dest.writeString(thumbnail);
        dest.writeString(haptFile);
        dest.writeByte((byte) (tactileEnabled ? 1 : 0));
        dest.writeInt(videoType);
        dest.writeInt(elapsedTime);
    }
}
