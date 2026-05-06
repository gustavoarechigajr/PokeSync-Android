package com.pokesync.android.di

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor() {
    var token: String? = null
    var userId: String? = null
    var username: String? = null
}
