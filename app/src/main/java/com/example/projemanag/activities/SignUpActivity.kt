package com.example.projemanag.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.example.projemanag.R
import com.example.projemanag.firebase.FirestoreClass
import com.example.projemanag.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SignUpActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val toolbarSignUp : Toolbar = findViewById(R.id.toolbar_sign_up_activity)
        setSupportActionBar(toolbarSignUp)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbarSignUp?.setNavigationOnClickListener {
            onBackPressed()
        }
        val btnSignUp : Button = findViewById(R.id.btnSignUp)
        btnSignUp.setOnClickListener {
            registerUser()
        }
    }

    fun userRegisteredSuccess(){
        Toast.makeText(this, "You have registered.", Toast.LENGTH_LONG).show()
        hideProgressDialog()
        FirebaseAuth.getInstance().signOut()
        finish()
    }

    private fun registerUser(){
        val etName: EditText = findViewById(R.id.etName)
        val etEmail: EditText = findViewById(R.id.etEmail)
        val etPassword: EditText = findViewById(R.id.etPassword)

        val name: String = etName.text.toString().trim {it <= ' '}
        val email: String = etEmail.text.toString().trim {it <= ' '}
        val password: String = etPassword.text.toString().trim {it <= ' '}

        if(validateForm(name, email, password)){

            showProgressDialog("Please wait")
            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password).addOnCompleteListener{
                        task ->
                        if(task.isSuccessful){
                            val firebaseUser : FirebaseUser = task.result!!.user!!
                            val registeredEmail = firebaseUser.email!!
                            val user = User(firebaseUser.uid, name, registeredEmail)
                            FirestoreClass().registerUser(this, user)
                        }else {
                            Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                        }
                    }
        }
    }

    private fun validateForm(name: String, email: String, password:String): Boolean{
        return when {
            TextUtils.isEmpty(name)->{
                showErrorSnackBar("Please enter a name")
            false}
            TextUtils.isEmpty(email)->{
                showErrorSnackBar("Please enter an email")
                false}
            TextUtils.isEmpty(password)->{
                showErrorSnackBar("Please enter a password")
                false}
            else->{true}
        }
    }
}