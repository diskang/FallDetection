package com.bendeming.falldetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class PhoneView extends View {

	private Camera mCamera;
	private Bitmap face;
	private final Matrix mMatrix = new Matrix ();
	private final Paint mPaint = new Paint ();
	private int centerX, centerY;
	private int deltaX, deltaY;
	private int bWidth, bHeight;

	public PhoneView(Context context) {

		super(context);

		this.init();

	}

	public PhoneView(Context context, AttributeSet attrs) {

		super(context, attrs);

		this.init();

	}

	private void init() {

		this.setWillNotDraw(false);
		this.mCamera = new Camera();
		this.mPaint.setAntiAlias(true);
		this.face = BitmapFactory.decodeResource (this.getResources (), R.drawable.phone);
		this.bWidth = this.face.getWidth ();
		this.bHeight = this.face.getHeight ();
		this.centerX = this.bWidth >> 1;
		this.centerY = this.bHeight >> 1;

	}

	void rotate (int degreeX, int degreeY) {

		this.deltaX += degreeX;
		this.deltaY += degreeY;
		this.mCamera.save ();
		this.mCamera.rotateY (this.deltaX);
		this.mCamera.rotateX (-this.deltaY);
		this.mCamera.translate (0, 0,-this.centerX);
		this.mCamera.getMatrix (this.mMatrix);
		this.mCamera.restore();
		this.mMatrix.preTranslate (-this.centerX,-this.centerY);
		this.mMatrix.postTranslate (this.centerX, this.centerY);
		this.mCamera.save();
		this.postInvalidate();

	}

	@Override
	public void dispatchDraw (Canvas canvas) {
		super.dispatchDraw (canvas);
		canvas.drawBitmap (this.face, this.mMatrix, this.mPaint);
	}

}
