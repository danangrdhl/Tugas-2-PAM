package com.example.newsfeedsimulator

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform