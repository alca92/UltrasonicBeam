package appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam;

/**import android.app.Fragment; **/

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


/**
 * A simple {@link Fragment} subclass.
 */
public class Tab2 extends Fragment {

    String TAG = "Tab2";
    Button receiveButton;
    Button syncButton;
    EditText centralFrequencyText;
    TextView receiveMessageText;
    Intent mServiceIntent;
    private Handler mHandler = new Handler();
    public static Activity Tab2;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tab_2,container,false);
        receiveButton = (Button) v.findViewById(R.id.receiveButton);
        receiveButton.setOnClickListener(receiveButtonListener);
        syncButton = (Button) v.findViewById(R.id.syncButtonR);
        syncButton.setOnClickListener(SyncButtonListener);

        //EditText
        centralFrequencyText = (EditText) v.findViewById(R.id.selectFrequencyR);
        receiveMessageText = (TextView) v.findViewById(R.id.receiveMessage);
        return v;
    }

    View.OnClickListener receiveButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //			On click the signal recorder is turned on by starting the SignalRecorder service
            if (centralFrequencyText.getText().toString().equals("")) {
                Toast.makeText(getActivity(), "Error, Insert frequency!", Toast.LENGTH_SHORT).show();
            } else if (!SignalRecorder.isRecording && !SignalRecorder.isSyncing) {
                SignalRecorder.isRecording = true;
                Toast.makeText(getActivity(), "Receiving...", Toast.LENGTH_SHORT).show();
                receiveButton.setText(R.string.stop);
                syncButton.setClickable(false);
                mServiceIntent = new Intent(MainActivity.context, SignalRecorder.class);
                mServiceIntent.putExtra("CentralFrequency", centralFrequencyText.getText().toString());
                Log.i(TAG, "CentralFrequency: " + centralFrequencyText.getText().toString());

                //MainActivity.context.stopService(mServiceIntent);
                if (!SignalRecorder.isRunning)
                    MainActivity.context.startService(mServiceIntent);
            } else if (SignalRecorder.isRecording) {
                SignalRecorder.isRecording = false;
//                MainActivity.context.stopService(mServiceIntent);
//                I won't stop the Service right now but in a while

                Toast.makeText(getActivity(), "Stopping...", Toast.LENGTH_SHORT).show();
                receiveButton.setClickable(false);
                receiveButton.setText(R.string.stopping);

                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        MainActivity.context.stopService(mServiceIntent);
                        receiveButton.setText(R.string.rec);
                        SignalRecorder.isRunning = false;
                        receiveButton.setClickable(true);
                        syncButton.setClickable(true);
                    }
                }, 5000);
            } else
                Toast.makeText(getActivity(), "Occupied, try later", Toast.LENGTH_SHORT).show();
        }
    };

    View.OnClickListener SyncButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //			On click the signal recorder is turned on by starting the SignalRecorder service
            if (centralFrequencyText.getText().toString().equals("")) {
                Toast.makeText(getActivity(), "Error, Insert frequency!", Toast.LENGTH_SHORT).show();
            } else if (!SignalRecorder.isRecording && !SignalRecorder.isSyncing) {
                SignalRecorder.isSyncing = true;
                Toast.makeText(getActivity(), "Syncing...", Toast.LENGTH_SHORT).show();

                receiveButton.setText(R.string.syncing);
                syncButton.setText(R.string.syncing);
                receiveButton.setClickable(false);
                syncButton.setClickable(false);

                mServiceIntent = new Intent(MainActivity.context, SignalRecorder.class);
                mServiceIntent.putExtra("CentralFrequency", centralFrequencyText.getText().toString());
                Log.i(TAG, "CentralFrequency: " + centralFrequencyText.getText().toString());

                SignalRecorder.syncOnes = true;

                //MainActivity.context.stopService(mServiceIntent);
                if (!SignalRecorder.isRunning)
                    MainActivity.context.startService(mServiceIntent); //Here the service starts

                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        SignalRecorder.syncOnes = false;
                        SignalRecorder.messageEnded = true;
                    }
                }, 6000);
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        SignalRecorder.syncOnes = false;
                        MainActivity.context.stopService(mServiceIntent);
                        receiveButton.setText(R.string.rec);
                        syncButton.setText(R.string.sync);
                        SignalRecorder.isSyncing = false;
                        receiveButton.setClickable(true);
                        syncButton.setClickable(true);
                    }
                }, 10000);
            } else
                Toast.makeText(getActivity(), "Occupied, try later", Toast.LENGTH_SHORT).show();
        }
    };

}