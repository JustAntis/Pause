package com.justantis.pause.domain.repository

/**
 * Deletes everything the app has stored on the device.
 */
interface DataWiper {
    suspend fun wipe()
}
