package com.eq.jh.earthquakeplayer2.rxbus

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject


/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-25
 *
 * Singleton instance
 * A simple rx bus that emits the data to the subscribers using Observable
 * https://androidwave.com/rxbus-implementing-event-bus-with-rxjava/
 */

/*
 * Singleton instance
 * A simple rx bus that emits the data to the subscribers using Observable
 */
object RxBus {

    // onNext를 남발할 경우 배출 순서를 유지하기 위해서 직렬화 사용함
//    private val publisher = PublishSubject.create<Any>()
    private val publisher = PublishSubject.create<Any>().toSerialized()

    fun publish(event: Any) {
        publisher.onNext(event)
    }

    // Listen should return an Observable and not the publisher
    // Using ofType we filter only events that match that class type
    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
}