package com.redsystem.proyectochatapp_kotlin.Profile

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.hbb20.CountryCodePicker
import com.redsystem.proyectochatapp_kotlin.Model.User
import com.redsystem.proyectochatapp_kotlin.R

class ProfileActivity : AppCompatActivity() {

    private lateinit var P_imagen : ImageView
    private lateinit var P_n_usuario : TextView
    private lateinit var P_email : TextView
    private lateinit var P_proveedor : TextView
    private lateinit var P_nombres : EditText
    private lateinit var P_apellidos : EditText
    private lateinit var P_profesion : EditText
    private lateinit var P_domicilio : EditText
    private lateinit var P_edad : EditText
    private lateinit var P_telefono : TextView
    private lateinit var Btn_guardar : Button
    private lateinit var Editar_imagen : ImageView
    private lateinit var Editar_Telefono : ImageView

    private lateinit var Btn_verificar : MaterialButton

    var user : FirebaseUser?=null
    var reference : DatabaseReference?=null

    private var codigoTel = ""
    private var numeroTel = ""
    private var codigo_numero_Tel = ""

    private lateinit var progressDialog : ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        InicializarVariables()
        ObtenerDatos()
        EstadoCuenta()

        Btn_guardar.setOnClickListener {
            ActualizarInformacion()
        }

        Editar_imagen.setOnClickListener {
            val intent = Intent(applicationContext, EditImageProfile::class.java)
            startActivity(intent)
        }

        Editar_Telefono.setOnClickListener {
            EstablecerNumTel()
        }

        Btn_verificar.setOnClickListener {
            if (user!!.isEmailVerified){
                //User verificado
                //Toast.makeText(applicationContext, "User verificado", Toast.LENGTH_SHORT).show()
                CuentaVerificada()
            }else{
                //User no está verificado
                ConfirmarEnvio()
            }
        }

    }

    private fun ConfirmarEnvio() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Подтвердить учетную запись")
            .setMessage("Отправить подтверждение по эл.почте? ${user!!.email}")
            .setPositiveButton("Да"){d,e ->
                EnviarEmailConfirmacion()

            }
            .setNegativeButton("Нет"){d,e ->
                d.dismiss()
            }
            .show()
    }

    private fun EnviarEmailConfirmacion() {
        progressDialog.setMessage("Отправка инструкций по подтверждению на ваш адрес электронной почты ${user!!.email}")
        progressDialog.show()

        user!!.sendEmailVerification()
            .addOnSuccessListener {
            //Envio fue exitoso
                progressDialog.dismiss()
                Toast.makeText(applicationContext, "Отправленные инструкции, проверьте свой почтовый ящик ${user!!.email}", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener { e->
                //Envio no fue exitoso
                progressDialog.dismiss()
                Toast.makeText(applicationContext, "Операция завершилась неудачно из-за ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun EstadoCuenta(){
        if (user!!.isEmailVerified){
            Btn_verificar.text = "Верифицирован"
        }else{
            Btn_verificar.text = "Не верифицирован"
        }
    }

    private fun EstablecerNumTel() {

        /*Declarar las vistas del CD*/
        val Establecer_Telefono : EditText
        val SelectorCodigoPais : CountryCodePicker
        val Btn_aceptar_Telefono : MaterialButton

        val dialog = Dialog(this@ProfileActivity)

        /*Realizar la conexión con el diseño*/
        dialog.setContentView(R.layout.window_phone)

        /*Inicializar las vistas*/
        Establecer_Telefono = dialog.findViewById(R.id.Establecer_Telefono)
        SelectorCodigoPais = dialog.findViewById(R.id.SelectorCodigoPais)
        Btn_aceptar_Telefono = dialog.findViewById(R.id.Btn_aceptar_Telefono)

        /*Asignar un evento al botón*/
        Btn_aceptar_Telefono.setOnClickListener {
            codigoTel = SelectorCodigoPais.selectedCountryCodeWithPlus
            numeroTel = Establecer_Telefono.text.toString().trim()
            codigo_numero_Tel = codigoTel + numeroTel
            if (numeroTel.isEmpty()){
                Toast.makeText(applicationContext,"Введите номер телефона", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }else{
                P_telefono.text = codigo_numero_Tel
                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.setCanceledOnTouchOutside(false)


    }

    private fun InicializarVariables(){
        P_imagen = findViewById(R.id.P_imagen)
        P_n_usuario = findViewById(R.id.P_n_usuario)
        P_proveedor = findViewById(R.id.P_proveedor)
        P_email = findViewById(R.id.P_email)
        P_nombres = findViewById(R.id.P_nombres)
        P_apellidos = findViewById(R.id.P_apellidos)
        P_profesion = findViewById(R.id.P_profesion)
        P_domicilio = findViewById(R.id.P_domicilio)
        P_edad = findViewById(R.id.P_edad)
        P_telefono = findViewById(R.id.P_telefono)
        Btn_guardar = findViewById(R.id.Btn_Guardar)
        Editar_imagen = findViewById(R.id.Editar_imagen)
        Editar_Telefono = findViewById(R.id.Editar_Telefono)
        Btn_verificar = findViewById(R.id.Btn_verificar)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Пожалуйста, подождите")
        progressDialog.setCanceledOnTouchOutside(false)

        user = FirebaseAuth.getInstance().currentUser
        reference = FirebaseDatabase.getInstance().reference.child("Usuarios").child(user!!.uid)

    }

    private fun ObtenerDatos(){
        reference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    //Obtenemos los datos de Firebase
                    val user : User?= snapshot.getValue(User::class.java)
                    val str_n_usuario = user!!.getN_Usuario()
                    val str_email = user.getEmail()
                    val str_proveedor = user.getProveedor()
                    val str_nombres = user.getNombres()
                    val str_apellidos = user.getApellidos()
                    val str_profesion = user.getProfesion()
                    val str_domicilio = user.getDomicilio()
                    val str_edad = user.getEdad()
                    val str_telefono = user.getTelefono()

                    //Seteamos la información en las vistas
                    P_n_usuario.text = str_n_usuario
                    P_email.text = str_email
                    P_proveedor.text = str_proveedor
                    P_nombres.setText(str_nombres)
                    P_apellidos.setText(str_apellidos)
                    P_profesion.setText(str_profesion)
                    P_domicilio.setText(str_domicilio)
                    P_edad.setText(str_edad)
                    P_telefono.setText(str_telefono)
                    Glide.with(applicationContext).load(user.getImagen()).placeholder(R.drawable.imagen_usuario_visitado).into(P_imagen)

                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun ActualizarInformacion(){
        val str_nombres = P_nombres.text.toString()
        val str_apellidos = P_apellidos.text.toString()
        val str_profesion = P_profesion.text.toString()
        val str_domicilio = P_domicilio.text.toString()
        val str_edad = P_edad.text.toString()
        val str_telefono = P_telefono.text.toString()

        val hashmap = HashMap<String, Any>()
        hashmap["nombres"] = str_nombres
        hashmap["apellidos"] = str_apellidos
        hashmap["profesion"] = str_profesion
        hashmap["domicilio"] = str_domicilio
        hashmap["edad"] = str_edad
        hashmap["telefono"] = str_telefono

        reference!!.updateChildren(hashmap).addOnCompleteListener{task->
                if (task.isSuccessful){
                    Toast.makeText(applicationContext,"Данные были обновлены", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(applicationContext,"Данные не были обновлены", Toast.LENGTH_SHORT).show()

                }
        }.addOnFailureListener{e->
            Toast.makeText(applicationContext,"Произошла ошибка: ${e.message}", Toast.LENGTH_SHORT).show()

        }


    }

    private fun CuentaVerificada(){

        val BtnEntendidoVerificado : MaterialButton
        val dialog = Dialog(this@ProfileActivity)

        dialog.setContentView(R.layout.window_verification)

        BtnEntendidoVerificado = dialog.findViewById(R.id.BtnEntendidoVerificado)
        BtnEntendidoVerificado.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.setCanceledOnTouchOutside(false)

    }


    private fun ActualizarEstado(estado : String){
        val reference = FirebaseDatabase.getInstance().reference.child("Usuarios")
            .child(user!!.uid)
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