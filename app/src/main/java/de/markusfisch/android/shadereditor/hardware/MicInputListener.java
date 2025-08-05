package de.markusfisch.android.shadereditor.hardware;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class MicInputListener extends AbstractListener {
	private static final String TAG = "MicInputListener";
	private static final int SAMPLE_RATE = 44100; // Hz
	private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
			SAMPLE_RATE,
			CHANNEL_CONFIG,
			AUDIO_FORMAT);

	private AudioRecord audioRecord;
	private Thread recordingThread;
	private volatile boolean isRecording = false;
	private float currentAmplitude = 0f;

	private final Context context;

	public MicInputListener(Context context) {
		super(context);
		this.context = context;
	}

	@Override
	public void unregister() {
		if (!isRecording) {
			return;
		}

		isRecording = false;
		if (recordingThread != null) {
			try {
				recordingThread.join();
			} catch (InterruptedException e) {
				Log.e(TAG, "Error joining recording thread", e);
			}
			recordingThread = null;
		}

		if (audioRecord != null) {
			audioRecord.stop();
			audioRecord.release();
			audioRecord = null;
		}
		currentAmplitude = 0f;
	}

	public boolean register() {


		if (isRecording) {
			return true;
		}

		if (ContextCompat.checkSelfPermission(this.context, Manifest.permission.RECORD_AUDIO)
				!= PackageManager.PERMISSION_GRANTED) {
			Log.e(TAG, "RECORD_AUDIO permission not granted.");
			return false;
		}

		if (BUFFER_SIZE == AudioRecord.ERROR_BAD_VALUE || BUFFER_SIZE == AudioRecord.ERROR) {
			Log.e(TAG, "AudioRecord.getMinBufferSize returned an error.");
			return false;
		}

		audioRecord = new AudioRecord(
				MediaRecorder.AudioSource.MIC,
				SAMPLE_RATE,
				CHANNEL_CONFIG,
				AUDIO_FORMAT,
				BUFFER_SIZE);

		if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
			Log.e(TAG, "AudioRecord initialization failed.");
			audioRecord.release();
			audioRecord = null;
			return false;
		}

		isRecording = true;
		recordingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				processAudioData();
			}
		});
		recordingThread.start();

		return true;
	}

	private void processAudioData() {
		short[] buffer = new short[BUFFER_SIZE / 2]; // 16-bit PCM, so 2 bytes per sample
		audioRecord.startRecording();

		while (isRecording) {
			int read = audioRecord.read(buffer, 0, buffer.length);
			if (read > 0) {
				long sum = 0;
				for (int i = 0; i < read; i++) {
					sum += Math.abs(buffer[i]);
				}
				currentAmplitude = ((float) sum / read) / (float) Short.MAX_VALUE;
			}
		}
	}

	public float getAmplitude() {
		return currentAmplitude;
	}
}
