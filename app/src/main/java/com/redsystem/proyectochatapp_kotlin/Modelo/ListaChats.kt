package com.redsystem.proyectochatapp_kotlin.Modelo

class ListaChats {

    private var uid : String = ""

    constructor()

    constructor(uid: String) {
        this.uid = uid
    }

    fun getUid() : String?{
        return uid
    }

    fun setUid (uid: String?){
        this.uid = uid!!
    }
}