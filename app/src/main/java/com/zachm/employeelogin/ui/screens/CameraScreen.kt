package com.zachm.employeelogin.ui.screens


import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.common.util.concurrent.ListenableFuture
import com.zachm.employeelogin.CameraViewModel
import com.zachm.employeelogin.R
import com.zachm.employeelogin.ui.theme.HomeBackground
import com.zachm.employeelogin.util.Employee
import com.zachm.employeelogin.util.TrackedFaces
import java.util.concurrent.Executor

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun CameraScreen(
    processCamera: ListenableFuture<ProcessCameraProvider>, lifeCycleOwner: LifecycleOwner, thread: Executor) {

    val viewModel: CameraViewModel = viewModel()

    val faces by viewModel.trackedFaces.collectAsState()
    val showAddScreen by viewModel.showAddScreen.collectAsState()

    var screenSize by remember { mutableStateOf(IntSize(0,0)) }
    val textMeasurer = rememberTextMeasurer()
    val text = remember { textMeasurer.measure(text = "+", style = TextStyle(fontSize = 80.sp, color = Color.Green, fontWeight = FontWeight.Thin)) }

    //For the text field TODO Logistics for if I want it in this screen
    val idText = remember { mutableStateOf("") }
    val nameText = remember { mutableStateOf("") }

    //Need this for the gui as the camera works in the backgroud. TODO Decide if I want it running in the background.
    val currentFace = remember { mutableStateOf<TrackedFaces?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { screenSize = it }
    ) {

        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = {
                viewModel.updateCameraFeed(processCamera, it.surfaceProvider, lifeCycleOwner, thread, screenSize)
            }
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { offset ->
                    faces?.let {
                        it.forEach { face ->
                            if(face.box.contains(offset.x.toInt(), offset.y.toInt())) {
                                currentFace.value = face
                                viewModel.showAddScreen()
                            }
                        }
                    }
                } }
        ) {
            faces?.let {

                it.forEach { face->

                    val color = if(face.employee != null) Color.Blue else Color.Green
                    val name = face.employee?.name ?: "Unknown"

                    drawRect(
                        color = color,
                        topLeft = Offset(face.box.left.toFloat(), face.box.top.toFloat()),
                        size = Size(face.box.width().toFloat(), face.box.height().toFloat()),
                        style = Stroke(width = 4f)
                    )

                    if(face.employee == null) {
                        drawText(
                            textLayoutResult = text,
                            topLeft = Offset(face.box.centerX().toFloat() - (text.size.width/2), face.box.centerY().toFloat() - (text.size.height/2))
                        )
                    }

                    drawText(
                        textLayoutResult = textMeasurer.measure(text = name, style = TextStyle(fontSize = 24.sp, color = color, fontWeight = FontWeight.Light)),
                        topLeft = Offset(face.box.left.toFloat(), face.box.top.toFloat() - 80f)
                    )
                }
            }

        }

        Log.d("CameraScreen", "ShowAddScreen: $showAddScreen")
        if(showAddScreen == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.6f)
                    .background(HomeBackground)
            ) {

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.75f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.weight(0.25f))

                    TextField(
                        value = nameText.value,
                        onValueChange = { nameText.value = it },
                        textStyle = TextStyle(fontSize = 20.sp),
                        singleLine = true,
                        placeholder = { Text(text = "Name", fontWeight = FontWeight.Light) },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedIndicatorColor = Color.White,
                            focusedIndicatorColor = Color.White,
                            focusedPlaceholderColor = Color.White,
                            unfocusedPlaceholderColor = Color.White,
                            cursorColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.weight(0.25f))

                    TextField(
                        value = idText.value,
                        onValueChange = { idText.value = it },
                        textStyle = TextStyle(fontSize = 20.sp),
                        singleLine = true,
                        placeholder = { Text(text = "Employee ID", fontWeight = FontWeight.Light) },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedIndicatorColor = Color.White,
                            focusedIndicatorColor = Color.White,
                            focusedPlaceholderColor = Color.White,
                            unfocusedPlaceholderColor = Color.White,
                            cursorColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Add/Update",
                        fontSize = 16.sp,
                        color = HomeBackground,
                        modifier = Modifier
                            .padding(bottom = 20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha=0.6f))
                            .padding(10.dp)
                            .clickable {
                                val face = currentFace.value!!
                                viewModel.addNewEmployee(nameText.value, idText.value.toInt(), face.embedding, face.currentId)
                                viewModel.closeAddScreen()
                            }
                    )

                }

            }
        }

    }
}

@Preview
@Composable
private fun preview() {
    var idText = remember { mutableStateOf("") }
    var nameText = remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(16.dp))
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.6f)
                .background(HomeBackground)
        ) {

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.75f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(0.25f))

                TextField(
                    value = nameText.value,
                    onValueChange = { nameText.value = it },
                    textStyle = TextStyle(fontSize = 20.sp),
                    placeholder = { Text(text = "Name", fontWeight = FontWeight.Light) },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        focusedPlaceholderColor = Color.White,
                        unfocusedPlaceholderColor = Color.White,
                        cursorColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.weight(0.25f))

                TextField(
                    value = idText.value,
                    onValueChange = { idText.value = it },
                    textStyle = TextStyle(fontSize = 20.sp),
                    placeholder = { Text(text = "Employee ID", fontWeight = FontWeight.Light) },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        focusedPlaceholderColor = Color.White,
                        unfocusedPlaceholderColor = Color.White,
                        cursorColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Add/Update",
                    fontSize = 16.sp,
                    color = HomeBackground,
                    modifier = Modifier
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha=0.6f))
                        .padding(10.dp)
                )

            }

        }

    }
}