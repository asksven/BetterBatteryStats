package com.noshufou.android.su;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.noshufou.android.su.util.Util;

public class PinActivity extends Activity implements OnClickListener {
//    private static final String TAG = "Su.PinActivity";
    
    public static final int MODE_NEW = 1;
    public static final int MODE_CHANGE = 2;
    public static final int MODE_CHECK = 3;
    public static final int MODE_SECRET_CODE = 4;
    
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_ATTEMPTS_ALLOWED = "attempts_allowed";
    public static final String EXTRA_PIN = "pin";
    public static final String EXTRA_SECRET_CODE = "secret_code";
    
    private String mPinConfirm = "";
    private int mAttemptsAllowed = 3;
    private int mAttempts = 0; 
    
    private int mMode = 0;
    
    private EditText mPinText;
    private int mOriginalHintTextColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_pin);
        
        mPinText = (EditText) findViewById(R.id.pin);
        mOriginalHintTextColor = mPinText.getCurrentHintTextColor();

        ((Button)findViewById(R.id.pin_0)).setOnClickListener(this);
        ((Button)findViewById(R.id.pin_1)).setOnClickListener(this);
        ((Button)findViewById(R.id.pin_2)).setOnClickListener(this);
        ((Button)findViewById(R.id.pin_3)).setOnClickListener(this);
        ((Button)findViewById(R.id.pin_4)).setOnClickListener(this);
        ((Button)findViewById(R.id.pin_5)).setOnClickListener(this);
        ((Button)findViewById(R.id.pin_6)).setOnClickListener(this);
        ((Button)findViewById(R.id.pin_7)).setOnClickListener(this);
        ((Button)findViewById(R.id.pin_8)).setOnClickListener(this);
        ((Button)findViewById(R.id.pin_9)).setOnClickListener(this);

        Button okButton = (Button) findViewById(R.id.pin_ok);
        okButton.setOnClickListener(this);
        okButton.setText(R.string.ok);
        Button cancelButton = (Button) findViewById(R.id.pin_cancel);
        cancelButton.setOnClickListener(this);
        cancelButton.setText(R.string.cancel);
        
        mMode = getIntent().getIntExtra(EXTRA_MODE, 0);
        switch (mMode) {
        case MODE_NEW:
            mPinText.setHint(R.string.pin_new_pin);
            break;
        case MODE_CHANGE:
        case MODE_CHECK:
            mPinText.setHint(R.string.pin_enter_pin);
            break;
        case MODE_SECRET_CODE:
            mPinText.setVisibility(View.GONE);
            mPinText = (EditText) findViewById(R.id.secret_code);
            findViewById(R.id.secret_code_layout).setVisibility(View.VISIBLE);
            break;
        default:
            throw new IllegalArgumentException("You must specify an operating mode");
        }
        
        if (getIntent().hasExtra(EXTRA_ATTEMPTS_ALLOWED)) {
            mAttemptsAllowed = getIntent().getIntExtra(EXTRA_ATTEMPTS_ALLOWED, 0);
        }
    }
    
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
            setResult(RESULT_CANCELED);
            finish();
            break;
        }
    }
    
    private void onOk() {
        if (mPinText.getText().equals("")) {
            return;
        }
        
        switch (mMode) {
        case MODE_NEW:
            String enteredPin = mPinText.getText().toString();
            if (mPinConfirm.equals("")) {
                mPinConfirm = mPinText.getText().toString();
                mPinText.setText("");
                mPinText.setHint(R.string.pin_confirm_pin);
                mPinText.setHintTextColor(mOriginalHintTextColor);
            } else if (enteredPin.equals(mPinConfirm)) {
                Intent intent = new Intent();
                intent.putExtra(EXTRA_PIN, Util.getHash(enteredPin));
                setResult(RESULT_OK, intent);
                finish();
            } else {
                mPinConfirm = "";
                mPinText.setText("");
                mPinText.setHint(R.string.pin_mismatch);
                mPinText.setHintTextColor(Color.RED);
            }
            break;
        case MODE_CHANGE:
        case MODE_CHECK:
            if (Util.checkPin(this, mPinText.getText().toString())) {
                if (mMode == MODE_CHECK) {
                    setResult(RESULT_OK);
                    finish();
                } else {
                    mPinText.setText("");
                    mPinText.setHint(R.string.pin_new_pin);
                    mPinText.setHintTextColor(mOriginalHintTextColor);
                    mMode = MODE_NEW;
                }
            } else {
                if (mAttempts + 1 < mAttemptsAllowed) {
                    mAttempts++;
                    mPinText.setText("");
                    mPinText.setHint(getResources()
                            .getQuantityString(R.plurals.pin_incorrect_try,
                                    mAttemptsAllowed - mAttempts,
                                    mAttemptsAllowed - mAttempts));
                    mPinText.setHintTextColor(Color.RED);
                } else {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
            break;
        case MODE_SECRET_CODE:
            Intent intent = new Intent();
            intent.putExtra(EXTRA_SECRET_CODE, mPinText.getText());
            setResult(RESULT_OK, intent);
            finish();
        }
    }

}
