package appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.nio.charset.Charset;

public class SignalGenerator extends Service {

    private final String TAG = "SignalGenerator";
    // Signal Settings
    int centralFrequency;
    private static final int SAMPLE_RATE = 44100;

    public static boolean isRunning;

    double lenInSec = 0.01;
    int lenInSamples = (int) (lenInSec * SAMPLE_RATE);
    double sample[];
    boolean[] binary;
    byte generatedSnd[];
    AudioTrack audioTrack;

    String charset = "UTF-16";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();
        centralFrequency = Integer.parseInt(extras.getString("CentralFrequency"));
        toBinary(extras.getString("Message"));

        sample = new double[binary.length];
        generatedSnd = new byte[2 * binary.length];

        new Thread(new Runnable() {
            public void run() {
                if (isRunning) {
                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                            AudioTrack.MODE_STATIC);
                    genTone();
                    playSound();

                    Log.i(TAG, "playSound()");
                    try {
                        Thread.sleep((long) (lenInSec * binary.length));
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    audioTrack.flush();
                    audioTrack.stop();
                    audioTrack.release();
                    isRunning = false;
                }
            }
        }).start();
        return Service.START_STICKY;
    }

    void genTone() {
        double[] windowSamples = generateBW();

        sample = new double[binary.length];

        for (boolean aBinary : binary)
            if (aBinary)
                for (int i = 0; i < lenInSamples && i < binary.length; i++)
                    sample[i] = windowSamples[i] * Math.sin(centralFrequency * 2 * Math.PI * i / (SAMPLE_RATE));


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
        //Toast.makeText(getApplicationContext(),"Sending...",Toast.LENGTH_SHORT).show();
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
        //Toast.makeText(getApplicationContext(), "Message transmitted", Toast.LENGTH_SHORT).show();
    }

    public double[] generateBW() {
        // generate nSamples Blackman window function values
        int m = lenInSamples / 2;
        double r;
        double pi = Math.PI;
        double[] w = new double[lenInSamples];
        // Blackman Window
        r = pi / m;
        for (int n = -m; n < m; n++)
            w[m + n] = 0.42f + 0.5f * Math.cos(n * r) + 0.08f * Math.cos(2 * n * r);
        return w;
    }

//    public String toBinary(byte[] bytes) {
//        StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
//        for (int i = 0; i < Byte.SIZE * bytes.length; i++)
//            sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
//        return sb.toString();
//    }

    public void toBinary(String message) {
        byte[] bytes = message.getBytes(Charset.forName(charset));

        binary = new boolean[bytes.length * 8];

        for (int i = 0; i < bytes.length * 8; i++) {
            if ((bytes[i / 8] & (1 << (7 - (i % 8)))) > 0)
                binary[i] = true;
        }
    }
}
