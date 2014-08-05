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

import java.util.List;

public class TransfersAdapter extends ArrayAdapter<PutioTransferLayout> {

	Context context;
	List<PutioTransferLayout> data = null;

	public TransfersAdapter(Context context, List<PutioTransferLayout> data) {
		super(context, R.layout.transfer, data);
		this.context = context;
		this.data = data;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		TransferHolder holder;
		
		if (row == null) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			try {
				row = inflater.inflate(R.layout.transfer, parent, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			holder = new TransferHolder();
			holder.textName = (TextView) row.findViewById(R.id.text_transfer_name);
			holder.textDownValue = (TextView) row.findViewById(R.id.text_transfer_downValue);
			holder.textDownUnit = (TextView) row.findViewById(R.id.text_transfer_downUnit);
			holder.textUpValue = (TextView) row.findViewById(R.id.text_transfer_upValue);
			holder.textUpUnit = (TextView) row.findViewById(R.id.text_transfer_upUnit);
            holder.textRatio = (TextView) row.findViewById(R.id.text_transfer_ratio);
			holder.textPercent = (TextView) row.findViewById(R.id.text_transfer_percent);
			holder.imgStatusIcon = (ImageView) row.findViewById(R.id.img_transfer_icon);
			holder.statusLoading = (ProgressBar) row.findViewById(R.id.transfer_statusLoading);
			
			holder.greenBar = row.findViewById(R.id.transfer_greenbar);
			holder.textMessage = (TextView) row.findViewById(R.id.text_transfer_message);
			holder.downHolder = row.findViewById(R.id.holder_transfer_down);
            holder.upHolder = row.findViewById(R.id.holder_transfer_up);
            holder.ratioHolder = row.findViewById(R.id.holder_transfer_ratio);

			row.setTag(holder);
		} else {
			holder = (TransferHolder) row.getTag();
		}

		PutioTransferLayout transfer = data.get(position);
		
		holder.textName.setText(transfer.name);
		
		String[] downStrings = PutioUtils.humanReadableByteCountArray(transfer.downSpeed, false);
		holder.textDownValue.setText(downStrings[0]);
		holder.textDownUnit.setText(downStrings[1] + "/sec");
		String[] upStrings = PutioUtils.humanReadableByteCountArray(transfer.upSpeed, false);
		holder.textUpValue.setText(upStrings[0]);
		holder.textUpUnit.setText(upStrings[1] + "/sec");
		
		int percentInt = transfer.percentDone;
		String percentString = Integer.toString(percentInt);

        switch (transfer.status) {
            case "COMPLETED":
            case "SEEDING":
                holder.imgStatusIcon.setImageResource(R.drawable.ic_transfer_done);
                holder.imgStatusIcon.setVisibility(View.VISIBLE);
                holder.statusLoading.setVisibility(View.INVISIBLE);
                holder.textMessage.setVisibility(View.GONE);
                holder.downHolder.setVisibility(View.GONE);
                holder.upHolder.setVisibility(View.VISIBLE);
                holder.ratioHolder.setVisibility(View.VISIBLE);
                holder.textRatio.setText(getContext().getString(R.string.ratio_is, String.valueOf(transfer.ratio)));
                holder.textPercent.setText(percentString + "%");
                holder.greenBar.setBackgroundColor(Color.parseColor("#2000FF00"));
                holder.greenBar.setPivotX(0);
                holder.greenBar.setScaleX(1f);
                holder.textPercent.setTextColor(context.getResources().getColor(R.color.putio_green));
                break;
            case "ERROR":
                holder.imgStatusIcon.setImageResource(R.drawable.ic_transfer_failed);
                holder.imgStatusIcon.setVisibility(View.VISIBLE);
                holder.statusLoading.setVisibility(View.INVISIBLE);
                holder.textMessage.setText(context.getString(R.string.transferfailed));
                holder.textMessage.setVisibility(View.VISIBLE);
                holder.downHolder.setVisibility(View.GONE);
                holder.upHolder.setVisibility(View.GONE);
                holder.ratioHolder.setVisibility(View.GONE);
                holder.textPercent.setText(":(");
                holder.greenBar.setBackgroundColor(context.getResources().getColor(R.color.putio_error));
                holder.greenBar.setPivotX(0);
                holder.greenBar.setScaleX(1f);
                holder.textPercent.setTextColor(Color.RED);
                break;
            default:
                holder.imgStatusIcon.setVisibility(View.INVISIBLE);
                holder.statusLoading.setVisibility(View.VISIBLE);
                holder.textMessage.setVisibility(View.GONE);
                holder.downHolder.setVisibility(View.VISIBLE);
                holder.upHolder.setVisibility(View.VISIBLE);
                holder.ratioHolder.setVisibility(View.GONE);
                holder.textPercent.setText(percentString + "%");
                holder.greenBar.setBackgroundColor(Color.parseColor("#2000FF00"));
                holder.greenBar.setPivotX(0);
                holder.greenBar.setScaleX((float) data.get(position).percentDone / 100);
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
        TextView textRatio;
		TextView textPercent;
		ImageView imgStatusIcon;
		ProgressBar statusLoading;
		
		View greenBar;
		TextView textMessage;
		View downHolder;
        View upHolder;
        View ratioHolder;
	}
}