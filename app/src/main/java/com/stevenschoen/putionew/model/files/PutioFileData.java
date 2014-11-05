package com.stevenschoen.putionew.model.files;

import android.os.Parcel;
import android.os.Parcelable;

import com.stevenschoen.putionew.PutioUtils;

public class PutioFileData implements Parcelable {
	public boolean isShared;
	public String name;
	public String icon;
	public String screenshot;
	public String createdAt;
    public String firstAccessedAt;
	public int parentId;
	public boolean isMp4Available;
	public String contentType;
	public int id;
	public long size;

    public PutioFileData() { }

	public boolean isFolder() {
		return contentType.equals("application/x-directory");
	}

    public boolean isMedia() {
        for (int i = 0; i < PutioUtils.streamingMediaTypes.length; i++) {
            if (contentType.startsWith(PutioUtils.streamingMediaTypes[i])) {
                return true;
            }
        }
        return false;
    }

    public boolean isVideo() {
        return contentType.startsWith("video");
    }

    public boolean isMp4() {
        return contentType.equals("video/mp4");
    }

    public boolean isAccessed() {
        return (firstAccessedAt != null && !firstAccessedAt.isEmpty());
    }

    public String getStreamUrl(PutioUtils utils, boolean mp4) {
        String base = PutioUtils.baseUrl + "/files/" + id;
        String streamOrStreamMp4 = "/stream";
        if (mp4) {
            streamOrStreamMp4 += "/mp4";
        }

        return base + streamOrStreamMp4 + utils.tokenWithStuff;
    }

	@Override
	public int describeContents() {
		return 0;
	}
	
	public PutioFileData(Parcel in) {
		readFromParcel(in);
	}
	
	private void readFromParcel(Parcel in) {
		this.isShared = (Boolean) in.readValue(ClassLoader.getSystemClassLoader());
		this.name = in.readString();
		this.icon = in.readString();
		this.screenshot = in.readString();
		this.createdAt = in.readString();
        this.firstAccessedAt = in.readString();
		this.parentId = in.readInt();
		this.isMp4Available = (Boolean) in.readValue(ClassLoader.getSystemClassLoader());
		this.contentType = in.readString();
		this.id = in.readInt();
		this.size = in.readLong();
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeValue(this.isShared);
		out.writeString(this.name);
		out.writeString(this.icon);
		out.writeString(this.screenshot);
		out.writeString(this.createdAt);
        out.writeString(this.firstAccessedAt);
		out.writeInt(this.parentId);
		out.writeValue(this.isMp4Available);
		out.writeString(this.contentType);
		out.writeInt(this.id);
		out.writeLong(this.size);
	}
	
	public static final Creator CREATOR = new Creator() {
		public PutioFileData createFromParcel(Parcel in) {
			return new PutioFileData(in);
		}

		public PutioFileData[] newArray(int size) {
			return new PutioFileData[size];
		}
	};
}