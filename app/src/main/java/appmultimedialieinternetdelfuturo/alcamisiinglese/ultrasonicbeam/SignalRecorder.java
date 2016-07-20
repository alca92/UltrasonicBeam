package appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import ddf.minim.analysis.FFT;
import ddf.minim.effects.BandPass;

public class SignalRecorder extends Service {

    private final String TAG = "SignalGenerator";

    // Signal Settings
    int centralFrequency;
    public static boolean isRecording;

    // Create a new AudioRecord object to record the audio.
    int bufferSize;
    private AudioRecord audioInput = null;
    static short[] audioBuffer = null;
    float[] buffer = null;
    final int channelConfiguration = AudioFormat.CHANNEL_IN_MONO; //CONFIGURATION becomes IN cause is deprecated.
    final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    private ddf.minim.effects.BandPass bpf = null;
    private ddf.minim.analysis.FFT fft = null;
    private double window[] = null;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Bundle extras = intent.getExtras();
        centralFrequency = Integer.parseInt(extras.getString("CentralFrequency"));

        bufferSize = AudioRecord.getMinBufferSize(centralFrequency,
                channelConfiguration, audioEncoding);
        audioBuffer = new short[bufferSize];
        audioInput = new AudioRecord(MediaRecorder.AudioSource.MIC,
                centralFrequency, channelConfiguration, audioEncoding, bufferSize);
        audioInput.startRecording();
        bpf = new BandPass(centralFrequency, 100, 44100);
        fft = new FFT(bufferSize, 44100);
        window = SignalGenerator.generateBW();

        new Thread(new Runnable() {
            public void run() {
                while (isRecording) {
                    Log.i(TAG, "recording...");
                    //take audio buffer from AudioRecord
                    int bufferReadResult = audioInput.read(audioBuffer, 0, bufferSize);
                    //check correct reading
                    if (AudioRecord.ERROR_INVALID_OPERATION != bufferReadResult) {
                        //convert to float to elaborate te input
                        buffer = shortToFloat(audioBuffer);
                        //Apply Band Pass Filter to the audio recorded
                        bpf.process(buffer);
                        //Windowing to improve FFT output
                        for (int i = 0; i < bufferSize; i++)
                            buffer[i] *= window[i];
                        //Finally, apply FFT!
                        fft.forward(buffer);

                    }
                }
                audioInput.stop();
                audioInput.release();
            }
        }).start();
        return Service.START_STICKY;
    }

    /**
     * Convert 16bit short[] audio to 32 bit float format.
     * From [-32768,32768] to [-1,1]
     *
     * @param audio is the buffer that has to be converted
     */
    private float[] shortToFloat(short[] audio) {

        float[] converted = new float[audio.length];

        for (int i = 0; i < converted.length; i++) {
            // [-3276,32768] -> [-1,1]
            converted[i] = audio[i] / 32768f;
        }

        return converted;
    }
}