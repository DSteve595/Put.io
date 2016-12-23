package com.stevenschoen.putionew.transfers;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.model.transfers.PutioTransfer;

import org.joda.time.Period;
import org.joda.time.Seconds;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.List;

public class TransfersAdapter extends RecyclerView.Adapter<TransfersAdapter.TransferHolder> {

	private PeriodFormatter remainingFormatter;
	private PeriodFormatter remainingWithoutSecondsFormatter;
	private PeriodFormatter remainingWithoutMinutesFormatter;

	private OnItemClickListener itemClickListener;
	private OnItemLongClickListener itemLongClickListener;

	private List<PutioTransfer> data;

	public TransfersAdapter(List<PutioTransfer> data) {
		super();
		this.data = data;
		setHasStableIds(true);

		remainingFormatter = new PeriodFormatterBuilder()
				.appendDays().appendSuffix("d").appendSeparator(" ")
				.appendHours().appendSuffix("h").appendSeparator(" ")
				.appendMinutes().appendSuffix("m").appendSeparator(" ")
				.appendSeconds().appendSuffix("s")
				.toFormatter();
		remainingWithoutSecondsFormatter = new PeriodFormatterBuilder()
				.appendDays().appendSuffix("d").appendSeparator(" ")
				.appendHours().appendSuffix("h").appendSeparator(" ")
				.appendMinutes().appendSuffix("m")
				.toFormatter();
		remainingWithoutMinutesFormatter = new PeriodFormatterBuilder()
				.appendDays().appendSuffix("d").appendSeparator(" ")
				.appendHours().appendSuffix("h")
				.toFormatter();
	}

	String formatRemainingTime(long remainingSeconds) {
		Period period = new Period(Seconds.seconds((int) remainingSeconds)).normalizedStandard();
		if (period.getDays() > 0) {
			return remainingWithoutMinutesFormatter.print(period);
		} else if (period.getHours() > 0) {
			return remainingWithoutSecondsFormatter.print(period);
		} else {
			return remainingFormatter.print(period);
		}
	}

	@Override
	public TransferHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.transfer, parent, false);

		return new TransferHolder(view);
	}

	@Override
	public void onBindViewHolder(TransferHolder holder, final int position) {
		PutioTransfer transfer = data.get(position);

		View.OnClickListener onClickListener;
		if (itemClickListener != null) {
			onClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					itemClickListener.onItemClick(v, position);
				}
			};
		} else {
			onClickListener = null;
		}
		holder.itemView.setOnClickListener(onClickListener);
		View.OnLongClickListener onLongClickListener;
		if (itemLongClickListener != null) {
			onLongClickListener = new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					itemLongClickListener.onItemLongClick(v, position);
					return true;
				}
			};
		} else {
			onLongClickListener = null;
		}
		holder.itemView.setOnLongClickListener(onLongClickListener);

		holder.buttonView.setOnClickListener(onClickListener);

		holder.progressBar.setProgress(transfer.percentDone);

		holder.textName.setText(transfer.name);

		String downString = PutioUtils.humanReadableByteCount(transfer.downSpeed, false);
		holder.textDown.setText(holder.textDown.getResources().getString(R.string.x_per_sec, downString));
		String upString = PutioUtils.humanReadableByteCount(transfer.upSpeed, false);
		holder.textUp.setText(holder.textUp.getResources().getString(R.string.x_per_sec, upString));

		switch (transfer.status) {
			case "COMPLETED":
			case "SEEDING":
				holder.progressBar.setError(false);
				holder.imgStatusIcon.setImageResource(R.drawable.ic_transfer_done);
				holder.imgStatusIcon.setVisibility(View.VISIBLE);
				holder.statusLoading.setVisibility(View.INVISIBLE);
				holder.textMessage.setVisibility(View.GONE);
				holder.buttonView.setVisibility(View.VISIBLE);
				holder.downHolder.setVisibility(View.GONE);
				holder.upHolder.setVisibility(View.VISIBLE);
				if (transfer.currentRatio != 0) {
					holder.textRatio.setText(holder.textRatio.getContext().getString(R.string.ratio_is, transfer.currentRatio));
					holder.ratioHolder.setVisibility(View.VISIBLE);
				} else {
					holder.ratioHolder.setVisibility(View.GONE);
				}
				holder.textRemaining.setVisibility(View.GONE);
				holder.textRemaining.setTextColor(holder.textRemaining.getContext().getResources().getColor(R.color.putio_green));
				break;
			case "ERROR":
				holder.progressBar.setError(true);
				holder.imgStatusIcon.setImageResource(R.drawable.ic_transfer_failed);
				holder.imgStatusIcon.setVisibility(View.VISIBLE);
				holder.statusLoading.setVisibility(View.INVISIBLE);
				holder.textMessage.setText(holder.textMessage.getContext().getString(R.string.transferfailed));
				holder.textMessage.setVisibility(View.VISIBLE);
				holder.buttonView.setVisibility(View.GONE);
				holder.downHolder.setVisibility(View.GONE);
				holder.upHolder.setVisibility(View.GONE);
				holder.ratioHolder.setVisibility(View.GONE);
				holder.textRemaining.setVisibility(View.VISIBLE);
				holder.textRemaining.setText(":(");
				holder.textRemaining.setTextColor(Color.RED);
				break;
			default:
				holder.progressBar.setError(false);
				holder.imgStatusIcon.setVisibility(View.INVISIBLE);
				holder.statusLoading.setVisibility(View.VISIBLE);
				holder.textMessage.setVisibility(View.GONE);
				holder.buttonView.setVisibility(View.GONE);
				holder.downHolder.setVisibility(View.VISIBLE);
				holder.upHolder.setVisibility(View.VISIBLE);
				holder.ratioHolder.setVisibility(View.GONE);
				String remainingString = "";
				long remainingSeconds = transfer.estimatedTime;
				if (remainingSeconds > 0) {
					remainingString = formatRemainingTime(remainingSeconds);
				}
				holder.textRemaining.setVisibility(View.VISIBLE);
				holder.textRemaining.setText(remainingString);
				holder.textRemaining.setTextColor(Color.BLACK);
		}
	}

	@Override
	public int getItemCount() {
		return data.size();
	}

	@Override
	public long getItemId(int position) {
		return data.get(position).id;
	}

	public static class TransferHolder extends RecyclerView.ViewHolder {
		View root;
		
		TextView textName;
		TextView textDown;
		TextView textUp;
		TextView textRatio;
		TextView textRemaining;
		ImageView imgStatusIcon;
		ProgressBar statusLoading;
		Button buttonView;

		TransferProgressBarView progressBar;
		TextView textMessage;
		View downHolder;
		View upHolder;
		View ratioHolder;

		public TransferHolder(View itemView) {
			super(itemView);

			root = itemView;
			textName = (TextView) itemView.findViewById(R.id.text_transfer_name);
			textDown = (TextView) itemView.findViewById(R.id.text_transfer_down);
			textUp = (TextView) itemView.findViewById(R.id.text_transfer_up);
			textRatio = (TextView) itemView.findViewById(R.id.text_transfer_ratio);
			textRemaining = (TextView) itemView.findViewById(R.id.text_transfer_remaining);
			imgStatusIcon = (ImageView) itemView.findViewById(R.id.img_transfer_icon);
			statusLoading = (ProgressBar) itemView.findViewById(R.id.transfer_statusLoading);
			buttonView = (Button) itemView.findViewById(R.id.transfer_view_button);

			progressBar = (TransferProgressBarView) itemView.findViewById(R.id.transfer_progressbar);
			textMessage = (TextView) itemView.findViewById(R.id.text_transfer_message);
			downHolder = itemView.findViewById(R.id.holder_transfer_down);
			upHolder = itemView.findViewById(R.id.holder_transfer_up);
			ratioHolder = itemView.findViewById(R.id.holder_transfer_ratio);
		}
	}

	public void setOnItemClickListener(OnItemClickListener itemClickListener) {
		this.itemClickListener = itemClickListener;
	}

	public void setOnItemLongClickListener(OnItemLongClickListener itemLongClickListener) {
		this.itemLongClickListener = itemLongClickListener;
	}

	public interface OnItemClickListener {
		void onItemClick(View view, int position);
	}
	public interface OnItemLongClickListener {
		void onItemLongClick(View view, int position);
	}
}
