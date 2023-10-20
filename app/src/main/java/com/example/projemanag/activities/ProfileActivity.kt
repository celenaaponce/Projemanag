package com.example.projemanag.activities

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.projemanag.R
import com.example.projemanag.firebase.FirestoreClass
import com.example.projemanag.models.User
import com.example.projemanag.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import de.hdodenhof.circleimageview.CircleImageView
import java.io.IOException

class ProfileActivity : BaseActivity() {

    private var mSelectedImageFileUri: Uri? = null
    private lateinit var mUserDetails: User
    private var mProfileImageURL : String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setupActionBar()

        FirestoreClass().loadUserData(this)
        val navImage : CircleImageView = findViewById(R.id.ivUserImage)
        navImage.setOnClickListener {
            choosePhotoFromGallery()
        }

        var btnUpdate : Button = findViewById(R.id.btnUpdate)
        btnUpdate.setOnClickListener {
            if(mSelectedImageFileUri != null){
                uploadUserImage()
            }else{
                showProgressDialog("Please wait")
                updateUserProfile()
            }
        }
    }

    private fun setupActionBar(){
        val toolbarMainActivity : Toolbar = findViewById(R.id.toolbar_my_profile)
        setSupportActionBar(toolbarMainActivity)
        val actionBar = supportActionBar
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
            actionBar.title = "My Profile"
        }
        toolbarMainActivity.setNavigationOnClickListener {
            onBackPressed()
        }

    }

    private fun updateUserProfile(){
        val etName: EditText = findViewById(R.id.etNameProfile)
        val etMobile: EditText = findViewById(R.id.etMobileProfile)
        var userHashMap = HashMap<String, Any>()
        var anyChanges = false
        if(mProfileImageURL.isNotEmpty() && mProfileImageURL != mUserDetails.image){
            userHashMap[Constants.IMAGE] = mProfileImageURL
            anyChanges = true
        }
        if(etName.text.toString() != mUserDetails.name){
            userHashMap[Constants.NAME] = etName.text.toString()
            anyChanges = true
        }
        if(etMobile.text.toString() != mUserDetails.mobile.toString()){
            userHashMap[Constants.MOBILE] = etMobile.text.toString().toLong()
            anyChanges = true
        }
        if(anyChanges){
            FirestoreClass().updateUserData(this, userHashMap)
        }
        hideProgressDialog()
    }

    fun setUserDataInUI(user: User){
        mUserDetails = user
        val navImage : CircleImageView = findViewById(R.id.ivUserImage)
        val etName : EditText = findViewById(R.id.etNameProfile)
        val etEmail : EditText = findViewById(R.id.etEmailProfile)
        Glide
            .with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(navImage)
        etName.setText(user.name)
        etEmail.setText(user.email)
        if(user.mobile != 0L){
            val etMobile : EditText = findViewById(R.id.etMobileProfile)
            etMobile.setText(user.mobile.toString())

        }
    }

    private fun choosePhotoFromGallery() {
        Dexter.withActivity(this)
            .withPermissions(
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()) {
                        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        resultLauncher.launch(galleryIntent)
                    } else showRationalDialogForPermissions()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<com.karumi.dexter.listener.PermissionRequest>,
                    token: PermissionToken
                ) {
                    showRationalDialogForPermissions()
                    token.continuePermissionRequest()
                }
            }).withErrorListener {

                // on below line method will be called when dexter
                // throws any error while requesting permissions.
                Toast.makeText(this, it.name, Toast.LENGTH_SHORT).show()
            }.onSameThread().check()
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        val navImage : CircleImageView = findViewById(R.id.ivUserImage)
        if(result.resultCode == Activity.RESULT_OK){
            val data: Intent? = result.data
            mSelectedImageFileUri = data?.data

            try{
                Glide
                    .with(this@ProfileActivity)
                    .load(mSelectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(navImage!!)
            }catch(e: IOException){
                e.printStackTrace()
            }
        }
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this).setMessage("It looks like you turned off permissions required.")
            .setPositiveButton("GO TO SETTINGS")
            { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }.show()
    }

    private fun uploadUserImage(){
        showProgressDialog("Please wait")
        if(mSelectedImageFileUri != null){
            val sRef : StorageReference = FirebaseStorage.getInstance().reference.child(
                "USER_IMAGE" + System.currentTimeMillis()
                        + "." + getFileExtension(mSelectedImageFileUri))
            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                taskSnapshot ->
                Log.i("Firebase Image URL", taskSnapshot.metadata!!.reference!!.downloadUrl.toString())
                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                    uri -> Log.i("Downloadable Image URI", uri.toString())
                    mProfileImageURL = uri.toString()

                    updateUserProfile()
                    hideProgressDialog()
                }
            }.addOnFailureListener{
                exception -> Toast.makeText(this@ProfileActivity, exception.message, Toast.LENGTH_LONG).show()

                hideProgressDialog()
            }

        }
    }

    private fun getFileExtension(uri: Uri?): String?{
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri!!))
    }

    fun profileUpdateSuccess(){
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object{
        private const val GALLERY = 1
    }

}