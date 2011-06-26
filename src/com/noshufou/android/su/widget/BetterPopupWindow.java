package com.noshufou.android.su.widget;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow;

import com.noshufou.android.su.R;

/**
 * This class does most of the work of wrapping the {@link PopupWindow} so it's simpler to use.
 * 
 * @author qberticus
 * 
 */
public class BetterPopupWindow {
    protected final View anchor;
    private final PopupWindow window;
    private View root;
    private Drawable background = null;
    private final WindowManager windowManager;

    /**
     * Create a BetterPopupWindow
     * 
     * @param anchor
     *            the view that the BetterPopupWindow will be displaying 'from'
     */
    public BetterPopupWindow(View anchor) {
        this.anchor = anchor;
        this.window = new PopupWindow(anchor.getContext());

        // when a touch even happens outside of the window
        // make the window go away
        this.window.setTouchInterceptor(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    BetterPopupWindow.this.window.dismiss();
                    return true;
                }
                return false;
            }
        });

        this.windowManager = (WindowManager) this.anchor.getContext()
                .getSystemService(Context.WINDOW_SERVICE);
        onCreate();
    }

    /**
     * Anything you want to have happen when created. Probably should create a
     * view and setup the event listeners on child views.
     */
    protected void onCreate() {}

    /**
     * In case there is stuff to do right before displaying.
     */
    protected void onShow() {}

    private void preShow() {
        if(this.root == null) {
            throw new IllegalStateException("setContentView was not called with a view to display.");
        }
        onShow();

        if(this.background == null) {
            this.window.setBackgroundDrawable(new BitmapDrawable());
        } else {
            this.window.setBackgroundDrawable(this.background);
        }

        // if using PopupWindow#setBackgroundDrawable this is the only values of the width and hight that make it work
        // otherwise you need to set the background of the root viewgroup
        // and set the popupwindow background to an empty BitmapDrawable
        this.window.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        this.window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        this.window.setTouchable(true);
        this.window.setFocusable(true);
        this.window.setOutsideTouchable(true);

        this.window.setContentView(this.root);
    }

    public void setBackgroundDrawable(Drawable background) {
        this.background = background;
    }

    /**
     * Sets the content view. Probably should be called from {@link onCreate}
     * 
     * @param root
     *            the view the popup will display
     */
    public void setContentView(View root) {
        this.root = root;
        this.window.setContentView(root);
    }

    /**
     * Will inflate and set the view from a resource id
     * 
     * @param layoutResID
     */
    public void setContentView(int layoutResID) {
        LayoutInflater inflator =
            (LayoutInflater) this.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.setContentView(inflator.inflate(layoutResID, null));
    }

    /**
     * If you want to do anything when {@link dismiss} is called
     * 
     * @param listener
     */
    public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
        this.window.setOnDismissListener(listener);
    }

    public void show() {
        this.preShow();
        
        this.window.setAnimationStyle(R.style.Animations_GrowFromTop);

        int[] location = new int[2];
        this.anchor.getLocationOnScreen(location);

        Rect anchorRect =
            new Rect(location[0], location[1],
                    location[0] + this.anchor.getWidth(),
                    location[1] + this.anchor.getHeight());

        this.root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        int rootWidth = this.root.getMeasuredWidth();
        int rootHeight = this.root.getMeasuredHeight();

        int screenWidth = this.windowManager.getDefaultDisplay().getWidth();
        int screenHeight = this.windowManager.getDefaultDisplay().getHeight();

        int xPos = anchorRect.left;
        int yPos = anchorRect.bottom;
        
        // Display above the anchor view
        if (yPos + rootHeight > screenHeight) {
            this.window.setAnimationStyle(R.style.Animations_GrowFromBottom);
            yPos = anchorRect.top - rootHeight;
        }
        
        // Keep the right edge of the popup on the screen
        if (xPos + rootWidth > screenWidth) {
            xPos = anchorRect.left - ((anchorRect.left + rootWidth) - screenWidth);
        }

        this.window.showAtLocation(this.anchor, Gravity.NO_GRAVITY, xPos, yPos);
    }

    public void dismiss() {
        this.window.dismiss();
    }
}