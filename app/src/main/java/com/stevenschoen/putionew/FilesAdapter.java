package com.stevenschoen.putionew;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.stevenschoen.putionew.model.files.PutioFileData;

import java.util.List;

public class FilesAdapter extends ArrayAdapter<PutioFileData> {

	Context context;

	public FilesAdapter(Context context, List<PutioFileData> data) {
		super(context, R.layout.file_putio, data);
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		FileHolder holder;
		
		if (row == null) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			try {
				row = inflater.inflate(R.layout.file_putio, parent, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			holder = new FileHolder();
			holder.textName = (TextView) row.findViewById(R.id.text_fileListName);
			holder.textDescription = (TextView) row.findViewById(R.id.text_fileListDesc);
			holder.imgIcon = (ImageView) row.findViewById(R.id.img_fileIcon);
            holder.iconAccessed = (ImageView) row.findViewById(R.id.icon_file_accessed);

			row.setTag(holder);
		} else {
			holder = (FileHolder) row.getTag();
		}

		PutioFileData file = getItem(position);
		holder.textName.setText(file.name);
		holder.textDescription.setText(PutioUtils.humanReadableByteCount(file.size, false));
        if (file.icon != null && !file.icon.isEmpty()) {
            Picasso.with(context).load(file.icon).into(holder.imgIcon);
        }
        if (file.isAccessed()) {
            holder.iconAccessed.setVisibility(View.VISIBLE);
        } else {
            holder.iconAccessed.setVisibility(View.GONE);
        }

        return row;
	}

    @Override
    public long getItemId(int position) {
        if (position != ListView.INVALID_POSITION) {
            return getItem(position).id;
        }

        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    static class FileHolder {
		TextView textName;
		TextView textDescription;
		ImageView imgIcon;
        ImageView iconAccessed;
	}
}