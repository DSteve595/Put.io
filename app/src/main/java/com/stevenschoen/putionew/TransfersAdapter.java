package com.stevenschoen.putionew;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nineoldandroids.view.ViewHelper;

import java.util.List;

public class TransfersAdapter extends ArrayAdapter<PutioTransferLayout> {

	Context context;
	int layoutResourceId;
	List<PutioTransferLayout> data = null;

	public TransfersAdapter(Context context, int layoutResourceId, List<PutioTransferLayout> data) {
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		this.data = data;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		TransferHolder holder = null;
		
		if (row == null) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			try {
				row = inflater.inflate(layoutResourceId, parent, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			holder = new TransferHolder();
			holder.textName = (TextView) row.findViewById(R.id.text_transfer_name);
			holder.textDownValue = (TextView) row.findViewById(R.id.text_transfer_downValue);
			holder.textDownUnit = (TextView) row.findViewById(R.id.text_transfer_downUnit);
			holder.textUpValue = (TextView) row.findViewById(R.id.text_transfer_upValue);
			holder.textUpUnit = (TextView) row.findViewById(R.id.text_transfer_upUnit);
			holder.textPercent = (TextView) row.findViewById(R.id.text_transfer_percent);
			holder.imgStatusIcon = (ImageView) row.findViewById(R.id.img_transfer_icon);
			holder.statusLoading = (ProgressBar) row.findViewById(R.id.transfer_statusLoading);
			
			holder.greenBar = row.findViewById(R.id.transfer_greenbar);
			holder.textMessage = (TextView) row.findViewById(R.id.text_transfer_message);
			holder.speedHolder = row.findViewById(R.id.transfer_speedHolder);

			row.setTag(holder);
		} else {
			holder = (TransferHolder) row.getTag();
		}

		PutioTransferLayout transfer = data.get(position);
		
		holder.textName.setText(transfer.name);
		
		String[] downStrings = PutioUtils.humanReadableByteCountArray(data.get(position).downSpeed, true);
		holder.textDownValue.setText(downStrings[0]);
		holder.textDownUnit.setText(downStrings[1] + "/sec");
		String[] upStrings = PutioUtils.humanReadableByteCountArray(data.get(position).upSpeed, true);
		holder.textUpValue.setText(upStrings[0]);
		holder.textUpUnit.setText(upStrings[1] + "/sec");
		
		int percentInt = data.get(position).percentDone;
		String percentString = Integer.toString(percentInt);
		
		if (data.get(position).status.matches("COMPLETED")) {
			holder.imgStatusIcon.setImageResource(R.drawable.ic_transfer_done);
			holder.imgStatusIcon.setVisibility(View.VISIBLE);
			holder.statusLoading.setVisibility(View.INVISIBLE);
			holder.textMessage.setVisibility(View.GONE);
			holder.speedHolder.setVisibility(View.VISIBLE);
			holder.textPercent.setText(percentString + "%");
			holder.greenBar.setBackgroundColor(Color.parseColor("#2000FF00"));
			ViewHelper.setPivotX(holder.greenBar, 0);
			ViewHelper.setScaleX(holder.greenBar, 1f);
			holder.textPercent.setTextColor(context.getResources().getColor(R.color.putio_green));
		} else if (data.get(position).status.matches("SEEDING")) {
			holder.imgStatusIcon.setImageResource(R.drawable.ic_transfer_done);
			holder.imgStatusIcon.setVisibility(View.VISIBLE);
			holder.statusLoading.setVisibility(View.INVISIBLE);
			holder.textMessage.setVisibility(View.GONE);
			holder.speedHolder.setVisibility(View.VISIBLE);
			holder.textPercent.setText(percentString + "%");
			holder.greenBar.setBackgroundColor(Color.parseColor("#2000FF00"));
			ViewHelper.setPivotX(holder.greenBar, 0);
			ViewHelper.setScaleX(holder.greenBar, 1f);
			holder.textPercent.setTextColor(context.getResources().getColor(R.color.putio_green));
		} else if (data.get(position).status.matches("ERROR")) {
			holder.imgStatusIcon.setImageResource(R.drawable.ic_transfer_failed);
			holder.imgStatusIcon.setVisibility(View.VISIBLE);
			holder.statusLoading.setVisibility(View.INVISIBLE);
			holder.textMessage.setText(context.getString(R.string.transferfailed));
			holder.textMessage.setVisibility(View.VISIBLE);
			holder.speedHolder.setVisibility(View.GONE);
			holder.textPercent.setText(":(");
			holder.greenBar.setBackgroundColor(context.getResources().getColor(R.color.putio_error));
			ViewHelper.setPivotX(holder.greenBar, 0);
			ViewHelper.setScaleX(holder.greenBar, 1f);
			holder.textPercent.setTextColor(Color.RED);
		} else {
			holder.imgStatusIcon.setVisibility(View.INVISIBLE);
			holder.statusLoading.setVisibility(View.VISIBLE);
			holder.textMessage.setVisibility(View.GONE);
			holder.speedHolder.setVisibility(View.VISIBLE);
			holder.textPercent.setText(percentString + "%");
			holder.greenBar.setBackgroundColor(Color.parseColor("#2000FF00"));
			ViewHelper.setPivotX(holder.greenBar, 0);
			ViewHelper.setScaleX(holder.greenBar, (float) data.get(position).percentDone / 100);
			holder.textPercent.setTextColor(Color.BLACK);
		}

		return row;
	}
	
	static class TransferHolder {
		TextView textName;
		TextView textDownValue;
		TextView textDownUnit;
		TextView textUpValue;
		TextView textUpUnit;
		TextView textPercent;
		ImageView imgStatusIcon;
		ProgressBar statusLoading;
		
		View greenBar;
		TextView textMessage;
		View speedHolder;
	}
}