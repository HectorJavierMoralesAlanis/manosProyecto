// Copyright 2021 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.example.proyectoe3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import androidx.appcompat.widget.AppCompatImageView
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark
import com.google.mediapipe.solutions.hands.Hands
import com.google.mediapipe.solutions.hands.HandsResult

/** An ImageView implementation for displaying [HandsResult].  */
class HandsResultImageView(context: Context?) : AppCompatImageView(context!!) {
    private var latest: Bitmap? = null

    init {
        setScaleType(ScaleType.FIT_CENTER)
    }

    /**
     * Sets a [HandsResult] to render.
     *
     * @param result a [HandsResult] object that contains the solution outputs and the input
     * [Bitmap].
     */
    fun setHandsResult(result: HandsResult?) {
        if (result == null) {
            return
        }
        val bmInput: Bitmap = result.inputBitmap()
        val width = bmInput.getWidth()
        val height = bmInput.getHeight()
        latest = Bitmap.createBitmap(width, height, bmInput.getConfig())
        val canvas = Canvas(latest!!)
        canvas.drawBitmap(bmInput, Matrix(), null)
        val numHands: Int = result.multiHandLandmarks().size
        for (i in 0 until numHands) {
            drawLandmarksOnCanvas(
                result.multiHandLandmarks().get(i).getLandmarkList(),
                result.multiHandedness().get(i).getLabel().equals("Left"),
                canvas,
                width,
                height
            )
        }
    }

    /** Updates the image view with the latest [HandsResult].  */
    fun update() {
        postInvalidate()
        if (latest != null) {
            setImageBitmap(latest)
        }
    }

    private fun drawLandmarksOnCanvas(
        handLandmarkList: List<NormalizedLandmark>,
        isLeftHand: Boolean,
        canvas: Canvas,
        width: Int,
        height: Int
    ) {
        // Draw connections.
        for (c in Hands.HAND_CONNECTIONS) {
            val connectionPaint = Paint()
            connectionPaint.setColor(
                if (isLeftHand) LEFT_HAND_CONNECTION_COLOR else RIGHT_HAND_CONNECTION_COLOR
            )
            connectionPaint.strokeWidth = CONNECTION_THICKNESS.toFloat()
            val start: NormalizedLandmark = handLandmarkList[c.start()]
            val end: NormalizedLandmark = handLandmarkList[c.end()]
            canvas.drawLine(
                start.getX() * width,
                start.getY() * height,
                end.getX() * width,
                end.getY() * height,
                connectionPaint
            )
        }
        val landmarkPaint = Paint()
        landmarkPaint.setColor(if (isLeftHand) LEFT_HAND_LANDMARK_COLOR else RIGHT_HAND_LANDMARK_COLOR)
        // Draws landmarks.
        for (landmark in handLandmarkList) {
            canvas.drawCircle(
                landmark.getX() * width,
                landmark.getY() * height,
                LANDMARK_RADIUS.toFloat(),
                landmarkPaint
            )
        }
        // Draws hollow circles around landmarks.
        landmarkPaint.setColor(
            if (isLeftHand) LEFT_HAND_HOLLOW_CIRCLE_COLOR else RIGHT_HAND_HOLLOW_CIRCLE_COLOR
        )
        landmarkPaint.strokeWidth = HOLLOW_CIRCLE_WIDTH.toFloat()
        landmarkPaint.style = Paint.Style.STROKE
        for (landmark in handLandmarkList) {
            canvas.drawCircle(
                landmark.getX() * width,
                landmark.getY() * height,
                (
                        LANDMARK_RADIUS + HOLLOW_CIRCLE_WIDTH).toFloat(),
                landmarkPaint
            )
        }
    }

    companion object {
        private const val TAG = "HandsResultImageView"
        private val LEFT_HAND_CONNECTION_COLOR = Color.parseColor("#30FF30")
        private val RIGHT_HAND_CONNECTION_COLOR = Color.parseColor("#FF3030")
        private const val CONNECTION_THICKNESS = 8 // Pixels
        private val LEFT_HAND_HOLLOW_CIRCLE_COLOR = Color.parseColor("#30FF30")
        private val RIGHT_HAND_HOLLOW_CIRCLE_COLOR = Color.parseColor("#FF3030")
        private const val HOLLOW_CIRCLE_WIDTH = 5 // Pixels
        private val LEFT_HAND_LANDMARK_COLOR = Color.parseColor("#FF3030")
        private val RIGHT_HAND_LANDMARK_COLOR = Color.parseColor("#30FF30")
        private const val LANDMARK_RADIUS = 10 // Pixels
    }
}
