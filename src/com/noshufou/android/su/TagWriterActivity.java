package com.noshufou.android.su;

import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

public class TagWriterActivity extends Activity {
    private static final String TAG = "Su.TagWriterActivity";
    
    public static final String EXTRA_TAG = "tag";
    public static final int TAG_ALLOW = 1;
    
    private TextView mStatusText = null;
    private NfcAdapter mNfcAdapter = null;
    
    private int mTagToWrite = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            finish();
        }

        mStatusText = new TextView(this);
        mStatusText.setGravity(Gravity.CENTER);
        mStatusText.setText(R.string.nfc_write_tag);
        
        mTagToWrite = getIntent().getIntExtra(EXTRA_TAG, 0);
        if (mTagToWrite == 0) {
            throw new IllegalArgumentException("You must specify a tag to write");
        }
        
        setContentView(mStatusText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (MalformedMimeTypeException e) {
            Log.e(TAG, "Bad MIME type declared", e);
            return;
        }
        IntentFilter[] filters = new IntentFilter[] { ndef };
        String[][] techLists = new  String[][] {
                new String[] { Ndef.class.getName() },
                new String[] { NdefFormatable.class.getName() }
        };
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, techLists);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        switch (mTagToWrite) {
        case TAG_ALLOW:
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            NdefRecord record = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
                    "text/x-su-a".getBytes(),
                    new byte[0],
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .getString("pin", "").getBytes());
            NdefMessage message = new NdefMessage(new NdefRecord[] {record });

            Ndef ndef = Ndef.get(tagFromIntent);
            if (ndef != null) {
                if (!ndef.isWritable()) {
                    Toast.makeText(this, "Tag not writeable", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                int maxSize = ndef.getMaxSize();
                Log.d(TAG, "Max tag size = " + maxSize + ", Message size = " + message.toByteArray().length);
                
                if (maxSize < message.toByteArray().length) {
                    Toast.makeText(this, "Tag not big enough", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    ndef.connect();
                    ndef.writeNdefMessage(message);
                    Log.d(TAG, "Tag written");
                    finish();
                } catch (IOException e) {
                    Log.e(TAG, "IOException", e);
                    return;
                } catch (FormatException e) {
                    Log.e(TAG, "FormatException", e);
                    return;
                }
            } else {
                NdefFormatable format = NdefFormatable.get(tagFromIntent);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        Log.d(TAG, "Formated tag");
                        finish();
                    } catch (IOException e) {
                        Log.e(TAG, "IOException", e);
                        return;
                    } catch (FormatException e) {
                        Log.e(TAG, "FormatException", e);
                        return;
                    }
                }
            }
            break;
        }
    }
}
