package com.redsystem.proyectochatapp_kotlin

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class Inicio : AppCompatActivity() {

    private lateinit var Btn_ir_logeo: MaterialButton

    var firebaseUser: FirebaseUser? = null
    private lateinit var auth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)
        Btn_ir_logeo = findViewById(R.id.Btn_ir_logeo)
        auth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Пожалуйста, подождите")
        progressDialog.setCanceledOnTouchOutside(false)



        Btn_ir_logeo.setOnClickListener {
            val intent = Intent(this@Inicio, LoginActivity::class.java)
            //Toast.makeText(applicationContext, "Login", Toast.LENGTH_SHORT).show()
            startActivity(intent)
        }
    }


    private fun GuardarInfoBD() {
        progressDialog.setMessage("Ваша информация записывается...")
        progressDialog.show()

        /*Obtener información de una cuenta de Google*/
        val uidGoogle = auth.uid
        val correoGoogle = auth.currentUser?.email
        val n_Google = auth.currentUser?.displayName
        val nombre_usuario_G: String = n_Google.toString()

        val hashmap = HashMap<String, Any?>()
        hashmap["uid"] = uidGoogle
        hashmap["n_usuario"] = nombre_usuario_G
        hashmap["email"] = correoGoogle
        hashmap["imagen"] = ""
        hashmap["buscar"] = nombre_usuario_G.lowercase()

        /*Nuevos datos de usuario*/
        hashmap["nombres"] = ""
        hashmap["apellidos"] = ""
        hashmap["edad"] = ""
        hashmap["profesion"] = ""
        hashmap["domicilio"] = ""
        hashmap["telefono"] = ""
        hashmap["estado"] = "offline"
        hashmap["proveedor"] = "Google"

        /*Referencia a la base de datos*/
        val reference = FirebaseDatabase.getInstance().getReference("Usuarios")
        reference.child(uidGoogle!!)
            .setValue(hashmap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                startActivity(Intent(applicationContext, MainActivity::class.java))
                Toast.makeText(
                    applicationContext,
                    "Успешно",
                    Toast.LENGTH_SHORT
                ).show()
                finishAffinity()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT).show()
            }

    }

    private fun ComprobarSesion() {

        firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            val intent = Intent(this@Inicio, MainActivity::class.java)
            Toast.makeText(applicationContext, "Выполнен вход", Toast.LENGTH_SHORT).show()
            startActivity(intent)
            finish()
        }
    }

    override fun onStart() {
        ComprobarSesion()
        super.onStart()
    }
}