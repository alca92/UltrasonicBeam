package appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam.libs.MathUtils;
import appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam.libs.Useful;
import ddf.minim.analysis.FFT;
import ddf.minim.effects.BandPass;

public class SignalRecorder extends Service {

    private final String TAG = "SignalRecorder";

    // Signal Settings
    int centralFrequency;
    public static boolean isRecording;
    public static boolean isRunning;
    public static boolean isSyncing;
    public static boolean syncOnes;
    public static boolean messageStarted;
    public static boolean messageEnded;
    boolean[] binary;

    // Create a new AudioRecord object to record the audio.
    int bufferSize;
    private AudioRecord audioInput = null;
    static short[] audioBuffer = null;
    float[] buffer = null;
    float[] paddingBuffer = null;
    int zeros;

    private ddf.minim.effects.BandPass bpf = null;
    private ddf.minim.analysis.FFT fft = null;
    private double window[] = null;

    //TODO choose:
    public double freqAmplitude = 0;
    public double maxFreqAmplitude = 0;
    public double maxNoiseAmplitude = 0;

//    public static double[] power = null;
//    public double maxPower = 0;
//    public double maxNoisePower = 0;

    public double noiseThreshold = 0;

    //Queue to save
    Useful totalQueue = new Useful();
    Useful lastQueue = new Useful();


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

        int minBufferSize = AudioRecord.getMinBufferSize(
                MainActivity.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        bufferSize = MathUtils.closestPowerOfTwoAbove(
                (minBufferSize > SignalGenerator.lenInSamples ?
                        minBufferSize : SignalGenerator.lenInSamples));//to obtain a power of 2

        Log.i("bufferSize", Integer.toString(bufferSize));

        audioBuffer = new short[bufferSize];

        try {
            totalQueue.EmptyQueue();
            lastQueue.EmptyQueue();
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

                        //if Syncing or the app detected a message incoming,
                        if (isSyncing || messageStarted)
                            try {
                                //save the audioBuffer
                                totalQueue.Put(buffer);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        /*
                         * If isn't Syncing:
                         * a) if the message is incoming, look if it's ended,
                         * b) if the message has not already started, watch for the beginning
                         */
                        if (!isSyncing) {
                            Process();
//                            double tempMaxPower = MathUtils.getAbsMax(power);
//                            if (MathUtils.getAbsMax(power) > noiseThreshold) {
                            if (freqAmplitude > noiseThreshold) {
                                zeros = 0;
                                if (!messageStarted) //case b)
                                    messageStarted = true;
                            } else {
                                if (messageStarted)
                                    zeros++;
                                if (zeros > 10)
                                    messageEnded = true;
                            }
                        }
                        //If the message is ended
                        if (messageEnded && (messageStarted || isSyncing)) {
                            //Reset messageStarted and messageEnded
                            messageStarted = false;
                            messageEnded = false;

                            if (!isSyncing)
                                binary = new boolean[totalQueue.Size() / SignalGenerator.lenInSamples];

                            //extract from buffer
                            for (int i = 0;
                                 i < totalQueue.Size() / MathUtils.closestPowerOfTwoAbove(
                                         SignalGenerator.lenInSamples); i++) {
                                try {
                                    //Copy saved audio in buffer in order to process it.
                                    buffer = totalQueue.Take(MathUtils.closestPowerOfTwoAbove(
                                            SignalGenerator.lenInSamples));
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                //Process audio chunk.
                                BufferSwap(i == 0);
                                Process();
//                                double tempMaxPower = MathUtils.getMax(power);
//                                if (!isSyncing) {
//                                    if (tempMaxPower / maxPower > noiseThreshold)
//                                        binary[i] = true;
//                                } else { //if is Syncing
//                                    if (tempMaxPower > maxPower && syncOnes)
//                                        maxPower = tempMaxPower;
//
//                                    if (!syncOnes)
//                                        if (tempMaxPower > maxNoisePower) {
//                                            maxNoisePower = tempMaxPower;
//                                            noiseThreshold = maxNoisePower / maxPower ;
//                                            noiseThreshold *= 0.9;
//                                        }
//                                }
                                if (!isSyncing) {
                                    if (freqAmplitude / maxFreqAmplitude > noiseThreshold)
                                        binary[i] = true;
                                } else { //if is Syncing
                                    if (freqAmplitude > maxFreqAmplitude && syncOnes)
                                        maxFreqAmplitude = freqAmplitude;

                                    if (!syncOnes)
                                        if (freqAmplitude > maxNoiseAmplitude) {
                                            maxNoiseAmplitude = freqAmplitude;
                                            noiseThreshold = maxNoiseAmplitude / maxFreqAmplitude;
                                            noiseThreshold *= 1.1;
                                        }
                                }

                                if (!isSyncing)
                                    DecodeMessage();
                            }
                            if (isSyncing)
                                isSyncing = false;
                        }
//                        final FileOutputStream outputStream;
//                        Log.i(TAG, getFilesDir().toString());
//                        try {
//                            outputStream = openFileOutput("myFile", Context.MODE_APPEND);
////                            for (double s : power)
//                            outputStream.write((Double.toString(maxPower)).getBytes());
//                            outputStream.close();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
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
        window = SignalGenerator.generateBW(buffer.length);
        //Windowing to improve FFT or Hilbert output
        for (int i = 0; i < buffer.length; i++)
            buffer[i] *= window[i];

        //Finally, apply FFT !
        fft = new FFT(buffer.length, MainActivity.SAMPLE_RATE);
        fft.forward(buffer);
        freqAmplitude = fft.getFreq(centralFrequency);
//        //prepare to apply Hilbert transformation
//        double[] transformed = new double[buffer.length];
//
//        //float to double
//        for (int j = 0; j < buffer.length; j++) {
//            transformed[j] = (double) buffer[j];
//        }
//
//        //Apply Hilbert transformation!
//        ComplexArray hilbert = Hilbert.transform(transformed);
//        //Calculate power of transformation
//        power = MathUtils.abs(hilbert);
    }

    /* At the first call, I take the last paddingLength samples from buffer and I save them
     * in paddingBuffer.
     * Then, in the other calls, I take paddingBuffer and I save it for the "swap" in
     * swapBuffer. THEN
     */
    private void BufferSwap(boolean first) {
        int totalLength = MathUtils.closestPowerOfTwoAbove(SignalGenerator.lenInSamples);
        int paddingLength = totalLength - SignalGenerator.lenInSamples;
        if (first) {
            paddingBuffer = new float[paddingLength];
            System.arraycopy(buffer, SignalGenerator.lenInSamples, paddingBuffer, 0, paddingLength);
        } else {
            float[] swapBuffer = paddingBuffer;
            System.arraycopy(buffer, SignalGenerator.lenInSamples, paddingBuffer, 0, paddingLength);
            System.arraycopy(buffer, 0, buffer, paddingLength, SignalGenerator.lenInSamples);
            System.arraycopy(swapBuffer, 0, buffer, 0, paddingLength);
        }
    }

    private void DecodeMessage() {
        byte[] unicodeMessage = new byte[binary.length / 8];
        for (int i = 0; i < unicodeMessage.length; i++) {
            for (int bit = 0; bit < 8; bit++) {
                if (binary[i * 8 + bit]) {
                    unicodeMessage[i] |= (128 >> bit);
                }
            }
        }

        try {
            totalQueue.EmptyQueue();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final String message = new String(unicodeMessage, SignalGenerator.UTF8_CHARSET);

        Log.i(TAG, message);

        //send Message on screen
        Tab2.Tab2.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView receiveMessageText = (TextView) Tab2.Tab2.findViewById(R.id.receiveMessage);
                receiveMessageText.setText(message);
            }
        });
    }
}