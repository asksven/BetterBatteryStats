package com.noshufou.android.su.widget;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.widget.SectionIndexer;

import com.noshufou.android.su.util.Util;

public class DateIndexer implements SectionIndexer {
    private static final String TAG = "Su.DateIndexer";
    
    private Cursor mCursor;
    private int mColumnIndex;
    private int mSectionCount;
    private String[] mSections;
    private int[] mSectionDates;
    private int[] mSectionPositions;
    private SimpleDateFormat mIntFormat;
    
    public DateIndexer(Context context, Cursor cursor, int sortedColumnIndex) {
        mCursor = cursor;
        mColumnIndex = sortedColumnIndex;
        mIntFormat = new SimpleDateFormat("yyyyDDD");
        GregorianCalendar calendar = new GregorianCalendar();
        
        mCursor.moveToFirst();
        long firstDateLong = mCursor.getLong(mColumnIndex);
        mCursor.moveToLast();
        long lastDateLong = mCursor.getLong(mColumnIndex);

        int firstDateInt = Integer.parseInt(mIntFormat.format(firstDateLong));
        int lastDateInt = Integer.parseInt(mIntFormat.format(lastDateLong));
        mSectionCount = (firstDateInt - lastDateInt) + 1;

        mSections = new String[mSectionCount];
        mSectionDates = new int[mSectionCount];
        mSectionPositions = new int[mSectionCount];

        calendar.setTimeInMillis(firstDateLong);
        for (int i = 0; i < mSectionCount; i++) {
            mSections[i] = Util.formatDate(context, calendar.getTimeInMillis());
            mSectionDates[i] = Integer.parseInt(mIntFormat.format(calendar.getTime()));
            mSectionPositions[i] = -1;
            calendar.add(GregorianCalendar.DATE, -1);
        }
    }

    @Override
    public int getPositionForSection(int section) {
        if (mCursor == null) {
            return 0;
        }
        
        if (section <= 0) {
            return 0;
        }
        
        if (section >= mSectionCount) {
            return mCursor.getCount();
        }
        
        if (mSectionPositions[section] > 0) {
            return mSectionPositions[section];
        }
        
        int start = 0;
        int end = mCursor.getCount();
        
        for (int i = section - 1; i > 0; i--) {
            if (mSectionPositions[i] > 0) {
                start = mSectionPositions[i];
                break;
            }
        }
        
        int savedCursorPos = mCursor.getPosition();
        long date;
        int dateInt;
        for (int i = start; i < end; i++) {
            if (mCursor.moveToPosition(i)) {
                date = mCursor.getLong(mColumnIndex);
                dateInt = Integer.parseInt(mIntFormat.format(date));
                if (mSectionDates[section] >= dateInt) {
                    mSectionPositions[section] = i;
                    return i;
                }
            }
        }
        mCursor.moveToPosition(savedCursorPos);
                
        return 0;
    }

    @Override
    public int getSectionForPosition(int position) {
        int savedCursorPos = mCursor.getPosition();
        mCursor.moveToPosition(position);
        long date = mCursor.getLong(mColumnIndex);
        mCursor.moveToPosition(savedCursorPos);
        int dateInt = Integer.parseInt(mIntFormat.format(date));
        // Simple linear search since there aren't that many sections.
        // May optimize this later if necessary.
        for (int i = 0; i < mSectionCount; i++) {
            if (dateInt == mSectionDates[i]) {
                return i;
            }
        }
        // If it wasn't found, something went very wrong. Log it
        Log.e(TAG, "Section not found for date " + dateInt);
        return 0;
    }

    @Override
    public Object[] getSections() {
        return mSections;
    }

}
