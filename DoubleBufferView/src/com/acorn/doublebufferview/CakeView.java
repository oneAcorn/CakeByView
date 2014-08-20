package com.acorn.doublebufferview;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout.Alignment;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * 使用缓冲方法实现的饼图,因为surfaceview不支持移动(滚动scrollview,动画),所以用这个view
 * 
 * @author acorn
 *
 */
public class CakeView extends BufferView {
	private int BG_COLOR = 0xffffffff;
	private RectF cakeRect;
	private static final float ANGLE_NUM = 3.6f;
	private static final boolean isDrawByAnim = true;
	// /** 饼图占view空间的比率 */
	// private static final float CAKE_RATE = 0.8f;
	/** 文字大小,为了方便用的dp,因为颜色块也用这个尺寸 */
	private static final float TEXT_SIZE = 16;
	/** 说明文字的行间距 */
	private static final float TEXT_LINE_SPACE = 20;
	private final int[] ARC_COLORS = new int[] { 0xff4F50A0, 0xff649B9A,
			0xffF9BB08, 0xffA4529C, 0xffff6f2f, 0xff990099, 0xff999999,
			0xff663300 };
	private Paint paint;
	/** 起始角度 */
	private float startAngle = 0;

	/** 角数组 */
	private List<CakeValue> cakeValues = new ArrayList<CakeValue>();
	/** 传过来的真正的值(而不是比值) */
	private float[] counts;

	/************* 动画 **********/
	private float curAngle;
	/** 当前绘制的项 */
	private int curItem = 0;
	private float[] itemFrame;

	/** 旋转展现动画 */
	private ValueAnimator cakeValueAnimator;
	private int drawCount = 0;
	/** 动画持续时间 */
	private static final int DURATION = 1500;
	/**********************/

	/** 文字位置 */
	private Gravity textGravity = Gravity.bottom;
	private Rect detailRect;
	// /** 布局模式:自动布局,手动布局 */
	// private static final int AUTO_LAYOUT = 0;
	// private static final int HAND_LAYOUT = 1;
	// private int layoutMode = AUTO_LAYOUT;
	/** 靠右模式下,detail的默认宽度 */
	private static final float DEFAULT_DETAIL_WIDTH_FOR_GRAVITY_RIGHT = 80;
	/*********** 点击 *******/
	private float firstDownX, firstDownY, lastDownX, lastDownY;
	private OnItemClickListener l;
	/********************/

	/** 点击效果动画 */
	private ValueAnimator rotaValueAnimator;
	private PropertyValuesHolder rotaValues;
	// 当前点击的item
	private int curClickItem;
	private ValueAnimator highLightValueAnimator;
	private boolean isHighLigntMode = false;
	/** 焦点模式下,向下位移的值 */
	private float HIGHLIGHT_OFFSET = 20f;
	/** 向下偏移的比率(相对于饼图大小) */
	private float OFFSET_RATE = 0.02f;
	/** 模糊色 */
	private static final int FUZZY_COLOR = 0xff999999;
	/** 饼图与信息的间隔 */
	private int LEFT_SPACING = 3;
	private int TOP_SPACING = 15;

	private String unitName = "笔";
	private boolean isShowDecimals = true;
	/** 饼图信息排列方式 */
	private RankType rankType = RankType.RANK_BY_ROW;
	private Paint clearPaint;

	public enum Gravity {
		bottom, right;
	}

	public enum RankType {
		/** 按行排列,每2个换行 */
		RANK_BY_ROW,
		/** 按1列排序 */
		RANK_BY_COLUMN
	}

	public CakeView(Context context) {
		super(context);
	}

	public CakeView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void preInit() {
		super.preInit();
		initPaint();
		initValueAnimator();
	}

	private void initPaint() {
		this.setBackgroundColor(BG_COLOR);
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		paint.setTextSize(dip2px(getContext(), TEXT_SIZE));
		clearPaint = new Paint();
		clearPaint.setAntiAlias(true);
		clearPaint.setColor(BG_COLOR);
	}

	private void initRect(float cakeSize) {
		cakeRect = new RectF();

		if (textGravity == Gravity.bottom && cakeSize != width) {
			float left = ((float) width - cakeSize) / 2;
			cakeRect.set(left, 0, cakeSize + left, cakeSize);
		} else
			cakeRect.set(0, 0, cakeSize, cakeSize);
		HIGHLIGHT_OFFSET = cakeSize * OFFSET_RATE;
		PropertyValuesHolder topHolder = PropertyValuesHolder.ofFloat("top", 0,
				HIGHLIGHT_OFFSET);
		highLightValueAnimator.setValues(topHolder);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			firstDownX = event.getX();
			firstDownY = event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			lastDownX = event.getX();
			lastDownY = event.getY();
			break;
		case MotionEvent.ACTION_UP:
			float deltaX = Math.abs(lastDownX - firstDownX);
			float deltaY = Math.abs(lastDownY - firstDownY);
			// Log.v("ts", "变化的xy:" + deltaX + "," + deltaY);
			if (deltaX < 10 && deltaY < 10 && null != itemFrame) {

				int clickPosition = getClickPosition();
				if (clickPosition == -1)
					break;
				if (cakeValues.get(clickPosition).value != 100f) { // 如果只有一个的情况下,就不高亮
					if (!rotaValueAnimator.isRunning()
							&& !cakeValueAnimator.isRunning()
							&& !highLightValueAnimator.isRunning()
							&& textGravity == Gravity.bottom) {
						if (isHighLigntMode) {
							isHighLigntMode = false;
							highLightValueAnimator.reverse();
							break;
						} else {
							isHighLigntMode = true;
						}

						curClickItem = clickPosition;
						float toRotaAngle = 0;
						float sAngle = startAngle
								+ (clickPosition > 0 ? itemFrame[clickPosition - 1]
										: 0) * ANGLE_NUM;
						float tAngle = startAngle + itemFrame[clickPosition]
								* ANGLE_NUM;
						// 当前点击的扇形的中心点的角度
						float curItemCenterAngle = sAngle + (tAngle - sAngle)
								/ 2;
						toRotaAngle = startAngle + (90 - curItemCenterAngle);
						rotaValues = PropertyValuesHolder.ofFloat("rotation",
								startAngle, toRotaAngle);
						rotaValueAnimator.setDuration(Math
								.abs((int) (toRotaAngle - startAngle)) * 5);
						rotaValueAnimator.setValues(rotaValues);
						rotaValueAnimator.start();
					}
				}
				if (null != l)
					l.onItemClick(clickPosition);
				// l.OnItemClick(mPager.getCurrentItem() %
				// imgUris.length);
			}
			break;
		}
		return true;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (null == cakeValues) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			return;
		}
		int measuredHeight = dip2px(getContext(), 400);
		int measuredWidth = dip2px(getContext(), 340);
		// width模式
		int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
		// height模式
		int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
		int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

		float cakeSize;
		// wrap_content的情况触发,这种情况下specSize就是最大能到的尺寸
		if (textGravity == Gravity.bottom) {
			int totalLines = (int) (cakeValues.size()
					/ (rankType == RankType.RANK_BY_ROW ? 2f : 1f) + 0.5f);
			// 文字高度
			int detailHeight = totalLines
					* dip2px(getContext(), TEXT_LINE_SPACE)
					+ dip2px(getContext(), TOP_SPACING)
					+ dip2px(getContext(), 3);
			int diffSize = widthSpecSize - heightSpecSize;
			// 如果宽>高,并且bottom;
			if (diffSize > 0) {
				measuredHeight = heightSpecSize;
				if (widthSpecMode == MeasureSpec.AT_MOST) {
					measuredWidth = heightSpecSize - detailHeight;
				} else if (widthSpecMode == MeasureSpec.EXACTLY) {
					measuredWidth = widthSpecSize;
				}
				cakeSize = measuredHeight - detailHeight;
			} else {
				if (heightSpecSize - detailHeight > widthSpecSize) {
					measuredWidth = widthSpecSize;
					cakeSize = widthSpecSize;
				} else { // 没地方
					if (widthSpecMode == MeasureSpec.AT_MOST) {
						measuredWidth = heightSpecSize - detailHeight;
					} else if (widthSpecMode == MeasureSpec.EXACTLY) {
						measuredWidth = widthSpecSize;
					}
					cakeSize = heightSpecSize - detailHeight;
				}
				if (heightSpecMode == MeasureSpec.AT_MOST) {
					measuredHeight = (int) (cakeSize + detailHeight);
				} else if (heightSpecMode == MeasureSpec.EXACTLY) {
					measuredHeight = heightSpecSize;
				}
			}
		} else {
			int diffSize = widthSpecSize - heightSpecSize;
			// 如果宽>高,并且bottom;
			if (diffSize > 0) {
				measuredHeight = heightSpecSize;
				if (widthSpecMode == MeasureSpec.AT_MOST) {
					measuredWidth = measuredHeight
							+ dip2px(getContext(),
									DEFAULT_DETAIL_WIDTH_FOR_GRAVITY_RIGHT);
				} else if (widthSpecMode == MeasureSpec.EXACTLY) {
					measuredWidth = widthSpecSize;
				}
				cakeSize = measuredHeight;
			} else {
				measuredWidth = widthSpecSize;
				if (heightSpecMode == MeasureSpec.AT_MOST) {
					measuredHeight = widthSpecSize
							- dip2px(getContext(),
									DEFAULT_DETAIL_WIDTH_FOR_GRAVITY_RIGHT);
				} else if (heightSpecMode == MeasureSpec.EXACTLY) {
					measuredHeight = heightSpecSize;
				}
				cakeSize = widthSpecSize
						- dip2px(getContext(),
								DEFAULT_DETAIL_WIDTH_FOR_GRAVITY_RIGHT);
			}
		}

		initRect(cakeSize);
		// 设置大小
		setMeasuredDimension(measuredWidth, measuredHeight);
	}

	public void setData(List<CakeValue> cakes) {
		if (null != cakeValues) {
			// 初始化cakeValues;
			float sum = getSum(cakes);
			counts = new float[cakes.size()];
			for (int i = 0; i < cakes.size(); i++) {
				counts[i] = cakes.get(i).value;
				float value = 0;
				// if (i == cakes.size() - 1) {
				// float cakeSum = getSum(cakeValues);
				// value = 100f - cakeSum;
				// Log.v("ts", "value:" + value + ",sum" + cakeSum);
				// } else {
				value = cakes.get(i).value / sum * 100;
				// }
				cakeValues.add(new CakeValue(cakes.get(i).content, value, cakes
						.get(i).detail));
			}
			// logCakevalue();
			settleCakeValues(cakeValues.size() - 1);
			// logCakevalue();
			// 初始化itemframe
			itemFrame = new float[cakeValues.size()];
			for (int i = 0; i < cakeValues.size(); i++) {
				if (i == 0) {
					itemFrame[i] = cakeValues.get(i).value;
					continue;
				}
				itemFrame[i] = cakeValues.get(i).value + itemFrame[i - 1];
			}
		}
	}

	
	public void setBgColor(int bgColor) {
		this.BG_COLOR = bgColor;
		this.setBackgroundColor(bgColor);
		if (null != clearPaint)
			clearPaint.setColor(bgColor);
	}

	public void setGravity(Gravity gravity) {
		this.textGravity = gravity;
		// layoutMode = HAND_LAYOUT;
	}

	public void setOnItemClickListener(OnItemClickListener l) {
		this.l = l;
	}

	/**
	 * 设置饼图信息的左间距
	 * 
	 * @param leftSpacing
	 */
	public void setDetailLeftSpacing(int leftSpacing) {
		this.LEFT_SPACING = leftSpacing;
	}

	/**
	 * 设置饼图与饼图信息的间隔
	 * 
	 * @param topSpacing
	 */
	public void setDetailTopSpacing(int topSpacing) {
		this.TOP_SPACING = topSpacing;
	}

	public boolean isShowDecimals() {
		return isShowDecimals;
	}

	/**
	 * 是否显示小数
	 * 
	 * @param isShowDecimals
	 */
	public void setShowDecimals(boolean isShowDecimals) {
		this.isShowDecimals = isShowDecimals;
	}

	/**
	 * 设置饼图信息排列方式
	 * 
	 * @param rankType
	 */
	public void setRankType(RankType rankType) {
		this.rankType = rankType;
	}

	public void setUnitName(String string) {
		this.unitName = string;
	}

	private float getSum(List<CakeValue> mCakes) {
		float sum = 0;
		for (int i = 0; i < mCakes.size(); i++) {
			sum += mCakes.get(i).value;
		}
		return sum;
	}

	private float getSum(List<CakeValue> mCakes, int index) {
		float sum = 0;
		for (int i = 0; i < mCakes.size() && i < index; i++) {
			sum += mCakes.get(i).value;
		}
		return sum;
	}

	/**
	 * 使用递归保证cakeValues的值的总和必为100
	 * 
	 * @param i
	 */
	private void settleCakeValues(int i) {
		// int i = values.size() - 1;
		float sum = getSum(cakeValues, i);
		CakeValue value = cakeValues.get(i);
		if (sum <= 100f) {
			value.value = 100f - sum;
			cakeValues.set(i, value);
		} else {
			value.value = 0;
			settleCakeValues(i - 1);
		}
	}

	private void drawArc() {
		if (null == itemFrame)
			return;
		for (int i = 0; i < curItem; i++) {
			int colorIndex = i % ARC_COLORS.length;
			if (i == cakeValues.size() - 1 && colorIndex == 0) {
				colorIndex = 1;
			}
			paint.setColor(ARC_COLORS[colorIndex]);
			if (i == 0) {
				mCanvas.drawArc(cakeRect, startAngle, cakeValues.get(i).value
						* ANGLE_NUM, true, paint);
				// holder.unlockCanvasAndPost(canvas);
				continue;
			}
			mCanvas.drawArc(cakeRect, itemFrame[i - 1] * ANGLE_NUM,
					cakeValues.get(i).value * ANGLE_NUM, true, paint);
		}
		curItem = getCurItem(curAngle);
		int colorIndex = curItem % ARC_COLORS.length;
		if (curItem == itemFrame.length - 1 && colorIndex == 0) {
			colorIndex = 1;
		}
		paint.setColor(ARC_COLORS[colorIndex]);
		float curStartAngle = 0;
		float curSweepAngle = curAngle;
		if (curItem > 0) {
			curStartAngle = itemFrame[curItem - 1] * ANGLE_NUM;
			curSweepAngle = curAngle - (itemFrame[curItem - 1] * ANGLE_NUM);
		}
		mCanvas.drawArc(cakeRect, curStartAngle, curSweepAngle, true, paint);
		// 画雷达
		if (curAngle + 5 < 360) {
			// paint.setColor(ARC_COLORS[0]);
			// canvas.drawArc(cakeRect, curAngle + 5, 1, true, paint);
		} else {
			drawCakeText();
		}
		if (curAngle >= 360) {
			drawDetail();
		}
	}

	/**
	 * 绘制饼图上的文字
	 */
	private void drawCakeText() {
		paint.setColor(0xffffffff);
		float textSize;
		textSize = (cakeRect.right - cakeRect.left) * 0.03f;
		if (textSize > 18) {
			textSize = 18;
		} else if (textSize < 10) {
			textSize = 10;
		}
		paint.setTextSize(dip2px(getContext(), textSize));
		for (int i = 0; i < cakeValues.size(); i++) {
			float sAngle, tAngle;
			if (i == 0) {
				sAngle = 0;
				tAngle = cakeValues.get(i).value;
			} else {
				sAngle = itemFrame[i - 1];
				tAngle = itemFrame[i];
			}
			// 小于30度,不写字
			if (tAngle * ANGLE_NUM - sAngle * ANGLE_NUM < 30) {
				continue;
			}
			float[] position = getArcCenterPosition(startAngle + sAngle
					* ANGLE_NUM, startAngle + tAngle * ANGLE_NUM);
			String drawTxt = "";
			if (isShowDecimals()) {
				drawTxt = counts[i] + unitName;
			} else {
				drawTxt = (int) counts[i] + unitName;
			}
			mCanvas.drawText(drawTxt, position[0], position[1], paint);
		}
		paint.setTextSize(dip2px(getContext(), TEXT_SIZE));
	}

	/**
	 * 绘制饼图上的文字
	 */
	private void drawCakeText(int item) {
		paint.setColor(0xffffffff);
		float textSize;
		textSize = (cakeRect.right - cakeRect.left) * 0.03f;
		if (textSize > 18) {
			textSize = 18;
		} else if (textSize < 10) {
			textSize = 10;
		}
		paint.setTextSize(dip2px(getContext(), textSize));
		float sAngle, tAngle;
		if (item == 0) {
			sAngle = 0;
			tAngle = cakeValues.get(item).value;
		} else {
			sAngle = itemFrame[item - 1];
			tAngle = itemFrame[item];
		}
		// // 小于30度,不写字
		// if (tAngle * ANGLE_NUM - sAngle * ANGLE_NUM < 30) {
		// continue;
		// }
		float[] position = getArcCenterPosition(
				startAngle + sAngle * ANGLE_NUM, startAngle + tAngle
						* ANGLE_NUM);
		String drawTxt = "";
		if (isShowDecimals()) {
			drawTxt = counts[item] + unitName;
		} else {
			drawTxt = (int) counts[item] + unitName;
		}
		mCanvas.drawText(drawTxt, position[0], position[1], paint);
		// holder.unlockCanvasAndPost(canvas);
		paint.setTextSize(dip2px(getContext(), TEXT_SIZE));
	}

	/**
	 * 绘制文字信息
	 */
	private void drawDetail() {
		if (null == cakeValues)
			return;
		float cakeWidth = cakeRect.right - cakeRect.left;
		float cakeHeight = cakeRect.bottom - cakeRect.top;
		// 行间距
		int textLineSpace = dip2px(getContext(), TEXT_LINE_SPACE);
		// 颜色块的宽
		int colorRectWidth = dip2px(getContext(), TEXT_SIZE);
		// 左边界,上边界
		int left, top;
		if (textGravity == Gravity.right) {
			left = (int) (cakeWidth + dip2px(getContext(), LEFT_SPACING));
			top = 0;
		} else {
			left = (int) (cakeRect.left + dip2px(getContext(), LEFT_SPACING));
			top = (int) (cakeHeight + dip2px(getContext(), TOP_SPACING));
		}
		Rect colorRect = new Rect();
		detailRect = new Rect();
		detailRect.set(left, top, width, height);
		int detailWidth = detailRect.right - detailRect.left;
		// 清屏
		mCanvas.drawRect(detailRect, clearPaint);
		for (int i = 0; i < cakeValues.size(); i++) {
			int colorIndex = i % ARC_COLORS.length;
			if (i == cakeValues.size() - 1 && colorIndex == 0) {
				colorIndex = 1;
			}
			paint.setColor(ARC_COLORS[colorIndex]);
			// 如果越界(超过100,即360度),就不画了
			if (i > 0
					&& (itemFrame[i - 1] > 100 || cakeValues.get(i).value
							+ itemFrame[i - 1] > 100)) {
				break;
			}
			int textX, textY;
			int rectX, rectY;
			if (textGravity == Gravity.right) {
				rectX = left;
				rectY = top + i * textLineSpace + textLineSpace
						- dip2px(getContext(), TEXT_SIZE);
				textY = top + (i + 1) * textLineSpace;
			} else {
				int column = rankType == RankType.RANK_BY_ROW ? 2 : 1;
				rectX = left + (i % column) * (detailWidth / 2);
				rectY = top + (i / column) * textLineSpace + textLineSpace
						- dip2px(getContext(), TEXT_SIZE);
				textY = top + (i / column + 1) * textLineSpace;
			}
			textX = rectX + colorRectWidth + dip2px(getContext(), 3);
			colorRect.set(rectX, rectY, rectX + colorRectWidth, rectY
					+ colorRectWidth);
			mCanvas.drawRect(colorRect, paint);
			paint.setColor(0xff000000);
			String drawTxt = "";
			if (isShowDecimals()) {
				drawTxt = counts[i] + unitName;
			} else {
				drawTxt = (int) counts[i] + unitName;
			}
			mCanvas.drawText(cakeValues.get(i).content + ":" + drawTxt, textX,
					textY, paint);
		}
	}

	/**
	 * 绘制饼图
	 */
	private void drawCake() {
		if (null == itemFrame)
			return;
		Rect lockRect = new Rect();
		cakeRect.round(lockRect);
		for (int i = 0; i < cakeValues.size(); i++) {
			int colorIndex = i % ARC_COLORS.length;
			if (i == cakeValues.size() - 1 && colorIndex == 0) {
				colorIndex = 1;
			}
			paint.setColor(ARC_COLORS[colorIndex]);
			if (i == 0) {
				mCanvas.drawArc(cakeRect, startAngle, cakeValues.get(i).value
						* ANGLE_NUM, true, paint);
				continue;
			}
			// 如果越界(超过100,即360度),就不画了
			if (itemFrame[i - 1] >= 100
					|| cakeValues.get(i).value + itemFrame[i - 1] > 100) {
				break;
			}
			mCanvas.drawArc(cakeRect,
					startAngle + itemFrame[i - 1] * ANGLE_NUM,
					cakeValues.get(i).value * ANGLE_NUM, true, paint);
		}
		drawCakeText();
		// drawDetail();
	}

	private void drawItem(float top, int item) {

		RectF itemRectf = new RectF();
		itemRectf
				.set(cakeRect.left, top, cakeRect.right, top + cakeRect.bottom);
		Rect lockRect = new Rect();
		lockRect.set((int) cakeRect.left, 0, (int) cakeRect.right,
				40 + (int) cakeRect.bottom);
		// 清屏
		mCanvas.drawRect(lockRect, clearPaint);
		for (int i = 0; i < cakeValues.size(); i++) {
			RectF drawRect;
			int colorIndex = i % ARC_COLORS.length;
			if (i == item) {
				drawRect = itemRectf;
			} else {
				drawRect = cakeRect;
				// if (top != 0){
				// colorIndex = 6;
				// }
			}
			if (i == cakeValues.size() - 1 && colorIndex == 0) {
				colorIndex = 1;
			}
			paint.setColor(ARC_COLORS[colorIndex]);
			if (i == 0) {
				mCanvas.drawArc(drawRect, startAngle, cakeValues.get(i).value
						* ANGLE_NUM, true, paint);
				if (i != item && top != 0) {
					paint.setColor(FUZZY_COLOR);
					paint.setAlpha((int) (top * 200 / HIGHLIGHT_OFFSET));
					mCanvas.drawArc(drawRect, startAngle,
							cakeValues.get(i).value * ANGLE_NUM, true, paint);
				}
				continue;
			}
			// 如果越界(超过100,即360度),就不画了
			if (itemFrame[i - 1] >= 100
					|| cakeValues.get(i).value + itemFrame[i - 1] > 100) {
				break;
			}
			mCanvas.drawArc(drawRect,
					startAngle + itemFrame[i - 1] * ANGLE_NUM,
					cakeValues.get(i).value * ANGLE_NUM, true, paint);
			if (i != item && top != 0) {
				paint.setColor(FUZZY_COLOR);
				paint.setAlpha((int) (top * 200 / HIGHLIGHT_OFFSET));
				mCanvas.drawArc(drawRect, startAngle + itemFrame[i - 1]
						* ANGLE_NUM, cakeValues.get(i).value * ANGLE_NUM, true,
						paint);
			}
		}
		if (top != 0)
			drawCakeText(item);
		// else
		// drawCakeText();
		// if (top == 0) {
		// // isHighLigntMode=false;
		// // 清屏
		// // mCanvas.drawRect(lockRect, clearPaint);
		//
		// Log.v("ts", "高亮开始" + isHighLigntMode);
		// if (!isHighLigntMode) {
		// drawDetail();
		// }
		// } else if (top == HIGHLIGHT_OFFSET) {
		// // isHighLigntMode=true;
		// Log.v("ts", "高亮完成" + isHighLigntMode);
		// if (isHighLigntMode)
		// drawItemText(cakeValues.get(item).detail);
		// }
	}

	private void drawItemText(String txt) {
		if (null == txt)
			return;
		Rect itemRect = new Rect();
		itemRect.set(detailRect.left, detailRect.top + (int) HIGHLIGHT_OFFSET,
				detailRect.right, (int) HIGHLIGHT_OFFSET + detailRect.bottom);
		// 清屏
		mCanvas.drawRect(detailRect, clearPaint);
		TextPaint textPaint = new TextPaint(paint);
		textPaint.setColor(0xff000000);

		StaticLayout layout = new StaticLayout(txt, textPaint, itemRect.right
				- itemRect.left, Alignment.ALIGN_NORMAL, 1f, 0f, true);
		float dx = itemRect.left + dip2px(getContext(), LEFT_SPACING);
		float dy = itemRect.top;
		mCanvas.translate(dx, dy);
		layout.draw(mCanvas);
		// 移走了还要移回来
		mCanvas.translate(-dx, -dy);
	}

	/**
	 * 根据坐标计算点相对圆心的角度
	 * 
	 * @param x1
	 * @param y1
	 * @return 角度
	 */
	private float getAngleByPosition(float x1, float y1) {
		// 圆心
		float x = cakeRect.left + ((cakeRect.right - cakeRect.left) / 2f);
		float y = cakeRect.top + ((cakeRect.bottom - cakeRect.top) / 2f);
		float r = (cakeRect.right - cakeRect.left) / 2f;
		// 对边长
		float dBian = y1 - y;
		// 邻边
		float lBian = x1 - x;
		// 超出饼图范围
		if (Math.abs(dBian) > r || Math.abs(lBian) > r) {
			return -1f;
		}
		// 如果点到圆心长度超过了半径
		if (Math.sqrt(dBian * dBian + lBian * lBian) > r) {
			return -1f;
		}
		double arc = Math.atan(dBian / lBian);
		double angle;
		if (x1 < x) {
			angle = 180d + TransUtil.radians2angle(arc);
		} else {
			if (y1 > y) {
				angle = TransUtil.radians2angle(arc);
			} else {
				angle = 360d + TransUtil.radians2angle(arc);
			}
		}
		return (float) angle;
	}

	private int getClickPosition() {
		int clickPosition = -1;
		float clickAngle = getAngleByPosition(firstDownX, firstDownY);
		if (clickAngle != -1) {
			for (int i = 0; i < itemFrame.length; i++) {
				if (i == 0) {
					float aAngle = startAngle;
					float bAngle = startAngle + itemFrame[i] * ANGLE_NUM;
					aAngle = getRecoverStartAngle(aAngle);
					bAngle = getRecoverStartAngle(bAngle);
					if (aAngle <= bAngle) {
						if (clickAngle >= aAngle && clickAngle <= bAngle) {
							clickPosition = i;
							break;
						}
					} else {
						if ((clickAngle >= aAngle && clickAngle < 360)
								|| (clickAngle >= 0 && clickAngle <= bAngle)) {
							clickPosition = i;
							break;
						}
					}
					continue;
				}
				float aAngle = startAngle + itemFrame[i - 1] * ANGLE_NUM;
				float bAngle = startAngle + itemFrame[i] * ANGLE_NUM;
				aAngle = getRecoverStartAngle(aAngle);
				bAngle = getRecoverStartAngle(bAngle);
				if (aAngle <= bAngle) {
					if (clickAngle >= aAngle && clickAngle <= bAngle) {
						clickPosition = i;
						break;
					}
				} else {
					if ((clickAngle >= aAngle && clickAngle < 360)
							|| (clickAngle >= 0 && clickAngle <= bAngle)) {
						clickPosition = i;
						break;
					}
				}
			}
		}
		return clickPosition;
	}

	/**
	 * 根据给定原角度计算改变startAngle后的当前角度 即计算startAngle为0时,此角度的值
	 * 
	 * @return
	 */
	private float getRecoverStartAngle(float angle) {
		// 先还原为360度以内
		float res = angle == 360 ? 360 : angle % 360;
		return res < 0 ? 360 + res : res;
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	}

	private void initValueAnimator() {

		PropertyValuesHolder angleValues = PropertyValuesHolder.ofFloat(
				"angle", 0f, 360f);
		cakeValueAnimator = ValueAnimator.ofPropertyValuesHolder(angleValues);
		cakeValueAnimator.addUpdateListener(new AnimatorUpdateListener() {

			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				float mAngle = obj2Float(animation.getAnimatedValue("angle"));
				curAngle = mAngle;
				drawArc();
				postInvalidate();
			}
		});
		cakeValueAnimator.setDuration(DURATION);
		cakeValueAnimator.setRepeatCount(0);
		cakeValueAnimator.setInterpolator(new DecelerateInterpolator());
		cakeValueAnimator.setRepeatMode(ValueAnimator.RESTART);

		rotaValues = PropertyValuesHolder.ofFloat("rotation", 0f, 90f);
		rotaValueAnimator = ValueAnimator.ofPropertyValuesHolder(rotaValues);
		rotaValueAnimator.addUpdateListener(new AnimatorUpdateListener() {

			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				float rota = obj2Float(animation.getAnimatedValue("rotation"));
				startAngle = rota;
				drawCake();
				if (animation.getAnimatedFraction() == 1) {
					// Log.v("ts", "模式" + isHighLigntMode);
					highLightValueAnimator.start();
				}
				postInvalidate();
			}
		});
		rotaValueAnimator.setRepeatCount(0);
		rotaValueAnimator.setRepeatMode(ValueAnimator.RESTART);
		rotaValueAnimator.setInterpolator(new OvershootInterpolator());

		PropertyValuesHolder topValuesHolder = PropertyValuesHolder.ofFloat(
				"top", 0, HIGHLIGHT_OFFSET);
		highLightValueAnimator = ValueAnimator
				.ofPropertyValuesHolder(topValuesHolder);
		highLightValueAnimator.addUpdateListener(new AnimatorUpdateListener() {

			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				float top = obj2Float(animation.getAnimatedValue("top"));
				// Log.v("ts", "高亮" + top);
				drawItem(top, curClickItem);
				postInvalidate();
			}
		});
		highLightValueAnimator.setDuration(700);
		highLightValueAnimator.setRepeatCount(0);
		highLightValueAnimator.setRepeatMode(ValueAnimator.REVERSE);

		highLightValueAnimator.addListener(new AnimatorListener() {

			@Override
			public void onAnimationStart(Animator animation) {
			}

			@Override
			public void onAnimationRepeat(Animator animation) {

			}

			@Override
			public void onAnimationEnd(Animator animation) {
				if (!isHighLigntMode) {
					// 返回正常模式,重绘饼图信息
					drawDetail();
					drawCakeText();
				} else {
					drawItemText(cakeValues.get(curClickItem).detail);
				}
			}

			@Override
			public void onAnimationCancel(Animator animation) {

			}
		});
	}

	public void startAnim() {
		// mSCBitmap=getSCBitmap();
		// mCanvas.setBitmap(mSCBitmap);
		if (drawCount == 0
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
				&& isDrawByAnim) {
			if (null != cakeValueAnimator) {
				cakeValueAnimator.start();
				drawCount = 1;
			}
		}
	}

	private float obj2Float(Object o) {
		return ((Number) o).floatValue();
	}

	/**
	 * 获得当前绘制的饼.
	 * 
	 * @param curAngle
	 * @return
	 */
	private int getCurItem(float curAngle) {
		int res = 0;
		for (int i = 0; i < itemFrame.length; i++) {
			if (curAngle <= itemFrame[i] * ANGLE_NUM) {
				res = i;
				break;
			}
		}
		return res;
	}

	/**
	 * 将dip或dp值转换为px值，保证尺寸大小不变
	 * 
	 * @param dipValue
	 * @param scale
	 *            （DisplayMetrics类中属性density）
	 * @return
	 */
	private static int dip2px(Context context, float dipValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dipValue * scale + 0.5f);
	}

	/**
	 * 根据弧度得到饼图每一项的中心点
	 * 
	 * @param sAngle
	 * @param tAngle
	 */
	private float[] getArcCenterPosition(float sAngle, float tAngle) {
		// 计算中心角度
		// 如:一个扇形,起始角度为10,结束角度为40,则arc=10+(40-10)/2
		float arc = sAngle + (tAngle - sAngle) / 2;
		float x = (cakeRect.right - cakeRect.left) / 2f;
		float y = (cakeRect.bottom - cakeRect.top) / 2f;
		float angle = arc / 180f;
		double x1, y1;
		x1 = x + x / 2f * Math.cos(angle * Math.PI) + cakeRect.left;
		y1 = y + y / 2f * Math.sin(angle * Math.PI);
		x1 = x1 - dip2px(getContext(), 10);
		float[] res = new float[2];
		res[0] = (float) x1;
		res[1] = (float) y1;
		return res;
	}

	public interface OnItemClickListener {
		public void onItemClick(int position);
	}

	public static class CakeValue {
		/** 名称 */
		String content;
		/** 所占百分比 */
		float value;
		/** 详细描述 */
		String detail;

		/**
		 * 
		 * @param content
		 *            名称
		 * @param value
		 *            所占百分比
		 */
		public CakeValue(String content, float value) {
			this.content = content;
			this.value = value;
		}

		public CakeValue(String content, float value, String detail) {
			this.content = content;
			this.value = value;
			this.detail = detail;
		}
	}
}
