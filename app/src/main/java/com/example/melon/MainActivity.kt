package com.example.melon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import org.opencv.android.NativeCameraView.TAG
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File


class MainActivity : AppCompatActivity() {

//    inisiasi variabel
    private lateinit var imageView: ImageView
    private lateinit var cameraButton: Button
    private lateinit var galleryButton: Button
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>

    private var imageUri: Uri? = null

    //        fungsi grayscale
    fun preprocessImage(input:Mat): Mat{
        val gray = Mat()
        Imgproc.cvtColor(input, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.equalizeHist(gray, gray)
        return gray
    }

    //        fungsi untuk menampilkan gambar hasil preprocessing
    fun displaypreprocessBitmap(bitmap: Bitmap){
        imageView.setImageBitmap(bitmap) 
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//      memanggil opencv
        if (OpenCVLoader.initLocal()){
            Log.i(TAG, "OpenCV loaded successfully")
            Toast.makeText(this, "OpenCV berhasil", Toast.LENGTH_SHORT).show()
        } else{
            Log.e(TAG, "OpenCV initializazion failed")
            Toast.makeText(this, "OpenCV gagal", Toast.LENGTH_SHORT).show()
        }

//      button
        imageView = findViewById(R.id.imageView)
        cameraButton = findViewById(R.id.btnTakePhoto)
        galleryButton = findViewById(R.id.btnChoosePhoto)

//        konfigurasi untuk UCrop
        val uCropOption = UCrop.Options().apply {
            setCircleDimmedLayer(true)
//            setShowCropGrid(true)
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()){
            success ->
                if (success){
                    //mengambil gambar dari URI
                    val originalBitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)

                    //mengonversi bitmap ke mat (opencv)
                    val originalMat = Mat(originalBitmap.height, originalBitmap.width, CvType.CV_8UC4)
                    Utils.bitmapToMat(originalBitmap, originalMat)

                    //memproses gambar
                    val processedMat = preprocessImage(originalMat)

                    //konversi map kembali ke bitmap
                    val processedBitmap = Bitmap.createBitmap(processedMat.cols(), processedMat.rows(), Bitmap.Config.ARGB_8888)
                    Utils.matToBitmap(processedMat,processedBitmap)

//                    tampilkan hasil preprocessing
                    displaypreprocessBitmap(processedBitmap)

//                    val options = UCrop.Options()
//                    options.setCircleDimmedLayer(true)

                    UCrop.of(imageUri!!, Uri.fromFile(File(cacheDir, "cropped_image.jpg")))
                        .withAspectRatio(1f,1f)
                        .withOptions(uCropOption)
                        .start(this)

//                    imageView.setImageBitmap(processedBitmap)
                }else{
                    Toast.makeText(this, "Capture Failed", Toast.LENGTH_SHORT).show()
                }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){
                uri: Uri? ->
            if (uri!= null){
//                val options = UCrop.Options()
//                options.setCircleDimmedLayer(true)
//                options.setCompressionQuality(70)

                UCrop.of(uri, Uri.fromFile(File(cacheDir, "cropped_image.jpg")))
                    .withAspectRatio(1f,1f)
                    .withOptions(uCropOption)
                    .start(this)
                //menampilkan hasil
//                imageView.setImageURI(uri)
            }
        }

        cameraButton.setOnClickListener {
            val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            if (permission == PackageManager.PERMISSION_GRANTED){
                // membuat direktori cache jika belom ada
                val cacheDir = File(cacheDir, "images")
                if (!cacheDir.exists()){
                    cacheDir.mkdir()
                }
                // membuat file untuk menyimpan gambar
                val imageFile = File(cacheDir, "image${System.currentTimeMillis()}.jpg")
//                val imageFile = File.createTempFile("image", ".jpg", cacheDir)
                imageUri = FileProvider.getUriForFile(this, "com.example.melon.provider", imageFile)
                cameraLauncher.launch(imageUri)
            }else{
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
            }
        }

        galleryButton.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

//    @Deprecated
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            imageView.setImageURI(resultUri)

//            ambil gambar setelah proses cropping
            val croppedBitmap = MediaStore.Images.Media.getBitmap(contentResolver, resultUri)
            val croppedMat = Mat(croppedBitmap.height, croppedBitmap.width, CvType.CV_8UC4)
            Utils.bitmapToMat(croppedBitmap,croppedMat)

//            proses gambar setelah cropping
            val processedCroppedMat = preprocessImage(croppedMat)
            val processedCroppedBitmap = Bitmap.createBitmap(processedCroppedMat.cols(), processedCroppedMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(processedCroppedMat, processedCroppedBitmap)

            displaypreprocessBitmap(processedCroppedBitmap)
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            Toast.makeText(this, "Crop Error: ${cropError?.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                cameraButton.performClick()
            }else{
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
