package com.example.melon

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.theartofdev.edmodo.cropper.CropImage
//import com.theartofdev.edmodo.cropper.CropImageOptions
import com.theartofdev.edmodo.cropper.CropImageView

class CropImageContract : ActivityResultContract<Uri, CropImage.ActivityResult>() {
    override fun createIntent(context: Context, input: Uri): Intent {
        return CropImage.activity(input)
            .setGuidelines(CropImageView.Guidelines.ON)
            .setAspectRatio(1,1)
            .setCropShape(CropImageView.CropShape.OVAL)
            .getIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): CropImage.ActivityResult {
        return CropImage.getActivityResult(intent)
    }
}
