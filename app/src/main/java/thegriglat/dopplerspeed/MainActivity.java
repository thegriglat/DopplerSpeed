package thegriglat.dopplerspeed;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Timer;
import ca.uol.aig.fftpack.RealDoubleFFT;

public class MainActivity extends AppCompatActivity {
    /* RecordAudio recordTask;*/
    private  int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    AudioRecord mRecorder = null;
    int bufferSize;
    int blockSize = 256; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 1; // 2 bytes in 16bit format
    Thread recordingThread;
    boolean isRecording;
    RealDoubleFFT transformer;

    private Timer myTimer;
    private TextView textlabel;

    /* END audio records */

    MyTask mt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textlabel = (TextView) findViewById(R.id.textlabel);
        RECORDER_SAMPLERATE = getValidSampleRates();
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, blockSize * BytesPerElement);
        isRecording = true;
        mt = new MyTask();
        mt.execute();

    }

    public int getValidSampleRates() {
        for (int rate : new int[] {8000, 11025, 16000, 22050, 44100}) {  // add the rates you wish to check against
            int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_CONFIGURATION_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0) {
                return rate;
            }
        }
        return 8000;
    }

    private double getMax(double[] arr) {
        if (arr.length == 0) return 0;
        double m = arr[0];
        for (int i = 0; i < arr.length; i++)
            if (arr[i] > m) m = arr[i];
        return m;
    }

    private void publishTransform(double[] transform) {
        double freq = (getMax(transform) * RECORDER_SAMPLERATE) / blockSize;
        try {
            textlabel.setText(Double.toString(freq));
        } catch (Exception e){
            textlabel.setText(e.getMessage());
        }
    }

    public static int calculate(int sampleRate, short [] audioData){

        int numSamples = audioData.length;
        int numCrossing = 0;
        for (int p = 0; p < numSamples-1; p++)
        {
            if ((audioData[p] > 0 && audioData[p + 1] <= 0) ||
                    (audioData[p] < 0 && audioData[p + 1] >= 0))
            {
                numCrossing++;
            }
        }

        float numSecondsRecorded = (float)numSamples/(float)sampleRate;
        float numCycles = numCrossing/2;
        float frequency = numCycles/numSecondsRecorded;

        return (int)frequency;
    }

    class MyTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                writeAudioDataToFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private void writeAudioDataToFile() throws InterruptedException {
            // Write the output audio in byte
            mRecorder.startRecording();
            short sData[] = new short[blockSize];
            transformer = new RealDoubleFFT(blockSize);
            int bufferReadResult;
            double[] toTransform = new double[blockSize];
            int freq = 0;
            while (isRecording) {
                // gets the voice output from microphone to byte format
                try {
                    bufferReadResult = mRecorder.read(sData, 0, blockSize);
                    /*for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                        toTransform[i] = (double) sData[i] / 32768.0;
                    }
                    transformer.ft(toTransform);
                    //transformer.ft(toTransform);
                    //byte bData[] = short2byte(sData);
                    //os.write(bData, 0, blockSize * BytesPerElement);

                    publishProgress(getMax(toTransform));
                    */
                    freq = calculate(RECORDER_SAMPLERATE, sData);
                    publishProgress(freq);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Thread.sleep(200);
            }

        }
        @Override
        protected void onProgressUpdate(Integer ... value) {
            super.onProgressUpdate(value);
            textlabel.setText(Double.toString(value[0]) + " ?");
        }
    }

}
