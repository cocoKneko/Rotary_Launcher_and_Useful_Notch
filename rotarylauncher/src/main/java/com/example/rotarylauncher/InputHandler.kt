package com.example.rotarylauncher

enum class DpadDirection { UP, DOWN, LEFT, RIGHT }

interface InputHandler {
    fun onRotate(detents: Int)
    fun onDpad(direction: DpadDirection)
    fun onCenterTap()
    fun onCenterLongPress()
}