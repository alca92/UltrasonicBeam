package appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam.libs.ComplexArray;
import appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam.libs.Hilbert;
import appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam.libs.MathUtils;
import appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam.libs.Useful;
import ddf.minim.effects.BandPass;

import static java.lang.System.arraycopy;

public class SignalRecorder extends Service {

    private final String TAG = "SignalRecorder";

    // Signal Settings
    int centralFrequency;
    public static boolean isRecording;
    public static boolean isRunning;
    public static boolean isSyncing;
    boolean[] binary;

    // Create a new AudioRecord object to record the audio.
    int bufferSize;
    private AudioRecord audioInput = null;
    static short[] audioBuffer = null;
    float[] buffer = null;

    private ddf.minim.effects.BandPass bpf = null;
    //private double window[] = null;

    public static double[] envelope = null;
    int peakIndex;
    public double noiseThreshold = 0;

    //Queue to save
    Useful audioQueue = new Useful();


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null)
            return Service.START_NOT_STICKY;

        Bundle extras = intent.getExtras();
        centralFrequency = Integer.parseInt(extras.getString("CentralFrequency"));

        bufferSize = AudioRecord.getMinBufferSize(
                MainActivity.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

//        bufferSize = MathUtils.closestPowerOfTwoAbove(
//                (minBufferSize > SignalGenerator.lenInSamples ?
//                        minBufferSize : SignalGenerator.lenInSamples));//to obtain a power of 2

        Log.i("bufferSize", Integer.toString(bufferSize));

        audioBuffer = new short[bufferSize];

        try {
            audioQueue.EmptyQueue();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        audioInput = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                MainActivity.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        audioInput.startRecording();

        bpf = new BandPass(centralFrequency, 20, MainActivity.SAMPLE_RATE);

        new Thread(new Runnable() {
            public void run() {
                isRunning = true;
//                Tab2.Tab2.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        TextView receiveMessageText = (TextView) Tab2.Tab2.findViewById(R.id.receiveMessage);
//                        receiveMessageText.setText("per me è la cipolla");
//                    }
//                });
                if (SignalGenerator.isSyncing)
                    return;
                while (isRecording || isSyncing) {
                    Log.i(TAG, (isSyncing ? "Syncing" : "recording"));
                    //take audio buffer from AudioRecord
                    int bufferReadResult = audioInput.read(audioBuffer, 0, bufferSize);

                    //check correct reading
                    if (AudioRecord.ERROR_INVALID_OPERATION != bufferReadResult) {
                        //convert to float to elaborate the input
                        buffer = ShortToFloat(audioBuffer); //my float[] audioBuffer

                        try {
                            //save the audioBuffer
                            audioQueue.Put(buffer);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        //If the message is ended
                        if (audioQueue.Size() > SignalGenerator.lenInSamples * 100) {

                            if (!isSyncing)
                                binary = new boolean[audioQueue.Size() / SignalGenerator.lenInSamples];

                            //extract from buffer
                            for (int i = 0;
                                 i < audioQueue.Size() / MathUtils.closestPowerOfTwoAbove(
                                         SignalGenerator.lenInSamples); i++) {
                                try {
                                    //Copy saved audio in buffer in order to process it.
                                    buffer = audioQueue.Take(audioQueue.Size());
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            Process();
                            if (isSyncing) {
                                double tempNoiseThreshold = (MathUtils.max(envelope) -
                                        MathUtils.min(envelope)) / 2;
                                if (noiseThreshold < tempNoiseThreshold)
                                    noiseThreshold = tempNoiseThreshold;

                                //send Message on screen
                                MainActivity.MainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.MainActivity,
                                                "SYNC COMPLETE",
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                int maxIndex = MathUtils.getMaxIndex(envelope);
                                //Index of beginning of the Symbol having maxIndex
                                int symbolIndex = maxIndex - SignalGenerator.lenInSamples / 2;
                                int numberOfPreviousSymbols = (symbolIndex / SignalGenerator.lenInSamples)
                                        * SignalGenerator.lenInSamples;
                                if (numberOfPreviousSymbols == 0)
                                    peakIndex = symbolIndex + SignalGenerator.lenInSamples / 2;
                                else
                                    peakIndex = symbolIndex % numberOfPreviousSymbols
                                            + SignalGenerator.lenInSamples / 2;

                                for (int i = peakIndex, j = 0; i < envelope.length;
                                     i += SignalGenerator.lenInSamples, j++) {
                                    if (envelope[i] > noiseThreshold)
                                        binary[j] = true;
                                    DecodeMessage();
                                }
                            }
                        }
                    } else {
                        Log.i(TAG, "Failed");
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
    private float[] ShortToFloat(short[] audio) {
        float[] converted = new float[audio.length];

        for (int i = 0; i < converted.length; i++) {
            // [-3276,32768] -> [-1,1]
            converted[i] = audio[i] / 32768f;
        }
        return converted;
    }

    private void Process() {
        //Apply Band Pass Filter to the audio recorded
        bpf.process(buffer);
        //window = SignalGenerator.generateBW(buffer.length);
        //Windowing to improve FFT or Hilbert output
//        for (int i = 0; i < buffer.length; i++)
//            buffer[i] *= window[i];

//        //Finally, apply FFT !
//        fft = new FFT(buffer.length, MainActivity.SAMPLE_RATE);
//        fft.forward(buffer);
//        freqAmplitude = fft.getFreq(centralFrequency);

        //prepare to apply Hilbert transformation
        double[] transformed = new double[buffer.length];

        //float to double
        for (int j = 0; j < buffer.length; j++) {
            transformed[j] = (double) buffer[j];
        }

        //Apply Hilbert transformation!
        ComplexArray hilbert = Hilbert.transform(transformed);
        //Calculate power of transformation
        envelope = MathUtils.abs(hilbert);
    }

    private void DecodeMessage() {
        int startMessageIndex;
        boolean messDetected = false;
        for (startMessageIndex = 0; startMessageIndex < binary.length; startMessageIndex++)
            if (binary[startMessageIndex]) {
                messDetected = true;
                break;
            }

        if (!messDetected) {
//            MainActivity.MainActivity.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(MainActivity.MainActivity,
//                            "receiving...",
//                            Toast.LENGTH_SHORT).show();
//                }
//            });
        } else {
            boolean[] binaryMessage = new boolean[binary.length - (startMessageIndex + SignalGenerator.trailerSize)];
//        for (int k = 0, j = i; k < binaryMessage.length; i++, k++)
//            binaryMessage[i]=binary[j];
            arraycopy(binary, (startMessageIndex + SignalGenerator.trailerSize - 1), binaryMessage, 0, binaryMessage.length);


            byte[] unicodeMessage = new byte[binaryMessage.length / 8];
            for (int i = 0; i < unicodeMessage.length; i++) {
                for (int bit = 0; bit < 8; bit++) {
                    if (binaryMessage[i * 8 + bit]) {
                        unicodeMessage[i] |= (128 >> bit);
                    }
                }
            }

            final String message = new String(unicodeMessage, SignalGenerator.UTF8_CHARSET);

            Log.i(TAG, message);

            //send Message on screen
            MainActivity.MainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.MainActivity, message, Toast.LENGTH_LONG).show();
                }
            });
        }
        try {
            audioQueue.EmptyQueue();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}