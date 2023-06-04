package com.example.navdrawkotlin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {
    private lateinit var edUsername: EditText

    private lateinit var edPassword: EditText

    private lateinit var btnLogin: Button

    private lateinit var btnCancel: Button

    private lateinit var mAuth: FirebaseAuth

    var btnReg: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // btnReg = findViewById(R.id.btnReg)

        edUsername = findViewById(R.id.editTextTextEmailAddress)

        edPassword = findViewById(R.id.editTextTextPassword)

        btnLogin = findViewById(R.id.button)

        btnCancel = findViewById(R.id.button2)

        btnReg = findViewById(R.id.button5)

        mAuth = FirebaseAuth.getInstance()



        btnLogin.setOnClickListener {

            loginUser()

        }

    }



    private fun loginUser() {

        try {

            val email = edUsername.text.toString().trim()

            val password = edPassword.text.toString().trim()



            if (TextUtils.isEmpty(email)) {

                Toast.makeText(this, "Please enter an email address!", Toast.LENGTH_SHORT).show()

                edUsername.requestFocus()

                return

            }

            if (TextUtils.isEmpty(password)) {

                Toast.makeText(this, "Please enter a password!", Toast.LENGTH_SHORT).show()

                edPassword.requestFocus()

                return

            }



            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                    edUsername.setText("")

                    edPassword.setText("")

                    edUsername.requestFocus()


                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Prevents user from going back to the registration activity

                } else {

                    Toast.makeText(this, "Login Unsuccessful! Please try again.", Toast.LENGTH_SHORT).show()

                    edUsername.setText("")

                    edPassword.setText("")

                    edUsername.requestFocus()

                }

            }

        } catch (eer: Exception) {

            Toast.makeText(this, "Error Occurred: " + eer.message, Toast.LENGTH_SHORT).show()

        }

    }
    fun cancelFields(view: View)

    {

        Toast.makeText(this, "Login option cancelled", Toast.LENGTH_SHORT).show()

        edUsername.setText("")

        edPassword.setText("")

        edUsername.requestFocus()

    }



    fun regScreen(view: View)

    {

        val intent = Intent(this, Register::class.java)

        startActivity(intent)

    }
}