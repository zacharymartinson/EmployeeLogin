package com.zachm.employeelogin.util

import android.graphics.Rect

data class TrackedFaces(var currentId: Int = 0, val box: Rect, val employee: Employee?, val embedding: Embedding)