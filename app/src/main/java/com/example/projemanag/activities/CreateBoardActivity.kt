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
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.projemanag.R
import com.example.projemanag.firebase.FirestoreClass
import com.example.projemanag.models.Board
import com.example.projemanag.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import de.hdodenhof.circleimageview.CircleImageView
import java.io.IOException

class CreateBoardActivity : BaseActivity() {
    private var mSelectedImageFileUriBoard: Uri? = null
    private var mBoardImageURL: String = ""
    private lateinit var mUserName: String
    private lateinit var mBoardDetails: Board
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_board)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val toolbarCreateBoard : Toolbar = findViewById(R.id.toolbar_create_board)
        setSupportActionBar(toolbarCreateBoard)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "CREATE BOARD"

        toolbarCreateBoard?.setNavigationOnClickListener {
            onBackPressed()
        }
        if(intent.hasExtra(Constants.NAME)){
            mUserName = intent.getStringExtra(Constants.NAME)!!
        }
        val boardImage : CircleImageView = findViewById(R.id.ivBoardImage)
        boardImage.setOnClickListener {
            choosePhotoFromGallery()
        }

        val btnCreate : Button = findViewById(R.id.btnCreate)
        btnCreate.setOnClickListener {
            if(mSelectedImageFileUriBoard != null){
                uploadBoardImage()
            }else{
                showProgressDialog("Please Wait")
                createBoard()
            }
        }
    }

    private fun createBoard(){
        val assignedUsersArrayList: ArrayList<String> = ArrayList()
        assignedUsersArrayList.add(getCurrentUserID())
        var etBoardName : EditText = findViewById(R.id.etBoardName)
        var board = Board(
            etBoardName.text.toString(),
            mBoardImageURL,
            mUserName,
            assignedUsersArrayList
        )
        FirestoreClass().createBoard(this, board)
    }

    private fun uploadBoardImage(){
        showProgressDialog("Please wait")
        if(mSelectedImageFileUriBoard != null){
            val sRef : StorageReference = FirebaseStorage.getInstance().reference.child(
                "BOARD_IMAGE" + System.currentTimeMillis()
                        + "." + getFileExtension(mSelectedImageFileUriBoard))
            sRef.putFile(mSelectedImageFileUriBoard!!).addOnSuccessListener {
                    taskSnapshot ->
                Log.i("Board Image URL", taskSnapshot.metadata!!.reference!!.downloadUrl.toString())
                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                        uri -> Log.i("Downloadable Image URI", uri.toString())
                    mBoardImageURL = uri.toString()

                    createBoard()
                    hideProgressDialog()
                }
            }.addOnFailureListener{
                    exception -> Toast.makeText(this@CreateBoardActivity, exception.message, Toast.LENGTH_LONG).show()

                hideProgressDialog()
            }

        }
    }

    private fun updateBoard(){
        val etBoardName: EditText = findViewById(R.id.etBoardName)
        var boardHashMap = HashMap<String, Any>()
        var anyChanges = false
        if(mBoardImageURL.isNotEmpty() && mBoardImageURL != mBoardDetails.image){
            boardHashMap[Constants.IMAGE] = mBoardImageURL
            anyChanges = true
        }
        if(etBoardName.text.toString() != mBoardDetails.name){
            boardHashMap[Constants.NAME] = etBoardName.text.toString()
            anyChanges = true
        }
        if(anyChanges){
            FirestoreClass().updateBoard(this, boardHashMap)
        }
        hideProgressDialog()
    }

    fun boardCreatedSuccessfully(){
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }

    fun boardUpdateSuccess(){
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
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
        val boardImage : CircleImageView = findViewById(R.id.ivBoardImage)
        if(result.resultCode == Activity.RESULT_OK){
            val data: Intent? = result.data
            mSelectedImageFileUriBoard = data?.data

            try{
                Glide
                    .with(this@CreateBoardActivity)
                    .load(mSelectedImageFileUriBoard)
                    .centerCrop()
                    .placeholder(R.drawable.ic_board_place_holder)
                    .into(boardImage!!)
            }catch(e: IOException){
                e.printStackTrace()
            }
        }
    }

    private fun getFileExtension(uri: Uri?): String?{
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri!!))
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
}