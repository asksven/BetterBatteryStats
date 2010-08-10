package com.noshufou.android.su;

import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

public class ResponseHelper {
	private static final String TAG = "SuRequest";

	public static void sendResult(Context context, AppDetails appDetails, String socketPath) {
        LocalSocket socket;
        try {
            socket = new LocalSocket();
            socket.connect(new LocalSocketAddress(socketPath,
                    LocalSocketAddress.Namespace.FILESYSTEM));

            if (socket != null) {
                OutputStream os = socket.getOutputStream();
                String resultCode = appDetails.getPermissionCode();
                Log.d(TAG, "Sending result: " + resultCode + " for UID: " + appDetails.getUid());
                byte[] bytes = resultCode.getBytes("UTF-8");
                os.write(bytes);
                os.flush();
                os.close();
                socket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to socket '" + socketPath + "' for UID: " + appDetails.getUid());
            Log.e(TAG, e.getMessage(), e);
        }
    }
}
