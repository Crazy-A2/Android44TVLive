package com.tvlive.app.player

interface PlayerCallback {
    fun onPrepared()
    fun onError(what: Int, extra: Int)
    fun onInfo(what: Int, extra: Int)
    fun onVideoSizeChanged(width: Int, height: Int)
}
