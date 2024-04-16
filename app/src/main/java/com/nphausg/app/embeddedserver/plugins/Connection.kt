/*
 * Created by 09032 on 4/16/24, 11:25 PM
 * Copyright (c) 2024 . All rights reserved.
 * Last modified 4/16/24, 11:25 PM
 */

package com.nphausg.app.embeddedserver.plugins

import io.ktor.websocket.*
import java.util.concurrent.atomic.*

class Connection(val session: DefaultWebSocketSession) {
    companion object {
        val lastId = AtomicInteger(0)
    }
    val name = "user${lastId.getAndIncrement()}"
}