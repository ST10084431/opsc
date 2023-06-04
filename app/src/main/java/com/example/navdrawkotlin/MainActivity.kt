package com.example.navdrawkotlin

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.navdrawkotlin.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var emailTextView: TextView

    private lateinit var firestore: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)


        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        val navHeader = navView.getHeaderView(0)
        emailTextView = navHeader.findViewById(R.id.TvEmail)

        firestore = FirebaseFirestore.getInstance()



        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        updateEmail()

        navView.setNavigationItemSelectedListener { item ->
            val id = item.itemId

            when (id) {
                R.id.nav_about -> {
                    val intent = Intent(this@MainActivity, About::class.java)
                    startActivity(intent)
                }
                R.id.nav_login -> {
                    if (!isLoggedIn()) {
                        val intent = Intent(this@MainActivity, Login::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@MainActivity, "Already logged in", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.nav_camera -> {
                    if (isLoggedIn()) {
                        val intent = Intent(this@MainActivity, Camera::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@MainActivity, "Please log in to access the camera", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.nav_reg -> {
                    if (!isLoggedIn()) {
                        val intent = Intent(this@MainActivity, Register::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@MainActivity, "Already logged in", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.nav_tasks -> {
                    if (isLoggedIn()) {
                        val intent = Intent(this@MainActivity, Tasks::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@MainActivity, "Please log in to access tasks", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.nav_timer -> {
                    if (isLoggedIn()) {
                        val intent = Intent(this@MainActivity, TimerCapture::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@MainActivity, "Please log in to access the timer", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.LogOut -> {
                    if (isLoggedIn()) {
                        logOut()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Prevents user from going back to the registration activity
                    } else {
                        Toast.makeText(this@MainActivity, "Please log in to access the Logout", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            true
        }



    }

    private fun isLoggedIn(): Boolean {
        val firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser
        return currentUser != null
    }
    private fun logOut() {
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.signOut()
        // Perform any additional actions after signing out.
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
            // Inflate the menu; this adds items to the action bar if it is present.
            menuInflater.inflate(R.menu.main, menu)
            return true
        }

        override fun onSupportNavigateUp(): Boolean {
            val navController = findNavController(R.id.nav_host_fragment_content_main)
            return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
        }

    fun updateEmail() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email

        // Update the user email value in your code
        val updatedEmail = userEmail ?: ""

        // Use the updated email value in your code

        emailTextView.text = updatedEmail
    }


}

