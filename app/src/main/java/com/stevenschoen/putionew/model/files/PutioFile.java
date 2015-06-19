package com.stevenschoen.putionew.model.files;

import android.os.Parcel;
import android.os.Parcelable;

import com.stevenschoen.putionew.PutioUtils;

public class PutioFile implements Parcelable {
	public boolean isShared;
	public String name;
	public String icon;
	public String screenshot;
	public String createdAt;
    public String firstAccessedAt;
	public long parentId;
	public boolean isMp4Available;
	public String contentType;
	public long id;
	public long size;
    public String crc32;

    public PutioFile() { }

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
        String streamOrStreamMp4;
        if (mp4) {
            streamOrStreamMp4 = "/mp4/stream";
        } else {
            streamOrStreamMp4 = "/stream";
        }

        return base + streamOrStreamMp4 + utils.tokenWithStuff;
    }

	@Override
	public int describeContents() {
		return 0;
	}
	
	public PutioFile(Parcel in) {
		readFromParcel(in);
	}
	
	private void readFromParcel(Parcel in) {
		this.isShared = (Boolean) in.readValue(ClassLoader.getSystemClassLoader());
		this.name = in.readString();
		this.icon = in.readString();
		this.screenshot = in.readString();
		this.createdAt = in.readString();
        this.firstAccessedAt = in.readString();
		this.parentId = in.readLong();
		this.isMp4Available = (Boolean) in.readValue(ClassLoader.getSystemClassLoader());
		this.contentType = in.readString();
		this.id = in.readLong();
		this.size = in.readLong();
        this.crc32 = in.readString();
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeValue(this.isShared);
		out.writeString(this.name);
		out.writeString(this.icon);
		out.writeString(this.screenshot);
		out.writeString(this.createdAt);
        out.writeString(this.firstAccessedAt);
		out.writeLong(this.parentId);
		out.writeValue(this.isMp4Available);
		out.writeString(this.contentType);
		out.writeLong(this.id);
		out.writeLong(this.size);
        out.writeString(this.crc32);
	}
	
	public static final Creator CREATOR = new Creator() {
		public PutioFile createFromParcel(Parcel in) {
			return new PutioFile(in);
		}

		public PutioFile[] newArray(int size) {
			return new PutioFile[size];
		}
	};

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PutioFile putioFile = (PutioFile) o;

        if (id != putioFile.id) return false;
        if (isMp4Available != putioFile.isMp4Available) return false;
        if (isShared != putioFile.isShared) return false;
        if (parentId != putioFile.parentId) return false;
        if (size != putioFile.size) return false;
        if (contentType != null ? !contentType.equals(putioFile.contentType) : putioFile.contentType != null)
            return false;
        if (createdAt != null ? !createdAt.equals(putioFile.createdAt) : putioFile.createdAt != null) return false;
        if (firstAccessedAt != null ? !firstAccessedAt.equals(putioFile.firstAccessedAt) : putioFile.firstAccessedAt != null)
            return false;
        if (icon != null ? !icon.equals(putioFile.icon) : putioFile.icon != null) return false;
        if (name != null ? !name.equals(putioFile.name) : putioFile.name != null) return false;
        if (screenshot != null ? !screenshot.equals(putioFile.screenshot) : putioFile.screenshot != null) return false;
        if (crc32 != null ? !crc32.equals(putioFile.crc32) : putioFile.crc32 != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (isShared ? 1 : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (icon != null ? icon.hashCode() : 0);
        result = 31 * result + (screenshot != null ? screenshot.hashCode() : 0);
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (firstAccessedAt != null ? firstAccessedAt.hashCode() : 0);
        result = 31 * result + (int) (parentId ^ (parentId >>> 32));
        result = 31 * result + (isMp4Available ? 1 : 0);
        result = 31 * result + (contentType != null ? contentType.hashCode() : 0);
        result = 31 * result + (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (crc32 != null ? crc32.hashCode() : 0);
        return result;
    }
}