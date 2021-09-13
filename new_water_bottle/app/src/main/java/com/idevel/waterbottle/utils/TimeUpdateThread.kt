package com.idevel.waterbottle.utils

import android.os.Handler
import android.os.RemoteException
import com.google.android.exoplayer2.SimpleExoPlayer

/**
 * Created by jjbae on 2017. 9. 21..
 */

class TimeUpdateThread : Thread {
    var isFinish = false
        private set
    private var mBreak = false
    private var mPlayer: SimpleExoPlayer? = null
    private var mHandler: Handler? = null

    private constructor() {}

    constructor(player: SimpleExoPlayer, handler: Handler) {
        this.mPlayer = player
        this.mHandler = handler
    }

    override fun run() {
        while (!mBreak) {
            if (mPlayer == null) {
                mBreak = true
            } else {
                try {
                    if (mPlayer?.playWhenReady!!) {
                        val currentTime = mPlayer?.currentPosition?.toInt()
                        mHandler?.sendEmptyMessage(currentTime!!)
                    }

                    Thread.sleep(1000)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    //                    e.printStackTrace();
                }

            }
        }
        isFinish = true
    }

    fun cancel() {
        mBreak = true
        interrupt()
    }
}
