package com.pokesync.android.di

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerUrlProvider @Inject constructor() {
    var url: String = "http://localhost:5000/"
}
