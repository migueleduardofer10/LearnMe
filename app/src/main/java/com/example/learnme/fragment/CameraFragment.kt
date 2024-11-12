/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.learnme.fragment

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.camera.core.Preview
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.learnme.R
import com.example.learnme.MainViewModel
import com.example.learnme.TransferLearningHelper
import com.example.learnme.TransferLearningHelper.Companion.CLASS_FOUR
import com.example.learnme.TransferLearningHelper.Companion.CLASS_ONE
import com.example.learnme.TransferLearningHelper.Companion.CLASS_THREE
import com.example.learnme.TransferLearningHelper.Companion.CLASS_TWO
import com.example.learnme.data.AppDatabase
import com.example.learnme.databinding.FragmentCameraBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.label.Category
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(),
    TransferLearningHelper.ClassifierListener {

    companion object {
        private const val TAG = "Model Personalization"
        private const val LONG_PRESS_DURATION = 500
        private const val SAMPLE_COLLECTION_DELAY = 300
    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var transferLearningHelper: TransferLearningHelper
    private lateinit var bitmapBuffer: Bitmap

    private val viewModel: MainViewModel by activityViewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var previousClass: String? = null
    private var currentPlayingClass: String? = null  // Variable para almacenar la clase actualmente en reproducción

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    // When the user presses the "add sample" button for some class,
    // that class will be added to this queue. It is later extracted by
    // InferenceThread and processed.
    private val addSampleRequests = ConcurrentLinkedQueue<String>()


    private var audioUrisLoaded = false // Flag para evitar cargas múltiples
    private var mediaPlayer: MediaPlayer? = null
    private var audioUriMap: MutableMap<String, Uri> = mutableMapOf()


    private var sampleCollectionButtonPressedTime: Long = 0
    private var isCollectingSamples = false
    private val sampleCollectionHandler = Handler(Looper.getMainLooper())
    private val onAddSampleTouchListener =
        View.OnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    isCollectingSamples = true
                    sampleCollectionButtonPressedTime =
                        SystemClock.uptimeMillis()
                    sampleCollectionHandler.post(object : Runnable {
                        override fun run() {
                            val timePressed =
                                SystemClock.uptimeMillis() - sampleCollectionButtonPressedTime
                            view.findViewById<View>(view.id).performClick()
                            if (timePressed < LONG_PRESS_DURATION) {
                                sampleCollectionHandler.postDelayed(
                                    this,
                                    LONG_PRESS_DURATION.toLong()
                                )
                            } else if (isCollectingSamples) {
                                val className: String =
                                    getClassNameFromResourceId(view.id)
                                addSampleRequests.add(className)
                                sampleCollectionHandler.postDelayed(
                                    this,
                                    SAMPLE_COLLECTION_DELAY.toLong()
                                )
                            }
                        }
                    })
                }
                MotionEvent.ACTION_UP -> {
                    sampleCollectionHandler.removeCallbacksAndMessages(null)
                    isCollectingSamples = false
                }
            }
            true
        }

    override fun onResume() {
        super.onResume()

        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(
                requireActivity(),
                R.id.fragment_container
            ).navigate(CameraFragmentDirections.actionCameraToPermissions())
        }

        // Verificar si estamos en modo de inferencia y cargar URIs si es necesario
        if (viewModel.getCaptureMode() == false && !audioUrisLoaded) {
            loadAudioUrisFromDatabase()
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding =
            FragmentCameraBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    private fun loadAudioUrisFromDatabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            val classDao = AppDatabase.getInstance(requireContext()).classDao()
            val classes = classDao.getAllClasses()

            classes.forEach { classEntity ->
                classEntity.audioPath?.let { path ->
                    audioUriMap[classEntity.className] = Uri.parse(path)
                    Log.d("AudioDebug", "Cargado audio para clase ${classEntity.className}: URI=${Uri.parse(path)}")
                }
            }

            // Marcar los URIs como cargados para evitar recargas
            audioUrisLoaded = true
        }
    }
    @SuppressLint("MissingPermission", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializamos el MediaPlayer una sola vez
        initializeMediaPlayer()

        transferLearningHelper = TransferLearningHelper(
            context = requireContext(),
            classifierListener = this
        )

        cameraExecutor = Executors.newSingleThreadExecutor()

        viewModel.numThreads.observe(viewLifecycleOwner) {
            transferLearningHelper.numThreads = it
            transferLearningHelper.close()
            if (viewModel.getTrainingState() != MainViewModel.TrainingState.PREPARE) {
                // If the model is training, continue training with old image
                // sets.
                viewModel.setTrainingState(MainViewModel.TrainingState.TRAINING)
                transferLearningHelper.startTraining()
            }
        }

        viewModel.trainingState.observe(viewLifecycleOwner) {
            updateTrainingButtonState()
        }

        viewModel.captureMode.observe(viewLifecycleOwner) { isCaptureMode ->
            if (isCaptureMode) {
                viewModel.getNumberOfSample()?.let {
                    updateNumberOfSample(it)
                }
                // Unhighlight all class buttons
                highlightResult(null)
            }

            // Update the UI after switch to training mode.
            updateTrainingButtonState()
        }

        viewModel.numberOfSamples.observe(viewLifecycleOwner) {
            // Update the number of samples
            updateNumberOfSample(it)
            updateTrainingButtonState()
        }

        with(fragmentCameraBinding) {
            if (viewModel.getCaptureMode()!!) {
                btnTrainingMode.isChecked = true
            } else {
                btnInferenceMode.isChecked = true
            }
            llClassOne.setOnClickListener {
                addSampleRequests.add(CLASS_ONE)
            }
            llClassTwo.setOnClickListener {
                addSampleRequests.add(CLASS_TWO)
            }
            llClassThree.setOnClickListener {
                addSampleRequests.add(CLASS_THREE)
            }
            llClassFour.setOnClickListener {
                addSampleRequests.add(CLASS_FOUR)
            }
            llClassOne.setOnTouchListener(onAddSampleTouchListener)
            llClassTwo.setOnTouchListener(onAddSampleTouchListener)
            llClassThree.setOnTouchListener(onAddSampleTouchListener)
            llClassFour.setOnTouchListener(onAddSampleTouchListener)
            btnPauseTrain.setOnClickListener {
                viewModel.setTrainingState(MainViewModel.TrainingState.PAUSE)
                transferLearningHelper.pauseTraining()
            }
            btnResumeTrain.setOnClickListener {
                viewModel.setTrainingState(MainViewModel.TrainingState.TRAINING)
                transferLearningHelper.startTraining()
            }
            btnStartTrain.setOnClickListener {
                // Start training process
                viewModel.setTrainingState(MainViewModel.TrainingState.TRAINING)
                transferLearningHelper.startTraining()
            }
            radioButton.setOnCheckedChangeListener { _, checkedId ->
                if (checkedId == R.id.btnTrainingMode) {
                    // Switch to training mode.
                    viewModel.setCaptureMode(true)
                } else {
                    if (viewModel.getTrainingState() == MainViewModel.TrainingState.PREPARE) {
                        fragmentCameraBinding.btnTrainingMode.isChecked = true
                        fragmentCameraBinding.btnInferenceMode.isChecked = false

                        Toast.makeText(
                            requireContext(), "Inference can only " +
                                    "start after training is done.", Toast
                                .LENGTH_LONG
                        ).show()
                    } else {
                        // Pause the training process and switch to inference mode.
                        transferLearningHelper.pauseTraining()
                        viewModel.setTrainingState(MainViewModel.TrainingState.PAUSE)
                        viewModel.setCaptureMode(false)
                    }
                }
            }

            viewFinder.post {
                // Set up the camera and its use cases
                setUpCamera()
            }
        }
    }


    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding.viewFinder.display.rotation
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider =
            cameraProvider
                ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector - makes assumption that we're only using the back camera
        val cameraSelector =
            CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!::bitmapBuffer.isInitialized) {
                            // The image rotation and RGB image buffer are initialized only once
                            // the analyzer has started running
                            bitmapBuffer = Bitmap.createBitmap(
                                image.width,
                                image.height,
                                Bitmap.Config.ARGB_8888
                            )
                        }

                        val sampleClass = addSampleRequests.poll()
                        if (sampleClass != null) {
                            addSample(image, sampleClass)
                            viewModel.increaseNumberOfSample(sampleClass)
                        } else {
                            if (viewModel.getCaptureMode() == false) {
                                classifyImage(image)
                            }
                        }
                        image.close()
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun classifyImage(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the transfer learning helper for
        // processing and classification.
        transferLearningHelper.classify(bitmapBuffer, imageRotation)
    }

    private fun addSample(image: ImageProxy, className: String) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the transfer learning helper for
        // processing and prepare training data.
        transferLearningHelper.addSample(bitmapBuffer, className, imageRotation)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResults(
        results: List<Category>?,
        inferenceTime: Long
    ) {
        activity?.runOnUiThread {
            // Update the result in inference mode.
            if (viewModel.getCaptureMode() == false) {
                // Show result
                results?.let { list ->
                    // Highlight the class which is highest score.
                    list.maxByOrNull { it.score }?.let {
                        highlightResult(it.label)
                    }
                    updateScoreClasses(list)
                }

                fragmentCameraBinding.tvInferenceTime.text =
                    String.format("%d ms", inferenceTime)
            }
        }
    }

    private fun initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setOnPreparedListener {
                Log.d("MediaPlayer", "MediaPlayer preparado, iniciando reproducción")
                it.start()
            }
            mediaPlayer?.setOnCompletionListener {
                Log.d("MediaPlayer", "Reproducción completada")
                it.reset()
            }
        }
    }

    private fun playClassAudio(className: String) {
        if (className != currentPlayingClass) {
            mediaPlayer?.apply {
                if (isPlaying) {
                    Log.d("playClassAudio", "Deteniendo el audio actual y reseteando.")
                    stop()
                    reset()
                }
            }

            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
            }

            val audioUri = audioUriMap[className]
            if (audioUri != null) {
                try {
                    mediaPlayer?.setDataSource(requireContext(), audioUri)

                    mediaPlayer?.setOnPreparedListener {
                        Log.d("playClassAudio", "Nuevo audio preparado, iniciando reproducción.")
                        it.start()
                    }

                    mediaPlayer?.setOnCompletionListener {
                        Log.d("playClassAudio", "Reproducción completada para la clase $className.")
                        it.reset()
                    }

                    mediaPlayer?.prepareAsync()

                    currentPlayingClass = className

                } catch (e: IOException) {
                    Log.e("CameraFragment", "Error al preparar el audio: ${e.message}")
                } catch (e: IllegalStateException) {
                    Log.e("CameraFragment", "Error de estado ilegal en el MediaPlayer: ${e.message}")
                }
            } else {
                Log.e("CameraFragment", "No se encontró URI para la clase $className")
            }
        } else {
            Log.d("playClassAudio", "La clase $className ya está en reproducción, manteniendo el audio actual.")
        }
    }

    // Show the loss number after each training.
    override fun onLossResults(lossNumber: Float) {
        String.format(
            Locale.US,
            "Loss: %.3f", lossNumber
        ).let {
            fragmentCameraBinding.tvLossConsumerPause.text = it
            fragmentCameraBinding.tvLossConsumerResume.text = it
        }
    }

    // Show the accurate score of each class.
    private fun updateScoreClasses(categories: List<Category>) {
        categories.forEach {
            val view = getClassButtonScore(it.label)
            (view as? TextView)?.text = String.format(
                Locale.US, "%.1f", it.score
            )
        }
    }

    // Get the class button which represent for the label
    private fun getClassButton(label: String): View? {
        return when (label) {
            CLASS_ONE -> fragmentCameraBinding.llClassOne
            CLASS_TWO -> fragmentCameraBinding.llClassTwo
            CLASS_THREE -> fragmentCameraBinding.llClassThree
            CLASS_FOUR -> fragmentCameraBinding.llClassFour
            else -> null
        }
    }

    // Get the class button score which represent for the label
    private fun getClassButtonScore(label: String): View? {
        return when (label) {
            CLASS_ONE -> fragmentCameraBinding.tvNumberClassOne
            CLASS_TWO -> fragmentCameraBinding.tvNumberClassTwo
            CLASS_THREE -> fragmentCameraBinding.tvNumberClassThree
            CLASS_FOUR -> fragmentCameraBinding.tvNumberClassFour
            else -> null
        }
    }

    // Get the class name from resource id
    private fun getClassNameFromResourceId(id: Int): String {
        return when (id) {
            fragmentCameraBinding.llClassOne.id -> CLASS_ONE
            fragmentCameraBinding.llClassTwo.id -> CLASS_TWO
            fragmentCameraBinding.llClassThree.id -> CLASS_THREE
            fragmentCameraBinding.llClassFour.id -> CLASS_FOUR
            else -> {
                ""
            }
        }
    }

    // Highlight the current label and unhighlight the previous label
    private fun highlightResult(label: String?) {
        // skip the previous position if it is no position.
        previousClass?.let {
            setClassButtonHighlight(getClassButton(it), false)
        }
        if (label != null) {
            setClassButtonHighlight(getClassButton(label), true)
        }
        previousClass = label
    }

    private fun setClassButtonHighlight(view: View?, isHighlight: Boolean) {
        view?.run {
            background = AppCompatResources.getDrawable(
                context,
                if (isHighlight) R.drawable.btn_default_highlight else R.drawable.btn_default
            )
        }
    }

    // Update the number of samples. If there are no label in the samples,
    // set it 0.
    private fun updateNumberOfSample(numberOfSamples: Map<String, Int>) {
        fragmentCameraBinding.tvNumberClassOne.text = if (numberOfSamples
                .containsKey(CLASS_ONE)
        ) numberOfSamples.getValue(CLASS_ONE)
            .toString()
        else "0"
        fragmentCameraBinding.tvNumberClassTwo.text = if (numberOfSamples
                .containsKey(CLASS_TWO)
        ) numberOfSamples.getValue(CLASS_TWO)
            .toString()
        else "0"
        fragmentCameraBinding.tvNumberClassThree.text = if (numberOfSamples
                .containsKey(CLASS_THREE)
        ) numberOfSamples.getValue(CLASS_THREE)
            .toString()
        else "0"
        fragmentCameraBinding.tvNumberClassFour.text = if (numberOfSamples
                .containsKey(CLASS_FOUR)
        ) numberOfSamples.getValue(CLASS_FOUR)
            .toString()
        else "0"
    }

    private fun updateTrainingButtonState() {
        with(fragmentCameraBinding) {
            tvInferenceTime.visibility = if (viewModel
                    .getCaptureMode() == true
            ) View.GONE else View.VISIBLE

            btnCollectSample.visibility = if (
                viewModel.getTrainingState() == MainViewModel.TrainingState.PREPARE &&
                (viewModel.getNumberOfSample()?.size ?: 0) == 0 && viewModel
                    .getCaptureMode() == true
            ) View.VISIBLE else View.GONE

            btnStartTrain.visibility = if (
                viewModel.getTrainingState() == MainViewModel.TrainingState.PREPARE &&
                (viewModel.getNumberOfSample()?.size ?: 0) > 0 && viewModel
                    .getCaptureMode() == true
            ) View.VISIBLE else View.GONE

            btnPauseTrain.visibility =
                if (viewModel.getTrainingState() == MainViewModel
                        .TrainingState.TRAINING && viewModel
                        .getCaptureMode() == true
                ) View.VISIBLE else View.GONE

            btnResumeTrain.visibility =
                if (viewModel.getTrainingState() == MainViewModel
                        .TrainingState.PAUSE && viewModel
                        .getCaptureMode() == true
                ) View.VISIBLE else View.GONE

            // Disable adding button when it is training or in inference mode.
            (viewModel.getCaptureMode() == true && viewModel.getTrainingState() !=
                    MainViewModel.TrainingState.TRAINING).let { enable ->
                llClassOne.isClickable = enable
                llClassTwo.isClickable = enable
                llClassThree.isClickable = enable
                llClassFour.isClickable = enable
            }
        }
    }
}
