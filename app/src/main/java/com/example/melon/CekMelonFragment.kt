package com.example.melon


import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import java.io.IOException
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.melon.data.api.FileViewModel
import com.example.melon.databinding.FragmentCekMelonBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import android.graphics.Bitmap
import android.location.Location


@Suppress("DEPRECATION")
class  CekMelonFragment : Fragment() {
    private lateinit var binding: FragmentCekMelonBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var viewModel: FileViewModel
    private var imageUri: Uri? = null
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationSettingsRequest: LocationSettingsRequest
    private var capturedLatitude: Double? = null
    private var capturedLongitude: Double? = null
    private var isPhotoFromCamera: Boolean = false

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
        binding.btnCapture.setOnClickListener { showPictureDialog() }
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

    private fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(requireContext())
        pictureDialog.setTitle("Select Action")

        val pictureDialogItems = arrayOf(
            "Pilih dari Galeri",
            "Ambil dari Kamera")
        pictureDialog.setItems(pictureDialogItems
        ) { _, which ->
            when (which) {
                0 -> startGallery()
                1 -> startCamera()
            }
        }
        pictureDialog.show()
    }

    private fun uploadImage() {
        imageUri?.let { uri ->
            val rotateBitmap = handleImageOrientation(uri)
            val resizedBitmap = preprocessImage(rotateBitmap)
            val resizedImageFile = bitmapToFile(resizedBitmap, requireContext())
            Log.d("Image File", "showImage: ${resizedImageFile.path}")

            showLoading(true)

            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val exifInterface = inputStream?.let { ExifInterface(it) }
                val latLong = exifInterface?.latLong
                if (latLong != null) {
                    val latitude = latLong[0]
                    val longitude = latLong[1]
                    Log.d("EXIF Data", "Latitude: $latitude, Longitude: $longitude")
                    viewModel.uploadImage(resizedImageFile, latitude, longitude)
                } else {
                    Log.d("EXIF Data", "latLong is null")
                    showToast("Location not available in image")
                    showLoading(false)
                }
            } catch (e: IOException) {
                Log.e("EXIF Error", "Failed to read EXIF data", e)
                showToast("Failed to read EXIF data")
                showLoading(false)
            }
        } ?: showToast(getString(R.string.empty_image_warning))
    }

    private fun handleImageOrientation(uri: Uri): Bitmap? {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        val exifInterface = ExifInterface(requireContext().contentResolver.openInputStream(uri)!!)
        val rotatedBitmap = when (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(originalBitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(originalBitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(originalBitmap, 270f)
            else -> originalBitmap
        }
        return rotatedBitmap
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
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
            interval = 10000
            fastestInterval = 5000
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(requireContext())
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            // All location settings are satisfied
        }.addOnFailureListener {
            // Location settings are not satisfied
        }
    }
    private fun getUserLocation(callback: (Location?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            callback(location) // Panggil callback dengan lokasi yang diperoleh
        }.addOnFailureListener {
            showToast("Failed to retrieve location")
            callback(null) // Panggil callback dengan nilai null jika gagal mendapatkan lokasi
        }
    }
    private fun preprocessImage(inputBitmap: Bitmap?): Bitmap? {
        inputBitmap?.let{
            val inputMat = Mat()
            Utils.bitmapToMat(inputBitmap, inputMat)

            val gray = Mat()
            Imgproc.cvtColor(inputMat, gray, Imgproc.COLOR_BGR2GRAY)

            // Apply CLAHE (Contrast Limited Adaptive Histogram Equalization)
            val clahe = Imgproc.createCLAHE()
            clahe.apply(gray, gray)

            val targetSize = Size(416.0, 416.0)
            val resizedMat = Mat()
            Imgproc.resize(gray, resizedMat, targetSize)

            // Convert the resized Mat back to a Bitmap
            val outputBitmap = Bitmap.createBitmap(resizedMat.cols(), resizedMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(resizedMat, outputBitmap)

            inputMat.release()
            gray.release()
            resizedMat.release()

            return outputBitmap
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
    private val cropLauncher = registerForActivityResult(CropImageContract()){ result ->
        if(result.isSuccessful){
            val resultUri = result.uri
            displayResult(resultUri)
        } else {
            val error = result.error
            Toast.makeText(requireContext(), "Crop failed: ${error?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cropImage(uri: Uri){
        cropLauncher.launch(uri)
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            imageUri?.let { uri ->
                cropImage(uri)
            }
        } else {
            Toast.makeText(requireContext(), "Capture Failed", Toast.LENGTH_SHORT).show()
        }
    }
    private fun startCamera(){
        val permission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED){
            getUserLocation {location ->
                location?.let{
                    //ambil lokasi longlat
                    capturedLatitude = it.latitude
                    capturedLongitude = it.longitude

                    isPhotoFromCamera = true

                    //mulai kamera
                    imageUri = getImageUri(requireContext())
                    cameraLauncher.launch(imageUri)
                }
            }
        } else{
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
        }
    }

    private fun startGallery(){
        val permission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, GALLERY_REQUEST_CODE)
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
        }
    }

    private fun displayResult(uri: Uri) {
        binding.imageView.setImageURI(uri)
        binding.textResult.text = getString(R.string.hasil_deteksi)

        if (isPhotoFromCamera) {
            val location = Location("").apply {
                latitude = capturedLatitude ?: 0.0
                longitude = capturedLongitude ?: 0.0
            }

            val newUri = saveCroppedImageToGallery(uri, location)
            if (newUri != null) {
                imageUri = newUri
            } else {
                showToast("Failed to save cropped image")
            }
        } else {
            imageUri = uri
        }
    }

    private fun saveCroppedImageToGallery(uri: Uri, location: Location): Uri? {
        if (isPhotoFromCamera) {
            val resolver = requireContext().contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MyCamera")
            }
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            imageUri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    resolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                try {
                    resolver.openFileDescriptor(it, "rw")?.use { pfd ->
                        val exif = ExifInterface(pfd.fileDescriptor)
                        exif.setLatLong(location.latitude, location.longitude)
                        exif.saveAttributes()
                    }
                    showToast("Image saved successfully with location")
                    return imageUri // Return the new URI
                } catch (e: IOException) {
                    showToast("Failed to save location data to image")
                }
            }
        }
        return null // Return null if saving fails
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
                            Manifest.permission.CAMERA -> startCamera()
                            Manifest.permission.ACCESS_FINE_LOCATION -> getUserLocation{}
                            Manifest.permission.READ_EXTERNAL_STORAGE -> startGallery()
                        }
                    }
                }
            } else{
                Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            if (selectedImageUri != null) {
                imageUri = selectedImageUri
            } else {
                Log.d("Photo picker", "No media selected")
            }
        }
    }

    companion object {
        private const val TAG = "CekMelonFragment"
        private const val GALLERY_REQUEST_CODE = 1001
    }
}