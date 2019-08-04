package com.home.kotlinfirebasemlkitqrrealtimedemo

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.otaliastudios.cameraview.frame.Frame
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var isDetected = false // internal: 相同模塊內可見
    lateinit var options: FirebaseVisionBarcodeDetectorOptions
    lateinit var detector: FirebaseVisionBarcodeDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getPermissions()
    }

    private fun getPermissions() {
        Dexter.withActivity(this)
            .withPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    setupCamera()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                }
            }).check()
    }

    private fun setupCamera() {
        options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
            .build()
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)
        againButton.isEnabled = isDetected
        againButton.setOnClickListener {
            isDetected = !isDetected
            againButton.isEnabled = isDetected
        }
        cameraView.setLifecycleOwner(this)
        cameraView.addFrameProcessor {
            processImage(getVisionImageFromFrame(it))
        }
    }

    private fun processImage(firebaseVisionImage: FirebaseVisionImage) {
        if (!isDetected)
            detector.detectInImage(firebaseVisionImage)
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                }
                .addOnSuccessListener {
                    processResult(it)
                }
    }

    private fun processResult(barcodeList: List<FirebaseVisionBarcode>) {
        if (barcodeList.isNotEmpty()) {
            isDetected = true
            againButton.isEnabled = isDetected
            for (barcode in barcodeList) {
                when (barcode.valueType) {
                    FirebaseVisionBarcode.TYPE_TEXT -> {
                        createDialog(barcode.rawValue)
                    }
                    FirebaseVisionBarcode.TYPE_CONTACT_INFO -> {
                        val info = StringBuilder("Name: ")
                            .append(barcode.contactInfo!!.name!!.formattedName)
                            .append("\n")
                            .append("Address: ")
                            .append(barcode.contactInfo!!.addresses[0].addressLines[0])
                            .append("\n")
                            .append("Email: ")
                            .append(barcode.contactInfo!!.emails[0].address)
                            .toString()
                        createDialog(info)
                    }
                    FirebaseVisionBarcode.TYPE_URL -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(barcode.rawValue))
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun createDialog(rawValue: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(rawValue)
            .setPositiveButton("OK") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

    private fun getVisionImageFromFrame(frame: Frame): FirebaseVisionImage {
        val data = frame.data
        val metadata = FirebaseVisionImageMetadata.Builder()
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
            .setHeight(frame.size.height)
            .setWidth(frame.size.width)
            .build()
        return FirebaseVisionImage.fromByteArray(data, metadata)
    }
}
