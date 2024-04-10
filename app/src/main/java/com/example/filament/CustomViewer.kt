package com.example.filament

import android.content.Context
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceView
import com.google.android.filament.Skybox
import com.google.android.filament.gltfio.Animator

import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer

class CustomViewer {

    companion object {
        init {
            Utils.init()
        }
    }

    private lateinit var choreographer: Choreographer
    private lateinit var modelViewer: ModelViewer
    private var startAnimationSequence = false
    private var animationSequence = intArrayOf()



    fun loadEntity() {
        choreographer = Choreographer.getInstance()
    }

    fun loadAnimator() {
        choreographer = Choreographer.getInstance()
    }


    fun setSurfaceView(mSurfaceView: SurfaceView) {
        modelViewer = ModelViewer(mSurfaceView)
        mSurfaceView.setOnTouchListener(modelViewer)

        //Skybox and background color
        //without this part the scene'll appear broken
        modelViewer.cameraFocalLength = 52f
        modelViewer.scene.skybox = Skybox.Builder().build(modelViewer.engine)
        modelViewer.scene.skybox?.setColor(1.0f, 1.0f, 1.0f, 1.0f) //White color
    }


    fun loadGlb(context: Context, name: String) {
        val buffer = readAsset(context, "models/${name}.glb")
        modelViewer.apply {
            loadModelGlb(buffer)
            transformToUnitCube()
        }
    }

    fun loadGlb(context: Context, dirName: String, name: String) {
        val buffer = readAsset(context, "models/${dirName}/${name}.glb")
        modelViewer.apply {
            loadModelGlb(buffer)
            transformToUnitCube()

        }
    }

    fun loadGltf(context: Context, name: String) {
        val buffer = context.assets.open("models/${name}.gltf").use { input ->
            val bytes = ByteArray(input.available())
            input.read(bytes)
            ByteBuffer.wrap(bytes)
        }
        modelViewer.apply {
            loadModelGltf(buffer) { uri -> readAsset(context, "models/$uri") }
            transformToUnitCube()
        }
    }

    fun loadGltf(context: Context, dirName: String, name: String) {
        val buffer = context.assets.open("models/${dirName}/${name}.gltf").use { input ->
            val bytes = ByteArray(input.available())
            input.read(bytes)
            ByteBuffer.wrap(bytes)
        }
        modelViewer.apply {
            loadModelGltf(buffer) { uri -> readAsset(context, "models/${dirName}/$uri") }
            transformToUnitCube()
        }
    }

    fun loadIndirectLight(context: Context, ibl: String) {
        // Create the indirect light source and add it to the scene.
        val buffer = readAsset(context, "environments/venetian_crossroads_2k/${ibl}_ibl.ktx")
        KTX1Loader.createIndirectLight(modelViewer.engine, buffer).apply {
            intensity = 10_000f
            modelViewer.scene.indirectLight = this
        }
    }

    fun loadEnviroment(context: Context, ibl: String) {
        // Create the sky box and add it to the scene.
        val buffer = readAsset(context, "environments/venetian_crossroads_2k/${ibl}_skybox.ktx")
        KTX1Loader.createSkybox(modelViewer.engine, buffer).apply {
            modelViewer.scene.skybox = this
        }
    }

    private fun readAsset(context: Context, assetName: String): ByteBuffer {
        val input = context.assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    fun getAnimationDuration(animationIndex: Int): Float {
        return modelViewer.animator?.getAnimationDuration(animationIndex) ?: 0.0f
    }

    fun activeAnimation(boolean: Boolean,array: IntArray){
        startAnimationSequence=boolean;
        animationSequence = array
        Log.i("array", animationSequence.joinToString())
    }



    private val frameCallback = object : Choreographer.FrameCallback {
        private var baseTime = System.nanoTime()
        private var crossFadeAlpha = 0.0f
        private val crossFadeTime = 0.5f
        private var previousAnimationIndex = -1
        private var currentAnimationIndex = 0
        private var isTransitioning = false
        private var animationStartTime = baseTime
        private val animationDuration = 1700000000L

        override fun doFrame(currentTime: Long) {
            val seconds = (currentTime - baseTime).toDouble() / 1_000_000_000
            choreographer.postFrameCallback(this)
            modelViewer.animator?.apply {
                val animationElapsedTime = currentTime - animationStartTime

                if (!startAnimationSequence) {
                    applyAnimation(0, (currentTime - animationStartTime).toFloat() / animationDuration)
                    updateBoneMatrices()
                } else {

                    if (!isTransitioning) {
                        applyAnimation(animationSequence[currentAnimationIndex], (currentTime - animationStartTime).toFloat() / animationDuration)
                        updateBoneMatrices()
                    }

                    if (animationElapsedTime >= animationDuration) {
                        previousAnimationIndex = currentAnimationIndex
                        currentAnimationIndex = (currentAnimationIndex + 1) % animationSequence.size
                        crossFadeAlpha = 0.0f
                        isTransitioning = true
                        animationStartTime = currentTime

                        Log.i("Animation", "Nuevo índice de animación: ${animationSequence[currentAnimationIndex]}")
                        Log.i("Animation", "Secuencia de animación actual: ${animationSequence.contentToString()}")
                    }

                    if (isTransitioning) {
                        crossFadeAlpha += seconds.toFloat() / crossFadeTime
                        crossFadeAlpha = crossFadeAlpha.coerceIn(0.0f, 1.0f)

                        applyAnimation(animationSequence[currentAnimationIndex], (currentTime - animationStartTime).toFloat() / animationDuration)
                        applyCrossFade(animationSequence[previousAnimationIndex], (currentTime - animationStartTime).toFloat() / animationDuration, 1.0f - crossFadeAlpha)

                        updateBoneMatrices()

                        if (crossFadeAlpha == 1.0f) {
                            isTransitioning = false
                        }
                    }
                }
            }

            modelViewer.render(currentTime)
        }
    }



    fun onResume() {
        choreographer.postFrameCallback(frameCallback)
    }

    fun onPause() {
        choreographer.removeFrameCallback(frameCallback)
    }

    fun onDestroy() {
        choreographer.removeFrameCallback(frameCallback)
    }
}