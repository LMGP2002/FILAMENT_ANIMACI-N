package com.example.filament

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



class MainActivity : AppCompatActivity() {

    var surfaceView: SurfaceView? = null
    var customViewer: CustomViewer = CustomViewer()

    //Variables Traductor
    private lateinit var editText: EditText
    private lateinit var button: Button
    private lateinit var textView: TextView
    var index=0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        surfaceView = findViewById<View>(R.id.surface_view) as SurfaceView
        customViewer.run {
            loadEntity()
            setSurfaceView(requireNotNull(surfaceView))

            //directory and model each as param
            loadGlb(this@MainActivity, "grogu", "tuverbienn")


            //Enviroments and Lightning (OPTIONAL)
            loadIndirectLight(this@MainActivity, "venetian_crossroads_2k")

        }
        //Instancias Traductor
        editText = findViewById(R.id.editText)
        button = findViewById(R.id.button)
        textView = findViewById(R.id.textView)

        button.setOnClickListener {
            val arrayAnimaciones = arrayOf("base", "bien", "tu", "ver")
            val palabrasBuscadas = arrayOf("tu", "ver", "bien") // Palabras que deseas buscar

            val posicionesEncontradas = mutableListOf<Int>()

            // Recorre el arreglo de búsqueda
            for (palabra in palabrasBuscadas) {
                // Busca la palabra en el arreglo de animaciones
                val indice = arrayAnimaciones.indexOf(palabra)
                // Si se encuentra la palabra, agrega su índice a las posiciones encontradas
                if (indice != -1) {
                    posicionesEncontradas.add(indice)
                }
            }

            Log.i("posiciones", ""+posicionesEncontradas)




            //-----------------------
            val handler = Handler(Looper.getMainLooper())
            val delay: Long = 1600
            var index = 0

            val runnable = object : Runnable {
                override fun run() {
                    if (posicionesEncontradas.isNotEmpty()) {

                        // Calcular la animación previa y su tiempo correspondiente
                        val animacionPrevia = if (index == 0) posicionesEncontradas.last() else posicionesEncontradas[index - 1]
                        val tiempoAnimacionPrevia = customViewer.getAnimationDuration(animacionPrevia)
                        customViewer.changeAnimation(posicionesEncontradas[index])
                        customViewer.applyCrossFade(animacionPrevia, tiempoAnimacionPrevia, 0f)

                        index = (index + 1) % posicionesEncontradas.size // Avanzar al siguiente índice en bucle
                        handler.postDelayed(this, delay)
                    }
                }
            }

// Iniciar la reproducción de las posiciones
            handler.postDelayed(runnable, delay)


            if (editText.text.toString().trim().isNotEmpty()) {
                val text = editText.text.toString()
                makePythonAPIRequest(text)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        customViewer.onResume()
    }

    override fun onPause() {
        super.onPause()
        customViewer.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        customViewer.onDestroy()
    }

    //Métodos del Traductor

    private fun makePythonAPIRequest(text: String) {
        // Ejecuta la corrutina en el hilo principal de la UI utilizando el GlobalScope
        GlobalScope.launch {
            val result = doInBackgroundAsync(text)
            // Actualizar la UI en el hilo principal utilizando withContext
            withContext(Dispatchers.Main) {
                handleResult(result)
            }
        }
    }

    private suspend fun doInBackgroundAsync(text: String): String =
        withContext(Dispatchers.IO) {
            val url = URL("http://192.168.0.16:5000/analizar?text=$text")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val stringBuilder = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }

            reader.close()
            stringBuilder.toString()
        }

    private fun handleResult(result: String) {
        var mejorClave = ""
        var similitudMaxima = ""

        val datos = result.split("_")
        if (datos.size >= 2) {
            mejorClave = datos[0]
            similitudMaxima = datos[1]
        }

        // Mostrar el resultado en el consola
        println("Coincide con: $mejorClave Similitud: $similitudMaxima")
        // Mostrar el resultado en el TextView (en el hilo principal)
        textView.text = "Coincide con: $mejorClave Similitud: $similitudMaxima"




    }
}