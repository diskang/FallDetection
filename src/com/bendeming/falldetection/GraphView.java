package com.bendeming.falldetection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.util.AttributeSet;
import android.view.View;

public class GraphView extends View {

	public Paint paint = new Paint();
	public Paint dashedPaint = new Paint();

	private float[] items;
	private float max;

	public GraphView(Context context) {

		super(context);
		this.init();

	}

	public GraphView(Context context, AttributeSet attrs) {

		super(context, attrs);
		this.init();

	}

	private void init() {

		this.items = new float[0];

		this.paint.setColor(Color.BLACK);
		this.paint.setStrokeCap(Cap.ROUND);
		this.paint.setStrokeWidth(10);

		this.dashedPaint.setColor(Color.GRAY);
		this.dashedPaint.setPathEffect(new DashPathEffect(new float[] {10,10}, 0));
		this.dashedPaint.setStrokeWidth(10);

	}

	protected void updateWithValues(float[] items, float max) {

		this.items = items;
		this.max = max;
		this.invalidate();

	}

	@Override
	public void onDraw(Canvas canvas) {

		super.onDraw(canvas);

		if (this.items.length == 0)
			return;

		canvas.drawColor(0x00000000);

		canvas.drawLine(0, this.getHeight() / 2, this.getWidth(), this.getHeight() / 2, this.dashedPaint);

		float[][] actualPoints = new float[this.items.length][2];

		for (int i = 0; i < this.items.length; i++) {

			float[] point = new float[2];
			point[0] = (i / (float)this.items.length) * this.getWidth();

			if (this.items[i] >= 0) {

				point[1] = (this.getHeight() / 2) + ((Math.abs(this.items[i]) / this.max) * (this.getHeight() / 2));

			}

			else {

				point[1] = (this.getHeight() / 2) - ((Math.abs(this.items[i]) / this.max) * (this.getHeight() / 2));

			}

			actualPoints[i] = point;

		}

		for (int i = 0; i < actualPoints.length; i++) {

			canvas.drawPoint(actualPoints[i][0], actualPoints[i][1], this.paint);

			if (i < actualPoints.length - 1)
				canvas.drawLine(actualPoints[i][0], actualPoints[i][1], actualPoints[i + 1][0], actualPoints[i + 1][1], this.paint);

		}

	}

}
