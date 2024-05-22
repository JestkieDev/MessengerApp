package com.redsystem.proyectochatapp_kotlin.Profile

import android.Manifest
import android.app.Dialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.redsystem.proyectochatapp_kotlin.R

class EditImageProfile : AppCompatActivity() {

    private lateinit var ImagenPerfilActualizar : ImageView
    private lateinit var BtnElegirImagen : Button
    private lateinit var BtnActualizarImagen : Button
    private var imageUri : Uri?= null

    private lateinit var firebaseAuth : FirebaseAuth
    var firebaseUser : FirebaseUser?=null
    private lateinit var progressDialog : ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_image_profile)

        ImagenPerfilActualizar = findViewById(R.id.ImagenPerfilActualizar)
        BtnElegirImagen = findViewById(R.id.BtnElegirImagenDe)
        BtnActualizarImagen = findViewById(R.id.BtnActualizarImagen)

        progressDialog = ProgressDialog(this@EditImageProfile)
        progressDialog.setTitle("Пожалуйста, подождите")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = FirebaseAuth.getInstance().currentUser


        BtnElegirImagen.setOnClickListener {
            //Toast.makeText(applicationContext,"Seleccionar imagen de", Toast.LENGTH_SHORT).show()
            MostrarDialog()
        }

        BtnActualizarImagen.setOnClickListener {
            //Toast.makeText(applicationContext,"Actualizar imagen", Toast.LENGTH_SHORT).show()
            ValidarImagen()
        }
    }

    private fun ValidarImagen(){
        if (imageUri == null){
            Toast.makeText(applicationContext, "Вставьте изображение!",Toast.LENGTH_SHORT).show()
        }else{
            SubirImagen()
        }
    }

    private fun SubirImagen() {
        progressDialog.setMessage("Обновляем изображение... ")
        progressDialog.show()
        val rutaImagen = "Perfil_usuario/"+firebaseAuth.uid
        val referenceStorage = FirebaseStorage.getInstance().getReference(rutaImagen)
        referenceStorage.putFile(imageUri!!).addOnSuccessListener { tarea->
            val uriTarea : Task<Uri> = tarea.storage.downloadUrl
            while (!uriTarea.isSuccessful);
            val urlImagen = "${uriTarea.result}"
            ActualizarImagenBD(urlImagen)

        }.addOnFailureListener{e->
            Toast.makeText(applicationContext, "Не удалось загрузить изображение из-за: ${e.message}",Toast.LENGTH_SHORT).show()
        }
    }

    private fun ActualizarImagenBD(urlImagen: String) {
        progressDialog.setMessage("Обновление изображения профиля")
        val hashmap : HashMap<String, Any> = HashMap()
        if (imageUri!=null){
            hashmap["imagen"] = urlImagen
        }

        val reference = FirebaseDatabase.getInstance().getReference("Usuarios")
        reference.child(firebaseAuth.uid!!).updateChildren(hashmap).addOnSuccessListener {
            progressDialog.dismiss()
            Toast.makeText(applicationContext, "Ваше изображение было обновлено",Toast.LENGTH_SHORT).show()

        }.addOnFailureListener{e->
            Toast.makeText(applicationContext, "Ваше изображение не было обновлено из-за: ${e.message}",Toast.LENGTH_SHORT).show()
        }

    }

    private fun AbrirGaleria(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galeriaActivityResultLauncher.launch(intent)
    }

    private val galeriaActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult>{resultado->
            if (resultado.resultCode == RESULT_OK){
                val data = resultado.data
                imageUri = data!!.data
                ImagenPerfilActualizar.setImageURI(imageUri)
            }else{
                Toast.makeText(applicationContext,"Отменено пользователем", Toast.LENGTH_SHORT).show()

            }

        }
    )

    private val requestGalleryPermiso =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){Permiso_concedido->
            if (Permiso_concedido){
                AbrirGaleria()
            }else{
                Toast.makeText(applicationContext,"Разрешение на доступ к галерее не было предоставлено", Toast.LENGTH_SHORT).show()
            }
        }

    private fun AbrirCamara(){
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Titulo")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Descripcion")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        camaraActivityResultLauncher.launch(intent)
    }


    private val camaraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){resultado_camara->
            if (resultado_camara.resultCode == RESULT_OK){
                ImagenPerfilActualizar.setImageURI(imageUri)
            }else{
                Toast.makeText(applicationContext,"Отменено пользователем", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestCameraPermiso =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){Permiso_concedido->
            if (Permiso_concedido){
                AbrirCamara()
            }else{
                Toast.makeText(applicationContext,"Разрешение на доступ к камере не было предоставлено", Toast.LENGTH_SHORT).show()
            }

        }

    private fun MostrarDialog(){
        val Btn_abrir_galeria : Button
        val Btn_abrir_camara : Button

        val dialog = Dialog(this@EditImageProfile)

        dialog.setContentView(R.layout.window_image_choice)

        Btn_abrir_galeria = dialog.findViewById(R.id.Btn_abrir_galeria)
        Btn_abrir_camara = dialog.findViewById(R.id.Btn_abrir_camara)

        Btn_abrir_galeria.setOnClickListener {
            //Toast.makeText(applicationContext, "Abrir galería", Toast.LENGTH_SHORT).show()
            if (ContextCompat.checkSelfPermission(applicationContext,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                AbrirGaleria()
                dialog.dismiss()
            }else{
                requestGalleryPermiso.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        }

        Btn_abrir_camara.setOnClickListener {
            //Toast.makeText(applicationContext, "Abrir cámara", Toast.LENGTH_SHORT).show()
            if (ContextCompat.checkSelfPermission(applicationContext,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                AbrirCamara()
                dialog.dismiss()
            }else{
                requestCameraPermiso.launch(Manifest.permission.CAMERA)
            }

        }

        dialog.show()

    }



    private fun ActualizarEstado(estado : String){
        val reference = FirebaseDatabase.getInstance().reference.child("Usuarios")
            .child(firebaseUser!!.uid)
        val hashMap = HashMap<String, Any>()
        hashMap["estado"] = estado
        reference!!.updateChildren(hashMap)
    }

    override fun onResume() {
        super.onResume()
        ActualizarEstado("online")
    }

    override fun onPause() {
        super.onPause()
        ActualizarEstado("offline")
    }
}