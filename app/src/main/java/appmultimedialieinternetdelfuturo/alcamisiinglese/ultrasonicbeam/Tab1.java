package appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam;

/**import android.app.Fragment; prima c'era questa**/

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
    EditText centralFrequencyText;
    EditText sendMessageText;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tab_1, container, false);

        sendButton = (Button) v.findViewById(R.id.sendButton);
        sendButton.setOnClickListener(SendButtonListener);

        //EditText
        centralFrequencyText = (EditText) v.findViewById(R.id.selectFrequency);
        sendMessageText = (EditText) v.findViewById(R.id.sendMessage);
        return v;
    }
    View.OnClickListener SendButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //			On click the signal generator is turned on by starting the SignalGenerator service
            if (!SignalGenerator.isRunning) {
                SignalGenerator.isRunning = true;
                Toast.makeText(getActivity() /*getApplicationContext()*/ , "Sending...", Toast.LENGTH_SHORT).show();

                Intent mServiceIntent = new Intent(MainActivity.context, SignalGenerator.class);
                mServiceIntent.putExtra("CentralFrequency", centralFrequencyText.getText().toString());
                mServiceIntent.putExtra("Message", sendMessageText.getText().toString());

                Log.i(TAG, "CentralFrequency: " + centralFrequencyText.getText().toString());
                Log.i(TAG, "Message: " + sendMessageText.getText().toString());
                MainActivity.context.startService(mServiceIntent);
            }
        }
    };
}
