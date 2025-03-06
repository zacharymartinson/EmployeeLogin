package com.zachm.employeelogin.util

import android.graphics.Rect

data class TrackedFaces(private var currentId: Int = 0, private val box: Rect, private val employee: Employee)