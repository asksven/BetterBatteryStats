package com.noshufou.android.su.widget;

import com.noshufou.android.su.R;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PinView extends LinearLayout {
    private static final String TAG = "PinView";
    
    public static final int REASON_CANCELED = 1;
    public static final int REASON_MISMATCH = 2;
    public static final int REASON_WRONG = 3;
    
    private static final int ROWS = 4;
    private static final int COLUMNS = 3;
    private static final int NUM_BUTTONS = ROWS * COLUMNS;
    
    private boolean mReqConfirm = false;
    private CharSequence mPinConfirm = null;
    
    private PinCallbacks mCallbacks;
    
    private TextView mPinText;
    private Button[] mButtons = new Button[NUM_BUTTONS];

    public PinView(Context context) {
        this(context, null);
    }

    public PinView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PinView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        ((Activity)context).getLayoutInflater()
            .inflate(R.layout.pin_layout, this, true);
        
        mPinText = (TextView) findViewById(R.id.pin);
        
        for (int i = 0; i < NUM_BUTTONS; i++) {
            // Use i + 1 to find the buttons becuase the text view
            // is before the buttons in the layout
            mButtons[i] = (Button) getChildAt(i + 1);
//            mButtons[i].measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            mButtons[i].setOnClickListener(onButtonClick);
        }
    }
    
    public void setConfirm(boolean confirm) {
        mReqConfirm = confirm;
    }
    
    public void setCallbacks (PinCallbacks callbacks) {
        mCallbacks = callbacks;
    }
    
    private void onOk() {
        
    }
    
    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
            case R.id.pin_0: mPinText.append("0"); break;
            case R.id.pin_1: mPinText.append("1"); break;
            case R.id.pin_2: mPinText.append("2"); break;
            case R.id.pin_3: mPinText.append("3"); break;
            case R.id.pin_4: mPinText.append("4"); break;
            case R.id.pin_5: mPinText.append("5"); break;
            case R.id.pin_6: mPinText.append("6"); break;
            case R.id.pin_7: mPinText.append("7"); break;
            case R.id.pin_8: mPinText.append("8"); break;
            case R.id.pin_9: mPinText.append("9"); break;
            case R.id.pin_ok:
                onOk();
                break;
            case R.id.pin_cancel:
                mCallbacks.pinWrong(REASON_CANCELED);
                break;
            }
        }
    };

    public interface PinCallbacks {
        void pinRight();
        void pinWrong(int reason);
    }
}
