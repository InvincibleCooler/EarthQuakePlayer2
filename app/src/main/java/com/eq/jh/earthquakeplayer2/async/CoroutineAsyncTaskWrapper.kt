package com.eq.jh.earthquakeplayer2.async


import kotlinx.coroutines.*


/**
 * Copyright (C) 2020 Kakao Inc. All rights reserved.
 */
abstract class CoroutineAsyncTaskWrapper<Param, Return> : CoroutineScope {
    companion object {
        private const val TAG = "CoroutineAsyncTaskWrapper"
    }

    private var job = Job()
    override val coroutineContext = Dispatchers.Main + job // preTask, postTask에서 UI처리가 있을수 있기 때문에 일단 Dispatcher는 Main으로 설정함

    private fun startTask(param: Param? = null) {
        launch {
            preTask()
            val result = doInBackground(param)
            postTask(result)
        }
    }

    open fun preTask() {
    }

    private suspend fun doInBackground(param: Param? = null): Return? {
        return withContext(Dispatchers.IO) {
            backgroundWork(param)
        }
    }

    abstract suspend fun backgroundWork(param: Param? = null): Return?

    open fun postTask(result: Return? = null) {
    }

    fun cancel() {
        job.cancel()
    }

    fun execute(param: Param? = null) {
        startTask(param)
    }
}