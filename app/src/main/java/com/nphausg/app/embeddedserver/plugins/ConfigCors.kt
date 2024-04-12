/*
 * Created by khue.nguyen on 4/12/24, 10:06 AM
 * Copyright (c) 2024 . All rights reserved.
 * Last modified 4/12/24, 10:06 AM
 */

package com.nphausg.app.embeddedserver.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCors() {
    // configures Cross-Origin Resource Sharing. CORS is needed to make calls from arbitrary
    // JavaScript clients, and helps us prevent issues down the line.
    install(CORS) {
        anyHost()
    }
}
