package com.stevenschoen.putionew;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class FilesAdapter extends ArrayAdapter<PutioFileLayout> {

	Context context;
	List<PutioFileLayout> data = null;

	public FilesAdapter(Context context, List<PutioFileLayout> data) {
		super(context, R.layout.file_putio, data);
		this.context = context;
		this.data = data;
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

			row.setTag(holder);
		} else {
			holder = (FileHolder) row.getTag();
		}

		PutioFileLayout file = data.get(position);
		holder.textName.setText(file.name);
		holder.textDescription.setText(file.description);
		if (file.iconUrl == null) {
			Picasso.with(context).cancelRequest(holder.imgIcon);
			holder.imgIcon.setImageResource(file.iconRes);
		} else {
			holder.imgIcon.setImageResource(0);
			Picasso.with(context).load(file.iconUrl).into(holder.imgIcon);
		}

		return row;
	}
	
	static class FileHolder {
		TextView textName;
		TextView textDescription;
		ImageView imgIcon;
	}
}