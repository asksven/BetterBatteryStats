/*
 * Copyright (C) 2011-2018 asksven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asksven.android.common.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.asksven.betterbatterystats.BbsApplication;
import com.asksven.betterbatterystats.ImportExportPreferencesActivity;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.data.StatsProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Map.Entry;

public class SharedPreferencesUtils
{

    static final String TAG = "SharedPreferencesUtils";

    public static boolean saveSharedPreferencesToFile(SharedPreferences prefs, String file)
    {
        Context ctx = BbsApplication.getAppContext();
        Uri fileUri = null;
        boolean res = false;

        if (!DataStorage.isExternalStorageWritable())
        {
            Log.e(TAG, "External storage can not be written");
            Toast.makeText(ctx, ctx.getString(R.string.message_external_storage_write_error),
                    Toast.LENGTH_SHORT).show();
        }

        String path = StatsProvider.getWritableFilePath();

        if (!path.equals(""))
        {
            File backup = new File(path + "/" + ImportExportPreferencesActivity.BACKUP_FILE);
            fileUri = Uri.fromFile(backup);

            ObjectOutputStream output = null;
            try
            {
                output = new ObjectOutputStream(new FileOutputStream(backup));
                output.writeObject(prefs.getAll());
                res = true;
                // workaround: force mediascanner to run
                DataStorage.forceMediaScanner(ctx, fileUri);

            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    if (output != null)
                    {
                        output.flush();
                        output.close();
                    }
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
            }
        }
        else
        {
            Log.i(TAG, "Write error. *" + path + "* couldn't be written");
        }
        return res;
    }

    @SuppressWarnings({"unchecked"})
    public static boolean loadSharedPreferencesFromFile(SharedPreferences prefs, String file)
    {
        Context ctx = BbsApplication.getAppContext();
        boolean res = false;

        if (!DataStorage.isExternalStorageWritable())
        {
            Log.e(TAG, "External storage can not be read");
            Toast.makeText(ctx, ctx.getString(R.string.message_external_storage_write_error),
                    Toast.LENGTH_SHORT).show();
        }

        String path = StatsProvider.getWritableFilePath();

        if (!path.equals(""))
        {
            File backup = new File(path + "/" + ImportExportPreferencesActivity.BACKUP_FILE);

            ObjectInputStream input = null;
            try
            {
                input = new ObjectInputStream(new FileInputStream(backup));
                Editor prefEdit = prefs.edit();
                prefEdit.clear();
                Map<String, ?> entries = (Map<String, ?>) input.readObject();
                for (Entry<String, ?> entry : entries.entrySet())
                {
                    Object v = entry.getValue();
                    String key = entry.getKey();

                    if (v instanceof Boolean)
                        prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
                    else if (v instanceof Float)
                        prefEdit.putFloat(key, ((Float) v).floatValue());
                    else if (v instanceof Integer)
                        prefEdit.putInt(key, ((Integer) v).intValue());
                    else if (v instanceof Long)
                        prefEdit.putLong(key, ((Long) v).longValue());
                    else if (v instanceof String)
                        prefEdit.putString(key, ((String) v));
                }
                prefEdit.commit();
                res = true;

            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    if (input != null)
                    {
                        input.close();
                    }
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
            }
        }
        else
        {
            Log.i(TAG, "Read error. *" + path + "* couldn't be read");
        }
        return res;
    }
}
