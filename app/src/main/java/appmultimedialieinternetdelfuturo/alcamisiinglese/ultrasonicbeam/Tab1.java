package appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam;

/**import android.app.Fragment; prima c'era questa**/

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * A simple {@link Fragment} subclass.
 */
public class Tab1 extends Fragment {
    String TAG = "Tab1";
    Button sendButton;
    Button syncButton;
    EditText centralFrequencyText;
    EditText sendMessageText;
    public static Activity Tab1;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tab_1, container, false);

        Tab1 = this.getActivity();

        //Buttons
        sendButton = (Button) v.findViewById(R.id.sendButton);
        sendButton.setOnClickListener(SendButtonListener);
        syncButton = (Button) v.findViewById(R.id.syncButtonS);
        syncButton.setOnClickListener(SyncButtonListener);

        //EditText
        centralFrequencyText = (EditText) v.findViewById(R.id.selectFrequencyS);
        sendMessageText = (EditText) v.findViewById(R.id.sendMessage);
        return v;
    }

    View.OnClickListener SendButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //			On click the signal generator is turned on by starting the SignalGenerator service
            if (centralFrequencyText.getText().toString().equals("") || sendMessageText.getText().toString().equals("")) {
                Toast.makeText(getActivity(), "Error, Insert parameters!", Toast.LENGTH_SHORT).show();
            } else if (!SignalGenerator.isGenerating && !SignalGenerator.isSyncing) {
                SignalGenerator.isGenerating = true;
                Toast.makeText(getActivity(), "Elaborating message...", Toast.LENGTH_SHORT).show();

                Intent mServiceIntent = new Intent(MainActivity.context, SignalGenerator.class);
                mServiceIntent.putExtra("CentralFrequency", centralFrequencyText.getText().toString());
                mServiceIntent.putExtra("Message", sendMessageText.getText().toString());

                Log.i(TAG, "CentralFrequency: " + centralFrequencyText.getText().toString());
                Log.i(TAG, "Message: " + sendMessageText.getText().toString());
                sendButton.setClickable(false);
                syncButton.setClickable(false);
                MainActivity.context.stopService(mServiceIntent);
                MainActivity.context.startService(mServiceIntent);
            } else
                Toast.makeText(getActivity(), "Occupied, try later", Toast.LENGTH_SHORT).show();

        }
    };

    View.OnClickListener SyncButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //			On click the signal generator is turned on by starting the SignalGenerator service
            if (centralFrequencyText.getText().toString().equals("")) {
                Toast.makeText(getActivity(), "Error, Insert Frequency!", Toast.LENGTH_SHORT).show();
            } else if (!SignalGenerator.isGenerating && !SignalGenerator.isSyncing) {
                SignalGenerator.isSyncing = true;
                Toast.makeText(getActivity(), "Syncing...", Toast.LENGTH_SHORT).show();

                Intent mServiceIntent = new Intent(MainActivity.context, SignalGenerator.class);
                mServiceIntent.putExtra("CentralFrequency", centralFrequencyText.getText().toString());

                Log.i(TAG, "Syncing");
                sendButton.setText(R.string.syncing);
                syncButton.setText(R.string.syncing);
                sendButton.setClickable(false);
                syncButton.setClickable(false);
                MainActivity.context.stopService(mServiceIntent);
                MainActivity.context.startService(mServiceIntent);
            } else
                Toast.makeText(getActivity(), "Occupied, try later", Toast.LENGTH_SHORT).show();
        }
    };
}