package com.example.project9
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.camera.view.PreviewView
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: PreviewView
    private lateinit var camera: Camera
    private lateinit var imageCapture: ImageCapture
    private lateinit var captureButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewFinder = view.findViewById(R.id.viewFinder)
        captureButton = view.findViewById(R.id.captureButton)
        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        captureButton.setOnClickListener {
            takePhoto()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(requireContext(), "Failed to start camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val data = buffer.toByteArray()
                    val rotationDegrees = image.imageInfo.rotationDegrees
                    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                    val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees)
                    val fileName = "captured_image_${System.currentTimeMillis()}.jpg"

                    FirebaseUtils.uploadImage(rotatedBitmap, "images", fileName)
                        .thenAccept { downloadUrl ->
                            //NOPE
                        }
                        .exceptionally { exception ->
                            Toast.makeText(requireContext(), "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                            null
                        }

                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                }
            }
        )
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }
}