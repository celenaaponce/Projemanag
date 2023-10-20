package com.example.projemanag.activities

import android.content.Intent
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
import com.example.projemanag.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase

class SignInActivity : BaseActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        auth = FirebaseAuth.getInstance()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val toolbarSignIn : Toolbar = findViewById(R.id.toolbar_sign_in_activity)
        setSupportActionBar(toolbarSignIn)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbarSignIn?.setNavigationOnClickListener {
            onBackPressed()
        }

        val btnSignIn : Button = findViewById(R.id.btnSignIn)
        btnSignIn.setOnClickListener {
            signInUser()
        }
    }

    private fun signInUser(){
        val etEmail: EditText = findViewById(R.id.etEmailSignIn)
        val etPassword: EditText = findViewById(R.id.etPasswordSignIn)

        val email: String = etEmail.text.toString().trim {it <= ' '}
        val password: String = etPassword.text.toString().trim {it <= ' '}

        if(validateForm(email, password)){

            showProgressDialog("Please wait")
            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener{
                        task ->
                    hideProgressDialog()
                    if(task.isSuccessful){
                        Log.d("log in", "signInWithEmail:success")
                        val user = auth.currentUser
                        startActivity(Intent(this, MainActivity::class.java))
                    }else {
                        Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun validateForm(email: String, password:String): Boolean{
        return when {
            TextUtils.isEmpty(email)->{
                showErrorSnackBar("Please enter an email")
                false}
            TextUtils.isEmpty(password)->{
                showErrorSnackBar("Please enter a password")
                false}
            else->{true}
        }
    }

    fun signInSuccess(user: User){
        hideProgressDialog()
        startActivity(Intent(this, MainActivity::class.java))
        finish()

    }

}