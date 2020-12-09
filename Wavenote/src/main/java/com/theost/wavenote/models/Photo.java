package com.theost.wavenote.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class Photo implements Serializable, Parcelable {

    String id;
    String name;
    String uri;
    String date;

    public Photo(String id, String name, String uri, String date) {
        this.id = id;
        this.name = name;
        this.uri = uri;
        this.date = date;
    }

    protected Photo(Parcel in) {
        id = in.readString();
        name = in.readString();
        uri = in.readString();
        date = in.readString();
    }

    public static final Creator<Photo> CREATOR = new Creator<Photo>() {
        @Override
        public Photo createFromParcel(Parcel in) {
            return new Photo(in);
        }

        @Override
        public Photo[] newArray(int size) {
            return new Photo[size];
        }
    };

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    @SuppressWarnings("deprecation")
    public Bitmap getBitmap(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), Uri.fromFile(new File(uri))));
            } else {
                return MediaStore.Images.Media.getBitmap(context.getContentResolver(), Uri.fromFile(new File(uri)));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public String getDate() {
        return date;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(uri);
        dest.writeString(date);
    }
}
