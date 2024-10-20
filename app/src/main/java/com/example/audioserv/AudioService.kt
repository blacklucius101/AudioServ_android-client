package com.example.audioserv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket

class AudioService : Service() {

    private val TAG = "AudioStreamerService"
    private val CHANNEL_ID = "AudioStreamerChannel"
    private val SAMPLE_RATE = 44100
    private val CHANNELS = AudioFormat.CHANNEL_OUT_STEREO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNELS, AUDIO_FORMAT)
    private val PORT = 12345

    private lateinit var audioTrack: AudioTrack
    private var isPlaying = true

    override fun onCreate() {
        super.onCreate()

        // Create the notification channel
        createNotificationChannel()

        // Initialize AudioTrack using AudioTrack.Builder
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNELS)
                    .setEncoding(AUDIO_FORMAT)
                    .build()
            )
            .setBufferSizeInBytes(BUFFER_SIZE)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audioTrack.play()

        // Start the service in the foreground
        val notification = createNotification()
        startForeground(1, notification)

        // Start receiving and playing audio
        CoroutineScope(Dispatchers.IO).launch {
            receiveAudio()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPlaying = false
        audioTrack.stop()
        audioTrack.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Audio Streamer Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Streaming Service")
            .setContentText("Receiving and playing audio")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    private fun receiveAudio() {
        try {
            DatagramSocket(PORT).use { socket ->
                val buffer = ByteArray(BUFFER_SIZE)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isPlaying) {
                    socket.receive(packet)
                    audioTrack.write(packet.data, 0, packet.length)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error receiving audio", e)
        }
    }
}