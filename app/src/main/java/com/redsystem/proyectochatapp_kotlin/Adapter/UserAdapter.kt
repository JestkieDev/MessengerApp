package com.redsystem.proyectochatapp_kotlin.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.redsystem.proyectochatapp_kotlin.Chat.MessagesActivity
import com.redsystem.proyectochatapp_kotlin.Model.Chat
import com.redsystem.proyectochatapp_kotlin.Model.User
import com.redsystem.proyectochatapp_kotlin.R

class UserAdapter (context : Context, listaUsers : List<User>, chatLeido : Boolean) : RecyclerView.Adapter<UserAdapter.ViewHolder?>(){

    private val context : Context
    private val listaUsers : List<User>
    private var chatLeido : Boolean
    var ultimoMensaje : String = ""

    init {
        this.context = context
        this.listaUsers = listaUsers
        this.chatLeido = chatLeido
    }

    class ViewHolder(itemView : View):RecyclerView.ViewHolder(itemView){
        var nombre_usuario : TextView
        //var email_usuario : TextView
        var imagen_usuario : ImageView
        var imagen_online : ImageView
        var imagen_offline : ImageView
        var Txt_ultimo_mensaje : TextView

        init {
            nombre_usuario = itemView.findViewById(R.id.Item_nombre_usuario)
            //email_usuario = itemView.findViewById(R.id.Item_email_usuario)
            imagen_usuario = itemView.findViewById(R.id.Item_imagen)
            imagen_online = itemView.findViewById(R.id.imagen_online)
            imagen_offline = itemView.findViewById(R.id.imagen_offline)
            Txt_ultimo_mensaje = itemView.findViewById(R.id.Txt_ultimo_mensaje)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
       val view : View = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user : User = listaUsers[position]
        holder.nombre_usuario.text = user.getN_Usuario()
        //holder.email_usuario.text = usuario.getEmail()
        Glide.with(context).load(user.getImagen()).placeholder(R.drawable.ic_item_usuario).into(holder.imagen_usuario)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, MessagesActivity::class.java)
            //Enviamos el uid del usuario seleccionado
            intent.putExtra("uid_usuario", user.getUid())
            //Toast.makeText(context, "El usuario seleccionado es: "+usuario.getN_Usuario(),Toast.LENGTH_SHORT).show()
            context.startActivity(intent)
        }

        if (chatLeido){
            ObtenerUltimoMensaje(user.getUid(), holder.Txt_ultimo_mensaje)
        }else{
            holder.Txt_ultimo_mensaje.visibility = View.GONE
        }


        if (chatLeido){
            if (user.getEstado() == "online"){
                holder.imagen_online.visibility = View.VISIBLE
                holder.imagen_offline.visibility = View.GONE
            }else{
                holder.imagen_online.visibility = View.GONE
                holder.imagen_offline.visibility = View.VISIBLE
            }
        }
        else{
            holder.imagen_online.visibility = View.GONE
            holder.imagen_offline.visibility = View.GONE
        }




    }

    private fun ObtenerUltimoMensaje(ChatUsuarioUid: String?, txtUltimoMensaje: TextView) {
        ultimoMensaje = "default"
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().reference.child("Chats")
        reference.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children){
                    val chat : Chat?= dataSnapshot.getValue(Chat::class.java)
                    if (firebaseUser!= null && chat!= null){
                        if (chat.getReceptor() == firebaseUser!!.uid &&
                                chat.getEmisor() == ChatUsuarioUid ||
                                chat.getReceptor() == ChatUsuarioUid &&
                                chat.getEmisor() == firebaseUser!!.uid){
                            ultimoMensaje = chat.getMensaje()!!
                        }
                    }
                }

                when(ultimoMensaje){
                    "defaultMensaje" -> txtUltimoMensaje.text = "Нет сообщений"
                    "Se ha enviado la imagen" -> txtUltimoMensaje.text = "Изображение"
                    else-> txtUltimoMensaje.text = ultimoMensaje
                }
                ultimoMensaje =  "default"
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

    }

    override fun getItemCount(): Int {
        return listaUsers.size
    }
}