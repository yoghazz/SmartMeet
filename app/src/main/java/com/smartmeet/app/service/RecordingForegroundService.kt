package com.smartmeet.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream

class RecordingForegroundService : Service() {

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val chunkSeconds = 5

    private var recorder: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var outputFile: File? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopRecording()
            else -> {
                val sessionId = intent?.getStringExtra(EXTRA_SESSION_ID) ?: "unknown"
                val mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_REALTIME
                startRecording(sessionId, mode)
            }
        }
        return START_STICKY
    }

    private fun startRecording(sessionId: String, mode: String) {
        if (isRecording) return
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBuffer <= 0) return

        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate, channelConfig, audioFormat,
            maxOf(minBuffer, sampleRate * 2 * chunkSeconds)
        )
        recorder?.startRecording()
        isRecording = true

        if (mode == MODE_BATCH || mode == MODE_HYBRID) {
            outputFile = File(cacheDir, "smartmeet_$sessionId.pcm")
            recordingThread = Thread {
                val buffer = ByteArray(sampleRate * 2 * chunkSeconds)
                FileOutputStream(outputFile).use { fos ->
                    while (isRecording) {
                        val read = recorder?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) fos.write(buffer, 0, read)
                    }
                }
            }.also { it.start() }
        }
    }

    private fun stopRecording() {
        isRecording = false
        recordingThread?.join(2000)
        recorder?.stop()
        recorder?.release()
        recorder = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun getBatchFile(): File? = outputFile

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, RecordingForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("SmartMeet sedang merekam")
            .setContentText("Ketuk untuk menghentikan rekaman")
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPending)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Recording", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "recording_channel"
        const val ACTION_STOP = "com.smartmeet.app.action.STOP_RECORDING"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_MODE = "recording_mode"
        const val MODE_REALTIME = "realtime"
        const val MODE_BATCH = "batch"
        const val MODE_HYBRID = "hybrid"
        const val NOTIF_ID = 101
    }
}
