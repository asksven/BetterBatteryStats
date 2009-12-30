package org.zenthought.android.su; 

import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Intent;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.CheckBox;

public class SuRequest extends Activity {
    String resultCode = "DENY";
    String socketPath;
    LocalSocket socket;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.request);
 
        if (getCallingPackage() != null) {
            Log.e("SuRequest", "SuRequest must be started from su");
            finish();
            return;
        }

        if (savedInstanceState != null) {
            socketPath = savedInstanceState.getString("socket");
        } else {
            final Intent in = getIntent();
 
            final TextView callerId = (TextView)findViewById(R.id.callerId);
            final TextView callerCommand = (TextView)findViewById(R.id.callerCommand);
            final TextView desiredId = (TextView)findViewById(R.id.desiredId);
            final TextView desiredCommand = (TextView)findViewById(R.id.desiredCommand);

            socketPath = in.getStringExtra("socket");

            final int caller_pid = in.getIntExtra("caller_pid", -1);
            final String caller_uid = in.getStringExtra("caller_uid");
            final String caller_gid = in.getStringExtra("caller_gid");
            final String caller_bin = in.getStringExtra("caller_bin");
            final String caller_args = in.getStringExtra("caller_args");
            final String desired_uid = in.getStringExtra("desired_uid");
            final String desired_gid = in.getStringExtra("desired_gid");
            final String desired_cmd = in.getStringExtra("desired_cmd");
 
            callerId.setText("Process #" + caller_pid + " (" + caller_uid + ":" + caller_gid + ")");
            callerCommand.setText(caller_bin + " " + caller_args);

            desiredId.setText(desired_uid + ":" + desired_gid);
            desiredCommand.setText(desired_cmd);
        }

        findViewById(R.id.buttonAllow).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {clickHandler(view);}
        });
        findViewById(R.id.buttonDeny).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {clickHandler(view);}
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        try {
            socket = new LocalSocket();
            socket.connect(new LocalSocketAddress(socketPath, LocalSocketAddress.Namespace.FILESYSTEM));
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    public void clickHandler(View target) {
        final CheckBox checkBox = (CheckBox) findViewById(R.id.checkRemember);
        if (target.getId() == R.id.buttonAllow) {
            resultCode = "ALLOW";
        } else if (target.getId() == R.id.buttonDeny) {
            resultCode = "DENY";
        }
        if (checkBox.isChecked()) {
            resultCode = "ALWAYS_" + resultCode;
        }
        finish();
    }


    private void sendResult() throws IOException
    {
        OutputStream os = socket.getOutputStream();
        byte[] bytes = resultCode.getBytes("UTF-8");
        os.write(bytes);
        os.flush();
    }

    public void onSaveInstanceState(Bundle state)
    {
        super.onSaveInstanceState(state);
        state.putString("socket", socketPath);
    }
    
    @Override
    public void onStop()
    {
        super.onStop();

        if (!isFinishing()) {
            finish();
        }

        try {
            sendResult();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            socket.shutdownOutput();
            socket.shutdownInput();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        socket = null;
        socketPath = null;
    }
}
