package appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam;

/**import android.app.Fragment; **/

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
public class Tab2 extends Fragment {

    String TAG = "Tab2";
    Button receiveButton;
    EditText centralFrequencyText;
    EditText sendMessageText;
    Intent mServiceIntent;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tab_2,container,false);
        receiveButton = (Button) v.findViewById(R.id.receiveButton);
        receiveButton.setOnClickListener(receiveButtonListener);

        mServiceIntent = new Intent(MainActivity.context, SignalRecorder.class);

        //EditText
        centralFrequencyText = (EditText) v.findViewById(R.id.selectFrequencyR);
        sendMessageText = (EditText) v.findViewById(R.id.sendMessage);
        return v;
    }

    View.OnClickListener receiveButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //			On click the signal recorder is turned on by starting the SignalRecorder service
            if (centralFrequencyText.getText().toString().equals("")) {
                Toast.makeText(getActivity() /*getApplicationContext()*/, "Error, Insert frequency!", Toast.LENGTH_SHORT).show();
            } else if (!SignalRecorder.isRecording) {
                SignalRecorder.isRecording = true;
                Toast.makeText(getActivity() /*getApplicationContext()*/, "Receiving...", Toast.LENGTH_SHORT).show();
                receiveButton.setText(R.string.stop);

                mServiceIntent.putExtra("CentralFrequency", centralFrequencyText.getText().toString());
                mServiceIntent.putExtra("Message", sendMessageText.getText().toString());

                Log.i(TAG, "CentralFrequency: " + centralFrequencyText.getText().toString());
                Log.i(TAG, "Message: " + sendMessageText.getText().toString());
                MainActivity.context.stopService(mServiceIntent);
                MainActivity.context.startService(mServiceIntent);

            } else if (SignalRecorder.isRecording) {
                SignalRecorder.isRecording = false;
                Toast.makeText(getActivity() /*getApplicationContext()*/, "Stopping...", Toast.LENGTH_SHORT).show();
                receiveButton.setText(R.string.rec);
                MainActivity.context.stopService(mServiceIntent);

            } else
                Toast.makeText(getActivity() /*getApplicationContext()*/, "Occupied, try later", Toast.LENGTH_SHORT).show();
        }
    };
}