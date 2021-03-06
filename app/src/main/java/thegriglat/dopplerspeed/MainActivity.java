package thegriglat.dopplerspeed;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    /* RecordAudio recordTask;*/
    private  int RECORDER_SAMPLERATE = 8000;
    private static final double SOUND_SPEED = 340.29; //  m/s
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    AudioRecord mRecorder = null;
    int bufferSize;
    int blockSize = 256; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 1; // 2 bytes in 16bit format
    boolean isRecording;

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

    private double getDopplerSpeed(double freq, double w0){
        // w0 == 42 is 2500 rpm
        double v = 0;
        v = SOUND_SPEED * (1 - w0 / freq);
        return v;
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
            int freq = 0;
            while (isRecording) {
                // gets the voice output from microphone to byte format
                try {
                    mRecorder.read(sData, 0, blockSize);
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
