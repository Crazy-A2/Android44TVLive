package com.tvlive.app.player

import android.media.AudioManager
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer

class PlayerManager {

    private var player: IjkMediaPlayer? = null
    private var callback: PlayerCallback? = null
    private var surfaceView: SurfaceView? = null
    private var decoderMode: DecoderMode = DecoderMode.AUTO
    private var isPrepared = false

    fun init(surfaceView: SurfaceView) {
        this.surfaceView = surfaceView
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                player?.setDisplay(holder)
            }
            override fun surfaceChanged(holder: SurfaceHolder, fmt: Int, w: Int, h: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                player?.pause()
            }
        })
    }

    fun setCallback(callback: PlayerCallback) {
        this.callback = callback
    }

    fun play(url: String) {
        stop()
        isPrepared = false
        val p = IjkMediaPlayer()
        configurePlayer(p)
        p.setDataSource(surfaceView?.context, Uri.parse(url))
        surfaceView?.holder?.let { p.setDisplay(it) }
        p.setOnPreparedListener { mp ->
            isPrepared = true
            mp.start()
            callback?.onPrepared()
        }
        p.setOnErrorListener { _, what, extra ->
            callback?.onError(what, extra)
            true
        }
        p.setOnInfoListener { _, what, extra ->
            callback?.onInfo(what, extra)
            true
        }
        p.setOnVideoSizeChangedListener { _, width, height ->
            callback?.onVideoSizeChanged(width, height)
        }
        p.prepareAsync()
        player = p
    }

    fun stop() {
        player?.let {
            it.stop()
            it.release()
        }
        player = null
        isPrepared = false
    }

    fun release() {
        stop()
        surfaceView = null
        callback = null
    }

    fun setVolume(level: Float) {
        player?.setVolume(level, level)
    }

    fun isPlaying(): Boolean = player?.isPlaying == true

    fun setDecoderMode(mode: DecoderMode) {
        decoderMode = mode
    }

    private fun configurePlayer(p: IjkMediaPlayer) {
        when (decoderMode) {
            DecoderMode.AUTO -> {
                // 默认行为：优先硬解，失败回退软解
            }
            DecoderMode.SOFTWARE -> {
                p.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0L)
                p.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0L)
                p.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 0L)
            }
            DecoderMode.HARDWARE -> {
                p.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1L)
                p.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1L)
                p.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1L)
            }
        }

        // 起播优化
        p.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1000000L)
        p.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 1048576L)
        p.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1L)
        p.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1L)
        p.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48L)

        // 缓冲
        p.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-buffer-size", 1500000L)
        p.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 25L)
    }

    companion object {
        const val MEDIA_INFO_BUFFERING_START = IMediaPlayer.MEDIA_INFO_BUFFERING_START
        const val MEDIA_INFO_BUFFERING_END = IMediaPlayer.MEDIA_INFO_BUFFERING_END
    }
}
