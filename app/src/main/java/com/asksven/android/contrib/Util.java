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
package com.asksven.android.contrib;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;


public class Util {
    private static final String TAG = "Util";
    
    
    

    public static ArrayList<String> run(String command) {
        return run("/system/bin/sh", command);
    }

    public static ArrayList<String> run(String shell, String command) {
        return run(shell, new String[] {
                command
        });
    }

    public static ArrayList<String> run(String shell, ArrayList<String> commands) {
        String[] commandsArray = new String[commands.size()];
        commands.toArray(commandsArray);
        return run(shell, commandsArray);
    }

    public static ArrayList<String> run(String shell, String[] commands) {
        ArrayList<String> output = new ArrayList<String>();

        try {
            Process process = Runtime.getRuntime().exec(shell);

            BufferedOutputStream shellInput =
                    new BufferedOutputStream(process.getOutputStream());
            BufferedReader shellOutput =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            for (String command : commands) {
                Log.i(TAG, "command: " + command);
                shellInput.write((command + " 2>&1\n").getBytes());
            }

            shellInput.write("exit\n".getBytes());
            shellInput.flush();

            String line;
            while ((line = shellOutput.readLine()) != null) {
//                Log.d(TAG, "command output: " + line);
                output.add(line);
            }

            process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return output;
    }

    public static boolean writeStoreFile(Context context, int uid, int execUid, String cmd, int allow) {
        File storedDir = new File(context.getFilesDir().getAbsolutePath() + File.separator + "stored");
        storedDir.mkdirs();
        if (cmd == null) {
            Log.d(TAG, "App stored for logging purposes, file not required");
            return false;
        }
        String fileName = uid + "-" + execUid;
        try {
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(
                    new File(storedDir.getAbsolutePath() + File.separator + fileName)));
            out.write(cmd);
            out.write('\n');
            out.write(String.valueOf(allow));
            out.write('\n');
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Store file not written", e);
            return false;
        } catch (IOException e) {
            Log.w(TAG, "Store file not written", e);
            return false;
        }
        return true;
    }
    
    public static boolean writeDetaultStoreFile(Context context, String action) {
       File storedDir = new File(context.getFilesDir().getAbsolutePath() + File.separator + "stored");
       storedDir.mkdirs();
       File defFile = new File(storedDir.getAbsolutePath() + File.separator + "default");
       try {
           OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(defFile.getAbsolutePath()));
           if (action.equals("allow")) {
               out.write("1");
           } else if (action.equals("deny")) {
               out.write("0");
           } else {
               out.write("-1");
           }
           out.flush();
           out.close();
       } catch (FileNotFoundException e) {
           Log.w(TAG, "Default file not written", e);
           return false;
       } catch (IOException e) {
           Log.w(TAG, "Default file not written", e);
           return false;
       }
       return true;
    }
}
