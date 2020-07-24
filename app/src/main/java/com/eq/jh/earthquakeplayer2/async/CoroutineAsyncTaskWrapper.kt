package com.eq.jh.earthquakeplayer2.async


import kotlinx.coroutines.*


/**
 * Copyright (C) 2020 Kakao Inc. All rights reserved.
 * Created by Invincible on 19/06/2020
 * T는 파라미터, R은 Return
 */
abstract class CoroutineAsyncTaskWrapper<T, R> : CoroutineScope {
    companion object {
        private const val TAG = "CoroutineAsyncTaskWrapper"
    }

    private var job = Job()
    override val coroutineContext = Dispatchers.Main + job // preTask, postTask에서 UI처리가 있을수 있기 때문에 일단 Dispatcher는 Main으로 설정함

    private fun startTask(param: T? = null) {
        launch {
            preTask()
            val result = doInBackground(param)
            postTask(result)
        }
    }

    open fun preTask() {
    }

    private suspend fun doInBackground(param: T? = null): R? {
        return withContext(Dispatchers.IO) {
            backgroundWork(param)
        }
    }

    abstract suspend fun backgroundWork(param: T? = null): R?

    open fun postTask(result: R? = null) {
    }

    fun cancel() {
        job.cancel()
    }

    fun execute(param: T? = null) {
        startTask(param)
    }
}