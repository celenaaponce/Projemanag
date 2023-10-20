package com.example.projemanag.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import com.example.projemanag.R

class IntroActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        val btnSignUpIntro : Button = findViewById(R.id.btnSignUpIntro)
        val btnSignInIntro : Button = findViewById(R.id.btnSignInIntro)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        btnSignUpIntro.setOnClickListener{
            val intent : Intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        btnSignInIntro.setOnClickListener{
            val intent : Intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

    }
}