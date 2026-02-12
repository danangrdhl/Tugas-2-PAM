package com.example.newsfeedsimulator

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "NewsFeedSimulator",
    ) {
        App()
    }
}