package appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import java.nio.charset.Charset;

import appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam.libs.MathUtils;

public class SignalGenerator extends Service {

    private final String TAG = "SignalGenerator";
    // Signal Settings
    int centralFrequency;

    public static boolean isGenerating;
    public static boolean isSyncing;

    static double lenInSec = 0.1; //seconds
    static int lenInSamples = (int) (lenInSec * MainActivity.SAMPLE_RATE); //4410
    double sample[];
    boolean[] binary;
    byte generatedSnd[];
    public static int trailerSize = 8;
    AudioTrack audioTrack;


    static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        if (intent == null)
            return Service.START_NOT_STICKY;
        Bundle extras = intent.getExtras();
        if (extras != null) {
            centralFrequency = Integer.parseInt(extras.getString("CentralFrequency"));
            if (extras.containsKey("Message")) {
                toBinary(extras.getString("Message"));
                sample = new double[binary.length * lenInSamples];
                generatedSnd = new byte[2 * sample.length];
            } else
                binary = new boolean[1];
        }

        new Thread(new Runnable() {
            public void run() {
                if (isSyncing || isGenerating) {

                    if (isSyncing)
                        sample = new double[100 * lenInSamples];

                    generatedSnd = new byte[2 * sample.length];

                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            MainActivity.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                            AudioTrack.MODE_STATIC);
                    genTone(isSyncing);
                    playSound();
                    Log.i(TAG, (isSyncing ? "Syncing" : "playSound()"));
                    try {
                        //Thread.sleep((long) (lenInSec * (isSyncing ? 100 : binary.length) * 1000));
                        Thread.sleep((long) (generatedSnd.length / (2 * MainActivity.SAMPLE_RATE) * 1000));

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    audioTrack.flush();
                    audioTrack.stop();
                    audioTrack.release();
                    isGenerating = false;
                    MainActivity.MainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.MainActivity,
                                    ((isSyncing ? "SYNC " : "") + "MESSAGE SENT"),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                    Tab1.Tab1.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Button sendButton = (Button) Tab1.Tab1.findViewById(R.id.sendButton);
                            Button syncButton = (Button) Tab1.Tab1.findViewById(R.id.syncButtonS);
                            sendButton.setText(R.string.send);
                            syncButton.setText(R.string.sync);
                            sendButton.setClickable(true);
                            syncButton.setClickable(true);
                        }
                    });
                    isSyncing = false;
                }
            }
        }).start();
        return Service.START_STICKY;
    }


    void genTone(boolean sync) {
        double[] windowSamples = generateBW(lenInSamples);
        //sample = new double[binary.length * lenInSamples];

        for (int k = 0; k < (sync ? 100 : binary.length); k++) //if syncing => length = 100
            if ((sync ? true : binary[k])) //if syncing then all ones, otherwise OOK modulation.
                for (int i = k * lenInSamples; i < (k + 1) * lenInSamples; i++)
                    sample[i] = windowSamples[k] * Math.sin(centralFrequency * 2 * Math.PI * i /
                            (MainActivity.SAMPLE_RATE));
                        /*
                         * Obviously I could simplify the condition (sync? true : binary[k]),
                         * with (sync || binary[k]), but when I'm syncing I do not initialize
                         * the array binary. So I won't simplify the code.
                         */

        //normalization
        double sampleMax = MathUtils.max(sample);
        for (int i = 0; i < sample.length; i++)
            sample[i] /= sampleMax;


        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767)); //Map the double [-1, 1] to short [-32767, 32767]

            // in 16 bit wav PCM, first byte is the low order byte

            //Logical AND last 8bit with 0000000011111111 and byte-cast
            generatedSnd[idx++] = (byte) (val & 0x00ff);

            //Logical AND last 8bit with 1111111100000000
            //8-bit right-shift and byte-cast
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
    }

    void playSound() {
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        if (isSyncing)
            MainActivity.MainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.MainActivity, "Press Synchronize on the other device",
                            Toast.LENGTH_SHORT).show();
                }
            });
        audioTrack.play();
    }

    static public double[] generateBW(int length) {
        // generate nSamples Blackman window function values
        int m = length / 2;
        double r;
        double pi = Math.PI;
        double[] w = new double[length];
        // Blackman Window
        r = pi / m;
        for (int n = -m; n < m; n++)
            w[m + n] = 0.42f + 0.5f * Math.cos(n * r) + 0.08f * Math.cos(2 * n * r);
        return w;
    }

    public void toBinary(String message) {
        byte[] bytes = message.getBytes(UTF8_CHARSET);

        boolean[] binaryTemp = new boolean[bytes.length * 8];
        binary = new boolean[binaryTemp.length + trailerSize];

        for (int i = 0; i < (binaryTemp.length); i++) {
            if (((bytes[i / 8] & (1 << (7 - (i % 8)))) > 0))
                binaryTemp[i] = true;
        }
        for (int i = 0; i < trailerSize; i++)
            binary[i] = true;
        System.arraycopy(binaryTemp, 0, binary, trailerSize - 1, binaryTemp.length);
    }
}