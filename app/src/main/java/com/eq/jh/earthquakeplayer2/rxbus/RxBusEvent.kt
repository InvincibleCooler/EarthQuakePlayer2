package com.eq.jh.earthquakeplayer2.rxbus


/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-25
 *
 */
class RxBusEvent {

    /**
     * progress bar event (show or hide)
     */
    data class EventLoadingProgress(val isShow: Boolean)
}