package com.eq.jh.earthquakeplayer2.listener


/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 09/04/2020
 *
 */
interface RetrieveItemValue<IN, OUT> {
    fun getItemValue(`in`: IN): OUT
}