package com.acorn.doublebufferview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnPreDrawListener;

/**
 * 带缓冲效果的view
 * 
 * @author acorn
 *
 */
public class BufferView extends View {

	Paint mPaint = null;

	/** 缓冲区 */
	Bitmap bufferBitmap = null;
	Canvas mCanvas = null;

	protected int width = 0, height = 0;
	private boolean isInit = false;

	public BufferView(Context context) {
		super(context);
		preInit();
		init();
		afterInit();
	}

	public BufferView(Context context, AttributeSet attrs) {
		super(context, attrs);
		preInit();
		init();
		afterInit();
	}

	protected void preInit() {

	}

	private void init() {
		this.getViewTreeObserver().addOnPreDrawListener(// 绘制完毕
				new OnPreDrawListener() {
					public boolean onPreDraw() {
						width = getWidth();
						height = getHeight();
						Log.v("ts", "宽" + width + "," + height);
						if (width > 0 && !isInit) {
							initCanvas();
						}
						getViewTreeObserver().removeOnPreDrawListener(this);
						return false;
					}
				});
	}

	protected void afterInit() {

	}

	private void initCanvas() {
		isInit = true;
		/* 创建屏幕大小的缓冲区 */
		bufferBitmap = getBufferBitmap();
		mCanvas = new Canvas();
		mCanvas.setBitmap(bufferBitmap);
		mPaint = new Paint();
	}

	private Bitmap getBufferBitmap() {
		return Bitmap.createBitmap(width, height, Config.ARGB_8888);
	}

	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// 将bufferBitmap显示到屏幕上
		canvas.drawBitmap(bufferBitmap, 0, 0, mPaint);
	}

}
