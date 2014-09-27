package com.stevenschoen.putionew;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

import com.stevenschoen.putionew.model.files.PutioFileData;

public class PutioFileLayout implements Parcelable {
	public String name;
	public String description;
	public int iconRes;
	public String iconUrl;

    public PutioFileLayout(Resources resources, PutioFileData data) {
        name = data.name;
        description = resources.getString(R.string.size_is, PutioUtils.humanReadableByteCount(
                data.size, false));
        Integer iconResource = PutioFileData.contentTypes.get(data.contentType);
        if (iconResource == null) iconResource = R.drawable.ic_putio_file;
        iconRes = iconResource;
        iconUrl = data.icon;
    }

	@Override
	public int describeContents() {
		return 0;
	}

	public PutioFileLayout(Parcel in) {
		readFromParcel(in);
	}
	
	private void readFromParcel(Parcel in) {
		this.name = in.readString();
		this.description = in.readString();
		this.iconRes = in.readInt();
		this.iconUrl = in.readString();
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.name);
		out.writeString(this.description);
		out.writeInt(this.iconRes);
		out.writeString(this.iconUrl);
	}
	
	public static final Creator CREATOR = new Creator() {
		public PutioFileLayout createFromParcel(Parcel in) {
			return new PutioFileLayout(in);
		}

		public PutioFileLayout[] newArray(int size) {
			return new PutioFileLayout[size];
		}
	};
}