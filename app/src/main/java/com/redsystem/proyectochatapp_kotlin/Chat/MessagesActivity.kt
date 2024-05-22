package com.redsystem.proyectochatapp_kotlin.Chat

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.redsystem.proyectochatapp_kotlin.Adapter.ChatAdapter
import com.redsystem.proyectochatapp_kotlin.Model.Chat
import com.redsystem.proyectochatapp_kotlin.Model.User
import com.redsystem.proyectochatapp_kotlin.Notifications.*
import com.redsystem.proyectochatapp_kotlin.Profile.VisitProfileActivity
import com.redsystem.proyectochatapp_kotlin.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MessagesActivity : AppCompatActivity() {

    private lateinit var imagen_perfil_chat : ImageView
    private lateinit var N_usuario_chat : TextView
    private lateinit var Et_mensaje : EditText
    private lateinit var IB_Adjuntar : ImageButton
    private lateinit var IB_Enviar : ImageButton
    var uid_usuario_seleccionado : String = ""
    var firebaseUser : FirebaseUser ?= null
    private var imagenUri : Uri ?= null

    lateinit var RV_chats : RecyclerView
    var chatAdapter : ChatAdapter ?= null
    var chatList : List<Chat> ?= null

    var reference : DatabaseReference ?= null
    var seenListener : ValueEventListener ?= null

    var notificar = false
    var apiService : APIService?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)
        InicializarVistas()
        ObtenerUid()
        LeerInfoUsuarioSeleccionado()

        IB_Adjuntar.setOnClickListener {
            notificar = true
            if (ContextCompat.checkSelfPermission(applicationContext,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                AbrirGaleria()
            }else{
                requestGalleryPermiso.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        IB_Enviar.setOnClickListener {
            notificar = true
            val mensaje = Et_mensaje.text.toString()
            if (mensaje.isEmpty()){
                Toast.makeText(applicationContext, "Пожалуйста, введите сообщение", Toast.LENGTH_SHORT).show()
            }else{
                EnviarMensaje(firebaseUser!!.uid, uid_usuario_seleccionado, mensaje)
                Et_mensaje.setText("")
            }
        }

        MensajeVisto(uid_usuario_seleccionado)
    }

    private fun InicializarVistas(){

        val toolbar : Toolbar = findViewById(R.id.toolbar_chat)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""

        apiService = Client.Client.getClient("https://fcm.googleapis.com/")!!.create(APIService::class.java)

        imagen_perfil_chat = findViewById(R.id.imagen_perfil_chat)
        N_usuario_chat = findViewById(R.id.N_usuario_chat)
        Et_mensaje = findViewById(R.id.Et_mensaje)
        IB_Adjuntar = findViewById(R.id.IB_Adjuntar)
        IB_Enviar = findViewById(R.id.IB_Enviar)
        firebaseUser = FirebaseAuth.getInstance().currentUser

        RV_chats = findViewById(R.id.RV_chats)
        RV_chats.setHasFixedSize(true)
        var linearLayoutManager = LinearLayoutManager(applicationContext)
        linearLayoutManager.stackFromEnd = true
        RV_chats.layoutManager = linearLayoutManager
    }

    private fun ObtenerUid(){
        intent = intent
        uid_usuario_seleccionado = intent.getStringExtra("uid_usuario").toString()
    }

    private fun EnviarMensaje(uid_emisor : String, uid_receptor : String, mensaje : String) {
        val reference = FirebaseDatabase.getInstance().reference
        val mensajeKey = reference.push().key

        val infoMensaje = HashMap<String, Any?> ()
        infoMensaje["id_mensaje"] = mensajeKey
        infoMensaje["emisor"] = uid_emisor
        infoMensaje["receptor"] = uid_receptor
        infoMensaje["mensaje"] = mensaje
        infoMensaje["url"] = ""
        infoMensaje["visto"] = false
        reference.child("Chats").child(mensajeKey!!).setValue(infoMensaje).addOnCompleteListener { tarea->
            if (tarea.isSuccessful){
                val listaMensajesEmisor = FirebaseDatabase.getInstance().reference.child("ListaMensajes")
                    .child(firebaseUser!!.uid)
                    .child(uid_usuario_seleccionado)

                listaMensajesEmisor.addListenerForSingleValueEvent(object  : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()){
                            listaMensajesEmisor.child("uid").setValue(uid_usuario_seleccionado)
                        }

                        val listaMensajesReceptor = FirebaseDatabase.getInstance().reference.child("ListaMensajes")
                            .child(uid_usuario_seleccionado)
                            .child(firebaseUser!!.uid)
                        listaMensajesReceptor.child("uid").setValue(firebaseUser!!.uid)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                })
            }
        }

        val usuarioReference = FirebaseDatabase.getInstance().reference
            .child("Usuarios").child(firebaseUser!!.uid)
        usuarioReference.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (notificar){
                    enviarNotificacion(uid_receptor, user!!.getN_Usuario(), mensaje)
                }
                notificar = false
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

    }

    private fun enviarNotificacion(uidReceptor: String?, nUsuario: String?, mensaje: String) {
        val reference = FirebaseDatabase.getInstance().reference.child("Tokens")
        val query = reference.orderByKey().equalTo(uidReceptor)

        query.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children){
                    val token : Token?= dataSnapshot.getValue(Token::class.java)

                    val data = Data(
                        firebaseUser!!.uid,
                        R.mipmap.ic_chat,
                        "$nUsuario: $mensaje",
                        "Новое сообщение!",
                        uid_usuario_seleccionado
                    )

                    val sender = Sender(data!!, token!!.getToken().toString())

                    apiService!!.sendNotification(sender)
                        .enqueue(object  : Callback<MyResponse>{
                            override fun onResponse(
                                call: Call<MyResponse>,
                                response: Response<MyResponse>
                            ) {
                                if (response.code() == 800){
                                    if (response.body()!!.success !== 1){
                                        Toast.makeText(applicationContext, "Что-то пошло не так", Toast.LENGTH_SHORT).show()
                                    }
                                }

                            }

                            override fun onFailure(call: Call<MyResponse>, t: Throwable) {

                            }

                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

    }

    private fun LeerInfoUsuarioSeleccionado(){
        val reference = FirebaseDatabase.getInstance().reference.child("Usuarios")
            .child(uid_usuario_seleccionado)

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user : User? = snapshot.getValue(User::class.java)
                //Obtener el nombre de usuario
                N_usuario_chat.text = user!!.getN_Usuario()
                //Obtenemos la imagen de perfil
                Glide.with(applicationContext).load(user.getImagen())
                    .placeholder(R.drawable.ic_item_usuario)
                    .into(imagen_perfil_chat)

                RecuperarMensajes(firebaseUser!!.uid, uid_usuario_seleccionado, user.getImagen())
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun RecuperarMensajes(EmisorUid: String, ReceptorUid: String, ReceptorImagen: String?) {
        chatList = ArrayList()
        val reference = FirebaseDatabase.getInstance().reference.child("Chats")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                (chatList as ArrayList<Chat>).clear()
                for (sn in snapshot.children){
                    val chat = sn.getValue(Chat::class.java)

                    if (chat!!.getReceptor().equals(EmisorUid) && chat.getEmisor().equals(ReceptorUid)
                        || chat.getReceptor().equals(ReceptorUid) && chat.getEmisor().equals(EmisorUid)){
                        (chatList as ArrayList<Chat>).add(chat)
                    }

                    chatAdapter = ChatAdapter(this@MessagesActivity, (chatList as ArrayList<Chat>), ReceptorImagen!!)
                    RV_chats.adapter = chatAdapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

    }

    private fun MensajeVisto(usuarioUid : String){
        reference = FirebaseDatabase.getInstance().reference.child("Chats")
        seenListener = reference!!.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children){
                    val chat = dataSnapshot.getValue(Chat::class.java)
                    if (chat!!.getReceptor().equals(firebaseUser!!.uid) && chat!!.getEmisor().equals(usuarioUid)){
                        val hashMap = HashMap<String, Any>()
                        hashMap["visto"] = true
                        dataSnapshot.ref.updateChildren(hashMap)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun AbrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galeriaARL.launch(intent)
    }

    private val galeriaARL = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> {resultado->
            if (resultado.resultCode == RESULT_OK){
                val data = resultado.data
                imagenUri = data!!.data

                val cargandoImagen = ProgressDialog(this@MessagesActivity)
                cargandoImagen.setMessage("Пожалуйста, подождите, изображение отправляется")
                cargandoImagen.setCanceledOnTouchOutside(false)
                cargandoImagen.show()

                val carpetaImagenes = FirebaseStorage.getInstance().reference.child("Imágenes de mensajes")
                val reference = FirebaseDatabase.getInstance().reference
                val idMensaje = reference.push().key
                val nombreImagen = carpetaImagenes.child("$idMensaje.jpg")

                val uploadTask : StorageTask<*>
                uploadTask = nombreImagen.putFile(imagenUri!!)
                uploadTask.continueWithTask(Continuation <UploadTask.TaskSnapshot, Task<Uri>>{task->
                    if (!task.isSuccessful){
                        task.exception?.let {
                            throw it
                        }
                    }
                    return@Continuation nombreImagen.downloadUrl
                }).addOnCompleteListener { task->
                    if (task.isSuccessful){
                        cargandoImagen.dismiss()
                        val downloadUrl = task.result
                        val url = downloadUrl.toString()

                        val infoMensajeImagen = HashMap<String, Any?>()
                        infoMensajeImagen["id_mensaje"] = idMensaje
                        infoMensajeImagen["emisor"] = firebaseUser!!.uid
                        infoMensajeImagen["receptor"] = uid_usuario_seleccionado
                        infoMensajeImagen["mensaje"] = "Изображение отправлено"
                        infoMensajeImagen["url"] = url
                        infoMensajeImagen["visto"] = false

                        reference.child("Chats").child(idMensaje!!).setValue(infoMensajeImagen)
                            .addOnCompleteListener { tarea->
                                if (tarea.isSuccessful){
                                    val usuarioReference = FirebaseDatabase.getInstance().reference
                                        .child("Usuarios").child(firebaseUser!!.uid)
                                    usuarioReference.addValueEventListener(object : ValueEventListener{
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val user = snapshot.getValue(User::class.java)
                                            if (notificar){
                                                enviarNotificacion(
                                                    uid_usuario_seleccionado,
                                                    user!!.getN_Usuario(),
                                                    "Изображение отправлено")
                                            }
                                            notificar = false
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            TODO("Not yet implemented")
                                        }
                                    })

                                }
                            }

                        reference.child("Chats").child(idMensaje!!).setValue(infoMensajeImagen)
                            .addOnCompleteListener { tarea->
                                if (tarea.isSuccessful){
                                    val listaMensajesEmisor = FirebaseDatabase.getInstance().reference.child("ListaMensajes")
                                        .child(firebaseUser!!.uid)
                                        .child(uid_usuario_seleccionado)

                                    listaMensajesEmisor.addListenerForSingleValueEvent(object  : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            if (!snapshot.exists()){
                                                listaMensajesEmisor.child("uid").setValue(uid_usuario_seleccionado)
                                            }

                                            val listaMensajesReceptor = FirebaseDatabase.getInstance().reference.child("ListaMensajes")
                                                .child(uid_usuario_seleccionado)
                                                .child(firebaseUser!!.uid)
                                            listaMensajesReceptor.child("uid").setValue(firebaseUser!!.uid)
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            TODO("Not yet implemented")
                                        }

                                    })
                                }
                            }
                        Toast.makeText(applicationContext, "Изображение было успешно отправлено", Toast.LENGTH_SHORT).show()

                    }
                }
            }
            else{
                Toast.makeText(applicationContext,"Отменено пользователем", Toast.LENGTH_SHORT).show()
            }

        }
    )

    override fun onPause() {
        super.onPause()
        reference!!.removeEventListener(seenListener!!)
        ActualizarEstado("offline")
    }

    private val requestGalleryPermiso =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){Permiso_concedido->
            if (Permiso_concedido){
                AbrirGaleria()
            }else{
                Toast.makeText(applicationContext,"Разрешение на доступ к галерее не было предоставлено", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater : MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_visit_profile, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.menu_visitar->{
                val intent = Intent(applicationContext, VisitProfileActivity::class.java)
                intent.putExtra("uid", uid_usuario_seleccionado)
                startActivity(intent)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
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


}