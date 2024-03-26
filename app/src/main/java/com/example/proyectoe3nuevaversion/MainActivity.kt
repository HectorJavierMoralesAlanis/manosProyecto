package com.example.proyectoe3nuevaversion

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import com.example.proyectoe3.HandsResultGlRenderer
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.solutioncore.CameraInput
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView
import com.google.mediapipe.solutions.hands.HandLandmark
import com.google.mediapipe.solutions.hands.Hands
import com.google.mediapipe.solutions.hands.HandsOptions
import com.google.mediapipe.solutions.hands.HandsResult
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import java.lang.String

class MainActivity() : AppCompatActivity() {
    private var hands: Hands? = null

    private enum class InputSource {
        UNKNOWN,
        CAMERA
    }
    lateinit var sceneView: ArSceneView
    lateinit var loadingView: View
    lateinit var placeModelButton: ExtendedFloatingActionButton
    lateinit var newModelButton: ExtendedFloatingActionButton

    data class Model(
        val fileLocation: kotlin.String,
        val scaleUnits: Float? = null,
        val placementMode: PlacementMode = PlacementMode.BEST_AVAILABLE,
        val applyPoseRotation: Boolean = true
    )

    val models = listOf(
        Model("models/UPLOGO.glb")
    )
    var modelIndex = 0
    var modelNode: ArModelNode? = null


    private var inputSource = InputSource.UNKNOWN

    // Live camera demo UI and camera components.
    private var cameraInput: CameraInput? = null
    private var glSurfaceView: SolutionGlSurfaceView<HandsResult>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupLiveDemoUiComponents()
        sceneView = findViewById(R.id.sceneView)
    }

    override fun onResume() {
        super.onResume()
        if (inputSource == InputSource.CAMERA) {
            // Restarts the camera and the opengl surface rendering.
            cameraInput = CameraInput(this)
            cameraInput!!.setNewFrameListener { textureFrame -> hands?.send(textureFrame) }
            glSurfaceView?.post { startCamera() }
            glSurfaceView?.setVisibility(View.VISIBLE)
        }
    }

    override fun onPause() {
        super.onPause()
        if (inputSource == InputSource.CAMERA) {
            glSurfaceView?.setVisibility(View.GONE)
            cameraInput?.close()
        }
    }

    private fun startCamera() {
        cameraInput?.start(
            this,
            hands!!.getGlContext(),
            CameraInput.CameraFacing.BACK,
            glSurfaceView!!.getWidth(),
            glSurfaceView!!.getHeight()
        )
    }

    private fun setupLiveDemoUiComponents() {
        val startCameraButton = findViewById<Button>(R.id.button_start_camera)
        startCameraButton.setOnClickListener(
            { v: View? ->
                if (inputSource == InputSource.CAMERA) {
                    return@setOnClickListener
                }
                stopCurrentPipeline()
                setupStreamingModePipeline(InputSource.CAMERA)
            })
    }

    private fun setupStreamingModePipeline(inputSource: InputSource) {
        this.inputSource = inputSource
        // Initializes a new MediaPipe Hands solution instance in the streaming mode.
        hands = Hands(
            this,
            HandsOptions.builder()
                .setStaticImageMode(false)
                .setMaxNumHands(1) //NUMERO de manos
                .setRunOnGpu(RUN_ON_GPU)
                .build()
        )
        hands!!.setErrorListener { message, e ->
            Log.e(
                TAG,
                "MediaPipe Hands error:" + message
            )
        }
        if (inputSource == InputSource.CAMERA) {
            cameraInput = CameraInput(this)
            cameraInput!!.setNewFrameListener { textureFrame -> hands!!.send(textureFrame) }
        }

        // Initializes a new Gl surface view with a user-defined HandsResultGlRenderer.
        glSurfaceView = SolutionGlSurfaceView(this, hands!!.getGlContext(), hands!!.getGlMajorVersion())
        glSurfaceView!!.setSolutionResultRenderer(HandsResultGlRenderer())
        glSurfaceView!!.setRenderInputImage(true)
        hands!!.setResultListener { handsResult ->
            logWristLandmark(handsResult,  /*showPixelValues=*/false)
            glSurfaceView!!.setRenderData(handsResult)
            glSurfaceView!!.requestRender()
            //newModelNode()
        }

        // The runnable to start camera after the gl surface view is attached.
        // For video input source, videoInput.start() will be called when the video uri is available.
        if (inputSource == InputSource.CAMERA) {
            glSurfaceView!!.post { startCamera() }
        }

        // Updates the preview layout.
        val frameLayout = findViewById<FrameLayout>(R.id.preview_display_layout)
        frameLayout.removeAllViewsInLayout()
        frameLayout.addView(glSurfaceView)
        glSurfaceView!!.setVisibility(View.VISIBLE)
        //newModelNode()
        frameLayout.requestLayout()
    }

    fun newModelNode() {
        val model = models[modelIndex]
        modelIndex = (modelIndex + 1) % models.size
        modelNode = ArModelNode(model.placementMode).apply {
            applyPoseRotation = model.applyPoseRotation
            loadModelAsync(
                context = this@MainActivity,
                lifecycle = lifecycle,
                glbFileLocation = model.fileLocation,
                autoAnimate = true,
                scaleToUnits = model.scaleUnits,
                // Place the models origin at the bottom center
                centerOrigin = Position(y = -1.0f)
            ) {
                sceneView.planeRenderer.isVisible = true
            }

        }
        // Select the models node by default (the models node is also selected on tap)
        sceneView.selectedNode = modelNode
    }

    private fun stopCurrentPipeline() {
        if (cameraInput != null) {
            cameraInput!!.setNewFrameListener(null)
            cameraInput!!.close()
        }
        glSurfaceView?.setVisibility(View.GONE)
        hands?.close()
    }

    private fun logWristLandmark(result: HandsResult, showPixelValues: Boolean) {
        if (result.multiHandLandmarks().isEmpty()) {
            return
        }
        val wristLandmark: LandmarkProto.NormalizedLandmark =
            result.multiHandLandmarks().get(0).getLandmarkList().get(HandLandmark.WRIST)

        // For Bitmaps, show the pixel values. For texture inputs, show the normalized coordinates.
        if (showPixelValues) {
            val width: Int = result.inputBitmap().getWidth()
            val height: Int = result.inputBitmap().getHeight()
            Log.i(
                TAG,
                String.format(
                    "MediaPipe Hand wrist coordinates (pixel values): x=%f, y=%f",
                    wristLandmark.getX() * width, wristLandmark.getY() * height
                )
            )
        } else {
            Log.i(
                TAG,
                String.format(
                    "MediaPipe Hand wrist normalized coordinates (value range: [0, 1]): x=%f, y=%f",
                    wristLandmark.getX(), wristLandmark.getY()
                )
            )
        }
        if (result.multiHandWorldLandmarks().isEmpty()) {
            return
        }
        val wristWorldLandmark: LandmarkProto.Landmark =
            result.multiHandWorldLandmarks().get(0).getLandmarkList().get(HandLandmark.WRIST)
        Log.i(
            TAG,
            String.format(
                "MediaPipe Hand wrist world coordinates (in meters with the origin at the hand's"
                        + " approximate geometric center): x=%f m, y=%f m, z=%f m",
                wristWorldLandmark.getX(), wristWorldLandmark.getY(), wristWorldLandmark.getZ()
            )
        )
    }

    companion object {
        private val TAG = "MainActivity"

        // Run the pipeline and the models inference on GPU or CPU.
        private val RUN_ON_GPU = true
    }
}