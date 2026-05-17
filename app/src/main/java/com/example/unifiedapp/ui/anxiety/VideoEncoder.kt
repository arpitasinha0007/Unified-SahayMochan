package com.example.unifiedapp.ui.anxiety


import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.*
import android.view.Surface
import java.io.IOException

class VideoEncoder(
    private val outputPath: String,
    private val width: Int,
    private val height: Int,
    private val fps: Int = 30,
    private val bitRate: Int = 4_000_000
) {

    private val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
    private val timeoutUs = 10000L

    private lateinit var encoder: MediaCodec
    private lateinit var inputSurface: Surface
    private lateinit var muxer: MediaMuxer

    private var trackIndex = -1
    private var muxerStarted = false
    private val bufferInfo = MediaCodec.BufferInfo()

    fun start() {
        val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        encoder = MediaCodec.createEncoderByType(mimeType)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = encoder.createInputSurface()
        encoder.start()

        muxer = MediaMuxer(outputPath,
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    fun encodeFrame(bitmap: Bitmap, frameIndex: Int) {

        val canvas: Canvas = inputSurface.lockCanvas(null)
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        inputSurface.unlockCanvasAndPost(canvas)

        drainEncoder(false)
    }

    fun stop() {
        drainEncoder(true)

        encoder.stop()
        encoder.release()

        if (muxerStarted) {
            muxer.stop()
        }
        muxer.release()
    }

    private fun drainEncoder(endOfStream: Boolean) {

        if (endOfStream) {
            encoder.signalEndOfInputStream()
        }

        while (true) {

            val outputBufferId =
                encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)

            when {

                outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) break
                }

                outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (muxerStarted) {
                        throw RuntimeException("Format changed twice")
                    }

                    val newFormat = encoder.outputFormat
                    trackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    muxerStarted = true
                }

                outputBufferId >= 0 -> {

                    val encodedData =
                        encoder.getOutputBuffer(outputBufferId)
                            ?: throw RuntimeException("Null buffer")

                    if (bufferInfo.size > 0) {

                        if (!muxerStarted) {
                            throw RuntimeException("Muxer not started")
                        }

                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(
                            bufferInfo.offset + bufferInfo.size
                        )

                        muxer.writeSampleData(
                            trackIndex,
                            encodedData,
                            bufferInfo
                        )
                    }

                    encoder.releaseOutputBuffer(outputBufferId, false)

                    if ((bufferInfo.flags and
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                    ) {
                        break
                    }
                }
            }
        }
    }

    fun saveVideoFromFrames(
        frames: List<Bitmap>,
        outputPath: String
    ) {

        if (frames.isEmpty()) return

        val width = frames[0].width
        val height = frames[0].height

        val encoder = VideoEncoder(
            outputPath = outputPath,
            width = width,
            height = height,
            fps = 30
        )

        encoder.start()

        frames.forEachIndexed { index, bitmap ->
            encoder.encodeFrame(bitmap, index)
        }

        encoder.stop()
    }
}