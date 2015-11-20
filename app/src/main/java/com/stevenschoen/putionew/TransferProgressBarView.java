package com.stevenschoen.putionew;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.view.View;

public class TransferProgressBarView extends View {

	@ColorInt
	private static final int COLOR_TOP_INCOMPLETE = Color.parseColor("#F5F6F8");
	@ColorInt
	private static final int COLOR_TOP_COMPLETE = Color.parseColor("#ECEEF2");
	@ColorInt
	private static final int COLOR_BOTTOM_INCOMPLETE = Color.parseColor("#CCCCCC");
	@ColorInt
	private static final int COLOR_BOTTOM_COMPLETE = Color.parseColor("#FDCE45");
	@ColorInt
	private static final int COLOR_BOTTOM_ALL_COMPLETE = Color.parseColor("#1FAE7D");
	@ColorInt
	private static final int COLOR_BOTTOM_ERROR = Color.parseColor("#E74C3C");

	private Paint topPaintStart, topPaintEnd, bottomPaintStart, bottomPaintEnd;
	private float bottomThickness;

	private int progress = 0;
	private boolean error = false;

	public TransferProgressBarView(Context context) {
		super(context);
		init();
	}

	public TransferProgressBarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public TransferProgressBarView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	private void init() {
		topPaintStart = new Paint();
		topPaintStart.setStyle(Paint.Style.FILL);
		topPaintEnd = new Paint();
		topPaintEnd.setStyle(Paint.Style.FILL);
		bottomPaintStart = new Paint();
		bottomPaintStart.setStyle(Paint.Style.FILL);
		bottomPaintEnd = new Paint();
		bottomPaintEnd.setStyle(Paint.Style.FILL);

		bottomThickness = PutioUtils.pxFromDp(getContext(), 2);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (error || progress == 0 || progress == 100) {
			if (error) {
				topPaintStart.setColor(COLOR_TOP_INCOMPLETE);
				bottomPaintStart.setColor(COLOR_BOTTOM_ERROR);
			} else if (progress == 0) {
				topPaintStart.setColor(COLOR_TOP_INCOMPLETE);
				bottomPaintStart.setColor(COLOR_BOTTOM_INCOMPLETE);
			} else {
				topPaintStart.setColor(COLOR_TOP_COMPLETE);
				bottomPaintStart.setColor(COLOR_BOTTOM_ALL_COMPLETE);
			}
			canvas.drawRect(0, 0, canvas.getWidth(), (canvas.getHeight()) - bottomThickness, topPaintStart);
			canvas.drawRect(0, (canvas.getHeight()) - bottomThickness, canvas.getWidth(), canvas.getHeight(), bottomPaintStart);
		} else {
			float splitPoint = (canvas.getWidth() * (progress / 100f));

			topPaintStart.setColor(COLOR_TOP_COMPLETE);
			canvas.drawRect(0, 0, splitPoint, (canvas.getHeight()) - bottomThickness, topPaintStart);
			bottomPaintStart.setColor(COLOR_BOTTOM_COMPLETE);
			canvas.drawRect(0, (canvas.getHeight()) - bottomThickness, splitPoint, canvas.getHeight(), bottomPaintStart);

			topPaintEnd.setColor(COLOR_TOP_INCOMPLETE);
			canvas.drawRect(splitPoint, 0, canvas.getWidth(), (canvas.getHeight()) - bottomThickness, topPaintEnd);
			bottomPaintEnd.setColor(COLOR_BOTTOM_INCOMPLETE);
			canvas.drawRect(splitPoint, (canvas.getHeight()) - bottomThickness, canvas.getWidth(), canvas.getHeight(), bottomPaintEnd);
		}
	}

	/**
	 * @param progress Percentage completed
	 */
	public void setProgress(int progress) {
		this.progress = progress;
		invalidate();
	}

	public void setError(boolean error) {
		this.error = error;
		invalidate();
	}
}
