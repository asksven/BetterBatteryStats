/*******************************************************************************
 * Copyright (c) 2011 Adam Shanks (ChainsDD)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.noshufou.android.su.widget;

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
import android.widget.TextView;

import com.noshufou.android.su.R;

public class LogListItem extends ViewGroup {

    private final Context mContext;

    private final int mPerferredHeight;
    private final int mPaddingTop;
    private final int mPaddingRight;
    private final int mPaddingBottom;
    private final int mPaddingLeft;
    private final int mGapBetweenFields;
    private final int mHeaderPaddingLeft;

    private boolean mHorizontalDividerVisible;
    private Drawable mHorizontalDividerDrawable;
    private int mHorizontalDividerHeight;

    private boolean mHeaderVisible;
    private Drawable mHeaderBackgroundDrawable;
    private int mHeaderBackgroundHeight;
    private TextView mHeaderTextView;
    
    private TextView mTimeTextView;
    private TextView mNameTextView;
    private TextView mTypeTextView;
    
    private int mLineHeight;

    public LogListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        
        Resources resources = context.getResources();
        mPerferredHeight =
                resources.getDimensionPixelOffset(R.dimen.log_item_perferred_height);
        mPaddingTop =
                resources.getDimensionPixelOffset(R.dimen.log_item_padding_top);
        mPaddingBottom =
                resources.getDimensionPixelOffset(R.dimen.log_item_padding_bottom);
        mPaddingLeft =
                resources.getDimensionPixelOffset(R.dimen.log_item_padding_left);
        mPaddingRight =
                resources.getDimensionPixelOffset(R.dimen.log_item_padding_right);
        mGapBetweenFields =
                resources.getDimensionPixelOffset(R.dimen.log_item_gap_between_fields);
        mHeaderPaddingLeft =
                resources.getDimensionPixelOffset(R.dimen.log_item_header_padding_left);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = resolveSize(0, widthMeasureSpec);
        int height = 0;
        
        mLineHeight = 0;
        
        mTimeTextView.measure(0, 0);
        mLineHeight = mTimeTextView.getMeasuredHeight();
        
        if (mNameTextView != null) {
            mNameTextView.measure(0, 0);
            mLineHeight = Math.max(mLineHeight, mNameTextView.getMeasuredHeight());
        }
        
        mTypeTextView.measure(0, 0);
        mLineHeight = Math.max(mLineHeight, mTypeTextView.getMeasuredHeight());
        
        height = mLineHeight + mPaddingTop + mPaddingBottom;

        height = Math.max(height, mPerferredHeight);

        if (mHeaderVisible) {
            ensureHeaderBackground();
            mHeaderTextView.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(mHeaderBackgroundHeight, MeasureSpec.EXACTLY));
            height += 1/*mHeaderBackgroundDrawable.getIntrinsicHeight*/;
        }
        
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int height = bottom - top;
        int width = right - left;
        
        int topBound = 0;

        if (mHorizontalDividerVisible) {
            ensureHorizontalDivider();
            mHorizontalDividerDrawable.setBounds(
                    mPaddingLeft,
                    height - mHorizontalDividerHeight,
                    width,
                    height);
        }
        
        topBound += mPaddingTop;
        int bottomBound = topBound + mLineHeight;
        
        if (mHeaderVisible) {
            mHeaderBackgroundDrawable.setBounds(
                    0,
                    0,
                    width,
                    height);
            mHeaderTextView.layout(mHeaderPaddingLeft, topBound, width, bottomBound);
//            topBound += mHeaderBackgroundHeight;
        }

        
        int leftBound = left + mPaddingLeft;
        int timeRightBound = leftBound + mTimeTextView.getMeasuredWidth();
        mTimeTextView.layout(
                leftBound,
                topBound,
                timeRightBound,
                bottomBound);
        
        leftBound = timeRightBound + mGapBetweenFields;
        int rightBound = right - mPaddingRight;
        int typeLeftBound = rightBound - mTypeTextView.getMeasuredWidth();
        if (mNameTextView != null && mNameTextView.getVisibility() == View.VISIBLE) {
            mNameTextView.layout(
                    leftBound,
                    topBound,
                    typeLeftBound,
                    bottomBound);
        }
        
        mTypeTextView.layout(
                typeLeftBound,
                topBound,
                rightBound,
                bottomBound);
    }

    /**
     * Loads the drawable for the horizontal divider if it has not yet been loaded.
     */
    private void ensureHorizontalDivider() {
        if (mHorizontalDividerDrawable == null) {
            mHorizontalDividerDrawable = mContext.getResources().getDrawable(
                    R.drawable.divider_horizontal_bright_opaque);
            mHorizontalDividerHeight = mHorizontalDividerDrawable.getIntrinsicHeight();
        }
    }
    
    /**
     * Loads the drawable for the header background if it has not yet been loaded.
     */
    private void ensureHeaderBackground() {
        if (mHeaderBackgroundDrawable == null) {
            mHeaderBackgroundDrawable = mContext.getResources().getDrawable(
                    R.drawable.list_header_top);
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
//                mHeaderTextView.setTypeface(mHeaderTextView.getTypeface(), Typeface.BOLD);
                mHeaderTextView.setBackgroundColor(0xffffff);
                mHeaderTextView.setTextColor(mContext.getResources()
                        .getColor(R.color.dim_foreground_light));
                mHeaderTextView.setTextSize(14);
                mHeaderTextView.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
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
    
    public void setTimeText(CharSequence text) {
        if (mTimeTextView == null) {
            mTimeTextView = new TextView(mContext);
            mTimeTextView.setSingleLine(true);
            mTimeTextView.setEllipsize(TruncateAt.END);
            mTimeTextView.setTextAppearance(mContext, android.R.style.TextAppearance_Small);
            addView(mTimeTextView);
        }
        mTimeTextView.setText(text);
        mTimeTextView.setVisibility(View.VISIBLE);
    }
    
    public void setNameText(CharSequence text) {
        if (mNameTextView == null) {
            mNameTextView = new TextView(mContext);
            mNameTextView.setSingleLine(true);
            mNameTextView.setEllipsize(TruncateAt.END);
            mNameTextView.setTextAppearance(mContext, android.R.style.TextAppearance_Small);
            mNameTextView.setTypeface(mNameTextView.getTypeface(), Typeface.BOLD);
            addView(mNameTextView);
        }
        if (text == null) {
            mNameTextView.setVisibility(View.GONE);
        } else {
            mNameTextView.setText(text);
            mNameTextView.setVisibility(View.VISIBLE);
        }
    }
    
    public void setTypeText(CharSequence text) {
        if (mTypeTextView == null) {
            mTypeTextView = new TextView(mContext);
            mTypeTextView.setSingleLine(true);
            mTypeTextView.setEllipsize(TruncateAt.END);
            mTypeTextView.setTextAppearance(mContext, android.R.style.TextAppearance_Small);
            addView(mTypeTextView);
        }
        mTypeTextView.setText(text);
        mTypeTextView.setVisibility(View.VISIBLE);
    }
}
