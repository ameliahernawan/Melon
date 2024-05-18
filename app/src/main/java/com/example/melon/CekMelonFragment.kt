package com.example.melon

//import androidx.activity.result.ActivityResultLauncher
//import androidx.core.content.ContextCompat
//import androidx.core.content.FileProvider
//import java.util.jar.Pack200.Packer
//import org.opencv.core.MatOfInt
import android.Manifest
import android.app.Activity
import android.content.ContentValues
//import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.lifecycleScope
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.example.melon.data.api.ApiConfig
//import com.example.melon.data.api.FileUploadResponse
import com.example.melon.data.api.FileViewModel
import com.example.melon.databinding.FragmentCekMelonBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
//import com.google.gson.Gson
import com.theartofdev.edmodo.cropper.CropImage
//import kotlinx.coroutines.launch
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.MultipartBody
//import okhttp3.RequestBody.Companion.asRequestBody
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.opencv.android.NativeCameraView.TAG
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
//import retrofit2.HttpException
//import java.io.InputStream


@Suppress("DEPRECATION")
class  CekMelonFragment : Fragment() {
    private lateinit var binding: FragmentCekMelonBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var viewModel: FileViewModel
    private var imageUri: Uri? = null
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationSettingsRequest: LocationSettingsRequest
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCekMelonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view:View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        checkAndRequestPermissions()
        binding.btnCamera.setOnClickListener {
            startCamera()
        }
        binding.btnGallery.setOnClickListener {
            startGallery()
        }
        binding.btnUpload.setOnClickListener {uploadImage()}

//        memanggil opencv
        if (OpenCVLoader.initDebug()){ Log.i(TAG, "OpenCV loaded successfully") }

        initLocationProviderClient()

        viewModel = ViewModelProvider(this)[FileViewModel::class.java]
        viewModel.uploadResult.observe(viewLifecycleOwner) { result ->
            showLoading(false)
            handleUploadResult(result)
        }
    }
    private fun uploadImage() {
        imageUri?.let {uri ->
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            val resizedBitmap = getResizedBitmapCV(originalBitmap)
            val resizedImageFile = bitmapToFile(resizedBitmap, requireContext())
            Log.d("Image File", "showImage: ${resizedImageFile.path}")

            showLoading(true)

            if (currentLatitude == null || currentLongitude == null){
                getUserLocation{
                    viewModel.uploadImage(resizedImageFile, currentLatitude ?: 0.0, currentLongitude?:0.0)
                }
            } else{
                viewModel.uploadImage(resizedImageFile, currentLatitude ?: 0.0, currentLongitude ?: 0.0)
            }
        } ?: showToast(getString(R.string.empty_image_warning))
    }

    private fun handleUploadResult(success: Boolean) {
        if (success) {
            showToast("Image uploaded successfully")
        } else {
            showToast("Failed to upload image")
        }
    }

    private fun initLocationProviderClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000 // Interval untuk meminta pembaruan lokasi (dalam milidetik)
            fastestInterval = 5000 // Interval tercepat untuk menerima pembaruan lokasi (dalam milidetik)
        }

        locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()
        LocationServices.getSettingsClient(requireContext()).checkLocationSettings(locationSettingsRequest)
    }

    private fun getUserLocation(callback: () -> Unit) {
        if(ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),1)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if(location != null){
                currentLatitude = location.latitude
                currentLongitude = location.longitude
                callback()
            } else{
                showToast("Unable to retrieve location")
            }
        }.addOnFailureListener{
            showToast("Failed to retrieve location")
            showLoading(false)
        }
    }
    private fun getResizedBitmapCV(inputBitmap: Bitmap?): Bitmap? {
        inputBitmap?.let{
            val inputMat = Mat()
            Utils.bitmapToMat(inputBitmap, inputMat)

            val gray = Mat()
            Imgproc.cvtColor(inputMat, gray, Imgproc.COLOR_BGR2GRAY)
            Imgproc.equalizeHist(gray, gray)

            val targetWidth = 416
            val targetHeight = 416
            // Create a new Mat for the resized image
            val resizedMat = Mat()
            Imgproc.resize(inputMat, resizedMat, Size(targetWidth.toDouble(), targetHeight.toDouble()))

            // Convert the resized Mat back to a Bitmap
            val resizedBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(resizedMat, resizedBitmap)
            inputMat.release()
            resizedMat.release()
            return resizedBitmap
        }
        return null
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    //konfigurasi crop image
    private fun cropImage(uri: Uri?){
        cropImage(uri, requireContext())
    }

    private fun startCamera(){
        val permission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED){
            imageUri = getImageUri(requireContext())
            cameraLauncher.launch(imageUri)
        } else{
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()){
            isSuccess ->
        if (isSuccess){
            cropImage(imageUri)
        } else{
            Toast.makeText(requireContext(), "Capture Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startGallery(){
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){
            uri: Uri? ->
        if (uri != null){
            imageUri = uri
            cropImage(imageUri)
        }else{
            Log.d("Photo picker", "No media selected")
        }
    }

    private fun displayResult(uri: Uri) {
        binding.imageView.setImageURI(uri)
        binding.textResult.text= getString(R.string.hasil_deteksi)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            val result = CropImage.getActivityResult(data)
            val croppedUri = result.uri
            croppedUri?.let {
                displayResult(it)
                saveCroppedImageToGallery(it)
            }

        } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
            val cropError = CropImage.getActivityResult(data)?.error
            Toast.makeText(requireContext(), "Crop Error: ${cropError?.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCroppedImageToGallery(uri: Uri) {
        val resolver = requireContext().contentResolver
        val contentValues = ContentValues().apply{
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$timeStamp.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MyCamera/")
        }
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        imageUri?. let {
            resolver.openOutputStream(it).use { outputStream ->
                val inputStream = uri.let { it1 -> resolver.openInputStream(it1) }
                inputStream?.copyTo(outputStream!!)
                inputStream?.close()
                outputStream?.close()
            }
            showToast("Image saved to gallery")
        } ?: showToast("Failed to save image")
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), permissionsToRequest.toTypedArray(), REQUEST_CODE)
        }
    }
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1){
            if(grantResults.isNotEmpty()&& grantResults[0] == PackageManager.PERMISSION_GRANTED){
                permissions.forEachIndexed { index, permission ->
                    if (grantResults[index] == PackageManager.PERMISSION_GRANTED){
                        when (permission){
                            Manifest.permission.CAMERA -> binding.btnCamera.performClick()
                            Manifest.permission.ACCESS_FINE_LOCATION -> getUserLocation{}
                        }
                    }
                }
            } else{
                Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val TAG = "CekMelonFragment"
        private const val REQUEST_CODE = 1001
    }
}