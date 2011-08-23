package com.noshufou.android.su.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.noshufou.android.su.R;

public class PagerHeader extends ViewGroup {
    private static final String TAG = "Su.PagerHeader";
    
    private final int HEIGHT;
    private final int PADDING_TOP;
    private final int PADDING_BOTTOM;
    private final int PADDING_PUSH;
    private final int TAB_HEIGHT;
    private final int TAB_PADDING;
    private final int FADING_EDGE_LENGTH;
    private final float TEXT_SIZE = 16;
    
    private int mTextTop = 0;
    private int mTextHeight = 0;
    
    private Context mContext;
    private int mDisplayedPage = 0;
    
    private int mLastMotionAction;
    private float mLastMotionX;
    
    private Drawable mTabDrawable;
    private Drawable mFadingEdgeLeft;
    private Drawable mFadingEdgeRight;
    
    private OnHeaderChangeListener mOnHeaderChangeListener = null;
    
    public interface OnHeaderChangeListener {
        public void onHeaderSelected(int position);
    }
    
    public PagerHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        
        Resources res = context.getResources();
        HEIGHT = res.getDimensionPixelOffset(R.dimen.pager_header_height);
        PADDING_TOP = res.getDimensionPixelSize(R.dimen.pager_header_padding_top);
        PADDING_BOTTOM = res.getDimensionPixelSize(R.dimen.pager_header_padding_bottom);
        PADDING_PUSH = res.getDimensionPixelSize(R.dimen.pager_header_padding_push);
        TAB_HEIGHT = res.getDimensionPixelSize(R.dimen.pager_header_tab_height);
        TAB_PADDING = res.getDimensionPixelSize(R.dimen.pager_header_tab_padding);
        FADING_EDGE_LENGTH = res.getDimensionPixelSize(R.dimen.pager_header_fading_edge_length);
        
        mTabDrawable = res.getDrawable(R.drawable.pager_header_tab);
        mFadingEdgeLeft = res.getDrawable(R.drawable.pager_header_fading_edge_left);
        mFadingEdgeRight = res.getDrawable(R.drawable.pager_header_fading_edge_right);
    }
    
    public void add(int index, String label) {
        TextView textView = new TextView(mContext);
        textView.setText(label);
        textView.setTextColor(Color.BLACK);
//        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        textView.setTextSize(TEXT_SIZE);
        addView(textView);
    }
    
    public void setOnHeaderChangeListener(OnHeaderChangeListener listener) {
        mOnHeaderChangeListener = listener;
    }
    
    public void setDisplayedPage(int index) {
        mDisplayedPage = index;
    }
    
    public void setPosition(int position, float positionOffset, int positionOffsetPixels) {
        int width = getWidth();
        int center = width / 2;
        
        // Move the view at position. This will be the label for the left
        // of the two fragments that may be on the screen
        if (position >= 0 && position < getChildCount()) {
            TextView view = (TextView) getChildAt(position);
            int viewWidth = view.getWidth();
            int range = center - (viewWidth / 2);
            int newLeft = (int) (range - (range * positionOffset));
            view.layout(newLeft, view.getTop(), newLeft + viewWidth, view.getBottom());
            view.setTextColor(Color.rgb(
                    Math.max(0, (int) ((-328 * (float) positionOffset) + 164)),
                    Math.max(0, (int) ((-396 * (float) positionOffset) + 198)),
                    Math.max(0, (int) ((-114 * (float) positionOffset) + 57))));
        }
        
        // Move the view at position + 1. This will be the label for the
        // right of the two fragments that may be visible on screen
        if ((position + 1) < getChildCount()) {
            TextView view = (TextView) getChildAt(position + 1);
            int viewWidth = view.getWidth();
            int newLeft = view.getLeft();
            int range = (width - viewWidth) - (center - (viewWidth / 2));
            newLeft = (int) (width - viewWidth - (range * positionOffset));
            view.layout(newLeft, view.getTop(), newLeft + viewWidth, view.getBottom());
            view.setTextColor(Color.rgb(
                    Math.max(0, (int) ((328 * positionOffset) - 164)),
                    Math.max(0, (int) ((396 * positionOffset) - 198)),
                    Math.max(0, (int) ((114 * positionOffset) - 57))));
        }
        
        // Move the view at position - 1. This will be the label for the 
        // fragment that is off the screen to the left, if it exists
        if (position > 0) {
            TextView view = (TextView) getChildAt(position - 1);
            int plusOneLeft = getChildAt(position).getLeft();
            int newLeft = view.getLeft();
            int viewWidth = view.getWidth();
            if (plusOneLeft < newLeft + viewWidth + PADDING_PUSH || newLeft < 0) {
                newLeft = Math.min(0, plusOneLeft - viewWidth - PADDING_PUSH);
                view.layout(newLeft, view.getTop(), newLeft + viewWidth, view.getBottom());
                int alpha = (int) (255 * ((float) view.getRight() / (float) viewWidth));
                view.setTextColor(Color.argb(alpha, 0, 0, 0));
            }
        }
        
        // Move the view at position + 2. This will be the label for the
        // fragment that is off the screen to the right, if it exists
        if ((position + 2) < getChildCount()) {
            TextView view = (TextView) getChildAt(position + 2);
            int minusOneRight = getChildAt(position + 1).getRight();
            int newLeft = view.getLeft();
            int viewWidth = view.getWidth();
            if (minusOneRight > (newLeft - PADDING_PUSH) || newLeft + viewWidth > width) {
                newLeft = Math.max(minusOneRight + PADDING_PUSH, width - viewWidth);
                view.layout(newLeft, view.getTop(), newLeft + viewWidth, view.getBottom());
                int alpha = (int) (255 * ((float) (width - newLeft) / (float) viewWidth));
                view.setTextColor(Color.argb(alpha, 0, 0, 0));
            }
        }
        
        // Draw the tab under the active or oncoming TextView based the
        // positionOffset
        View view = getChildAt(positionOffset < 0.5f?position:position + 1);
        int viewLeft = view.getLeft();
        int viewRight = view.getRight();
        float percent = (float) (Math.abs(positionOffset - 0.5f)/0.5f);
        int tabHeight =  (int) (TAB_HEIGHT * percent);
        int alpha = (int) (255 * percent); 
        mTabDrawable.setBounds(viewLeft - TAB_PADDING,
                getHeight() - tabHeight,
                viewRight + TAB_PADDING,
                getHeight());
        mTabDrawable.setAlpha(alpha);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        for (int i = 0; i < getChildCount(); i++ ) {
            View view = getChildAt(i);
            view.measure(0, 0);
            mTextHeight = Math.max(mTextHeight, view.getMeasuredHeight());
        }
        
        int width = resolveSize(0, widthMeasureSpec);
        
        int textHeight = getChildAt(0).getMeasuredHeight();
        mTextTop = PADDING_TOP;
        int height = Math.max(HEIGHT, textHeight + PADDING_TOP + PADDING_BOTTOM);
        
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = 0;
        int right = r - l;
        int center = (r - l) / 2;
        
        for (int i = 0; i < getChildCount(); i++) {
            TextView view = (TextView) getChildAt(i);
            int viewWidth = view.getMeasuredWidth();
            int viewHeight = view.getMeasuredHeight();
            int viewLeft = 0;
            if (i == mDisplayedPage - 1) {
                viewLeft = 0;
            } else if (i == mDisplayedPage) {
                viewLeft = center - (viewWidth / 2);
                view.setTextColor(0xffa4c639);
                mTabDrawable.setBounds(viewLeft - TAB_PADDING,
                        b - t - TAB_HEIGHT,
                        viewLeft + viewWidth + TAB_PADDING,
                        b - t);
            } else if (i == mDisplayedPage + 1) {
                viewLeft = right - viewWidth;
            } else if (i < (mDisplayedPage - 1)) {
                viewLeft = left - viewWidth - 5;
            } else if (i > (mDisplayedPage + 1)) {
                viewLeft = right + 5;
            }
            view.layout(viewLeft,
                    mTextTop,
                    viewLeft + viewWidth,
                    mTextTop + viewHeight);
        }
        
        // Set up the fading edges
        mFadingEdgeLeft.setBounds(0, 
                mTextTop,
                FADING_EDGE_LENGTH,
                mTextTop + mTextHeight);
        mFadingEdgeRight.setBounds(right - FADING_EDGE_LENGTH,
                mTextTop,
                right,
                mTextTop + mTextHeight);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        mTabDrawable.draw(canvas);
        mFadingEdgeLeft.draw(canvas);
        mFadingEdgeRight.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            
        }
        
        if (action == MotionEvent.ACTION_UP
                && mLastMotionAction == MotionEvent.ACTION_DOWN
                && event.getX() == mLastMotionX) {
            int width = getWidth();
            int pageSize = width / 4;
            if ((int)mLastMotionX < pageSize
                    && mDisplayedPage > 0) {
                setDisplayedPage(mDisplayedPage - 1);
                if (mOnHeaderChangeListener != null) {
                    mOnHeaderChangeListener.onHeaderSelected(mDisplayedPage);
                }
            } else if ((int)mLastMotionX > width - pageSize
                    && mDisplayedPage < getChildCount() - 1) {
                setDisplayedPage(mDisplayedPage + 1);
                if (mOnHeaderChangeListener != null) {
                    mOnHeaderChangeListener.onHeaderSelected(mDisplayedPage);
                }
            }
        }
        
        mLastMotionAction = event.getAction();
        mLastMotionX = event.getX();
        return true;
    }
}
