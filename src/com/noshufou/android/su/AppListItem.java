package com.noshufou.android.su;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

public class AppListItem extends ViewGroup {
	
	private final Context mContext;
	
	private final int mPreferredHeight;
	private final int mPaddingTop;
	private final int mPaddingRight;
	private final int mPaddingBottom;
	private final int mPaddingLeft;
	private final int mGapBetweenImageAndText;
	private final int mStatusButtonPadding;
	private final int mHeaderPaddingLeft;
	
	private boolean mHorizontalDividerVisible;
	private Drawable mHorizontalDividerDrawable;
	private int mHorizontalDividerHeight;
	
	private boolean mHeaderVisible;
	private Drawable mHeaderBackgroundDrawable;
	private int mHeaderBackgroundHeight;
	private TextView mHeaderTextView;
	
	private ImageView mIconView;
	private TextView mNameTextView;
	private TextView mLogTextView;
	private ImageView mStatusButton;
	
	private int mIconViewSize;
	private int mLine1Height;
	private int mLine2Height;
	
	private OnClickListener mStatusButtonClickListener;
	
	public AppListItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		
		Resources resources = context.getResources();
		mPreferredHeight =
				resources.getDimensionPixelOffset(R.dimen.list_item_perferred_height);
		mPaddingTop =
				resources.getDimensionPixelOffset(R.dimen.list_item_padding_top);
		mPaddingBottom =
				resources.getDimensionPixelOffset(R.dimen.list_item_padding_bottom);
		mPaddingLeft =
				resources.getDimensionPixelOffset(R.dimen.list_item_padding_left);
		mPaddingRight =
				resources.getDimensionPixelOffset(R.dimen.list_item_padding_right);
		mGapBetweenImageAndText =
				resources.getDimensionPixelOffset(R.dimen.list_item_gap_between_image_and_text);
		mStatusButtonPadding =
				resources.getDimensionPixelOffset(R.dimen.list_item_status_button_padding);
		mHeaderPaddingLeft =
				resources.getDimensionPixelOffset(R.dimen.list_item_header_padding_left);
	}

	/**
	 * Install status button click listener
	 */
	public void setOnStatusButtonClickListener(OnClickListener statusButtonClickListener) {
		mStatusButtonClickListener = statusButtonClickListener;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = resolveSize(0, widthMeasureSpec);
		int height = 0;
		
		mLine1Height = 0;
		mLine2Height = 0;
		
		mNameTextView.measure(0, 0);
		mLine1Height = mNameTextView.getMeasuredHeight();
		
		if (isVisible(mLogTextView)) {
			mLogTextView.measure(0, 0);
			mLine2Height = mLogTextView.getMeasuredHeight();
		}
		
		height += mLine1Height + mLine2Height;
		
		if (isVisible(mStatusButton)) {
			mStatusButton.measure(0, 0);
		}
		
		height = Math.max(height, mPreferredHeight);
		
		mIconViewSize = height - mPaddingTop - mPaddingBottom; 
			
		if (mHeaderVisible) {
			ensureHeaderBackground();
			mHeaderTextView.measure(
					MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(mHeaderBackgroundHeight, MeasureSpec.EXACTLY));
			height += mHeaderBackgroundDrawable.getIntrinsicHeight();
		}
		
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		int height = bottom - top;
		int width = right - left;
		
		int topBound = 0;
		
		if (mHeaderVisible) {
			mHeaderBackgroundDrawable.setBounds(
					0,
					0,
					width,
					mHeaderBackgroundHeight);
			mHeaderTextView.layout(mHeaderPaddingLeft, 0, width, mHeaderBackgroundHeight);
			topBound += mHeaderBackgroundHeight;
		}
		
		int leftBound = mPaddingLeft;
		if (mIconView != null) {
			int iconTop = topBound + (height - topBound - mIconViewSize) / 2;
			mIconView.layout(
					leftBound,
					iconTop,
					leftBound + mIconViewSize,
					iconTop + mIconViewSize);
			leftBound += mIconViewSize + mGapBetweenImageAndText;
		}
		
		int rightBound = right;
		if (isVisible(mStatusButton)) {
			int buttonWidth = mStatusButton.getMeasuredWidth();
			rightBound -= buttonWidth;
			mStatusButton.layout(
					rightBound,
					topBound,
					rightBound + buttonWidth,
					height);
		}
		
		if (mHorizontalDividerVisible) {
			ensureHorizontalDivider();
			mHorizontalDividerDrawable.setBounds(
					0,
					height - mHorizontalDividerHeight,
					width,
					height);
		}
		
		topBound += mPaddingTop;
		int bottomBound = height - mPaddingBottom;
		rightBound -= mPaddingRight;
		
		int totalTextHeight = mLine1Height + mLine2Height;
		int textTopBound = (bottomBound + topBound - totalTextHeight) / 2;
		
		mNameTextView.layout(
				leftBound,
				textTopBound,
				rightBound,
				textTopBound + mLine1Height);
		
		if (isVisible(mLogTextView)) {
			mLogTextView.layout(
					leftBound,
					textTopBound + mLine1Height,
					rightBound,
					textTopBound + mLine1Height + mLine2Height);
		}
	}

	private boolean isVisible(View view) {
		return view != null && view.getVisibility() == View.VISIBLE;
	}
	
	/**
	 * Loads the drawable for the horizontal divider if it has not yet been loaded.
	 */
	private void ensureHorizontalDivider() {
		if (mHorizontalDividerDrawable == null) {
			mHorizontalDividerDrawable = mContext.getResources().getDrawable(
					R.drawable.divider_horizontal_dark_opaque);
			mHorizontalDividerHeight = mHorizontalDividerDrawable.getIntrinsicHeight();
		}
	}
	
	/**
	 * Loads the drawable for the header background if it has not yet been loaded.
	 */
	private void ensureHeaderBackground() {
		if (mHeaderBackgroundDrawable == null) {
			mHeaderBackgroundDrawable = mContext.getResources().getDrawable(
					R.drawable.dark_header);
			mHeaderBackgroundHeight = mHeaderBackgroundDrawable.getIntrinsicHeight();
		}
	}
	
	@Override
	public void dispatchDraw(Canvas canvas) {
		if (mHeaderVisible) {
			mHeaderBackgroundDrawable.draw(canvas);
		}
		if (mHorizontalDividerVisible) {
			mHorizontalDividerDrawable.draw(canvas);
		}
		super.dispatchDraw(canvas);
	}
	
	/**
	 * Sets the flag that determines whether a divider should be drawn at the bottom
	 * of the view.
	 */
	public void setDividerVisible(boolean visible) {
		mHorizontalDividerVisible = visible;
	}
	
	/**
	 * Sets section header or makes it invisible if the title is null.
	 */
	public void setSectionHeader(String title) {
		if (!TextUtils.isEmpty(title)) {
			if (mHeaderTextView == null) {
				mHeaderTextView = new TextView(mContext);
				mHeaderTextView.setTypeface(mHeaderTextView.getTypeface(), Typeface.BOLD);
				mHeaderTextView.setTextColor(mContext.getResources()
						.getColor(R.color.dim_foreground_dark));
				mHeaderTextView.setTextSize(14);
				mHeaderTextView.setGravity(Gravity.LEFT);
				addView(mHeaderTextView);
			}
			mHeaderTextView.setText(title);
			mHeaderTextView.setVisibility(View.VISIBLE);
			mHeaderVisible = true;
		} else {
			if (mHeaderTextView != null) {
				mHeaderTextView.setVisibility(View.GONE);
			}
			mHeaderVisible = false;
		}
	}
	
	/**
	 * Returns the text view for the app name, creating it if necessary.
	 */
	public void setNameText(CharSequence text) {
		if (!TextUtils.isEmpty(text)) {
			if (mNameTextView == null) {
				mNameTextView = new TextView(mContext);
				mNameTextView.setSingleLine(true);
				mNameTextView.setEllipsize(TruncateAt.END);
				mNameTextView.setTextAppearance(mContext, android.R.style.TextAppearance_Large);
				mNameTextView.setGravity(Gravity.CENTER_VERTICAL);
				addView(mNameTextView);
			}
			mNameTextView.setText(text);
			mNameTextView.setVisibility(View.VISIBLE);
		} else {
			if (mNameTextView != null) {
				mNameTextView.setVisibility(View.GONE);
			}
		}
	}
	
	/**
	 * Adds or updates a text view for log information.
	 */
	public void setLogText(CharSequence text) {
		if (!TextUtils.isEmpty(text)) {
			if (mLogTextView == null) {
				mLogTextView = new TextView(mContext);
				mLogTextView.setSingleLine(true);
				mLogTextView.setEllipsize(TruncateAt.END);
				mLogTextView.setTextAppearance(mContext, android.R.style.TextAppearance_Small);
				addView(mLogTextView);
			}
			mLogTextView.setText(text);
			mLogTextView.setVisibility(View.VISIBLE);
		} else {
			if (mLogTextView != null) {
				mLogTextView.setVisibility(View.GONE);
			}
		}
	}
	
	/**
	 * Sets the app icon, creating it if necessary.
	 */
	public void setAppIcon(Drawable icon) {
		if (icon != null) {
			if (mIconView == null) {
				mIconView = new ImageView(mContext);
				addView(mIconView);
			}
			mIconView.setImageDrawable(icon);
			mIconView.setVisibility(View.VISIBLE);
		} else {
			if (mIconView != null) {
				mIconView.setVisibility(View.GONE);
			}
		}
	}
	
	/**
	 * Sets up the status icon, creating it if necessary.
	 */
	public void setStatusButton(Drawable icon, int id, long appId) {
		if (icon != null) {
			if (mStatusButton == null) {
				mStatusButton = new ImageView(mContext);
				mStatusButton.setId(id);
				mStatusButton.setOnClickListener(mStatusButtonClickListener);
				mStatusButton.setPadding(mStatusButtonPadding, 0, mStatusButtonPadding, 0);
				mStatusButton.setScaleType(ScaleType.CENTER);
				addView(mStatusButton);
			}
			mStatusButton.setImageDrawable(icon);
			mStatusButton.setTag(appId);
			mStatusButton.setVisibility(View.VISIBLE);
		} else {
			if (mStatusButton != null) {
				mStatusButton.setVisibility(View.GONE);
			}
		}
	}
}
