package com.example.myapplication.ui

import android.net.Uri
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.myapplication.model.ArchivoItem

class MainViewModel : ViewModel() {

    var selectedTab = mutableIntStateOf(1)
        private set

    val archivos = mutableStateListOf<ArchivoItem>()

    fun selectTab(tab: Int) {
        selectedTab.intValue = tab
    }

    fun agregarArchivo(nombre: String, uri: Uri?) {
        archivos.add(ArchivoItem(nombre = nombre, uri = uri))
    }

    fun eliminarArchivo(archivo: ArchivoItem) {
        archivos.remove(archivo)
    }
}
