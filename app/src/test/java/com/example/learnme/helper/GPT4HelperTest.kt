package com.example.learnme.helper

import com.example.learnme.config.GPTConfig
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GPT4HelperTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var gpt4Helper: GPT4Helper
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Usar la URL del servidor mock en lugar de la real
        GPTConfig.BASE_URL = mockWebServer.url("/").toString()

        okHttpClient = OkHttpClient()
        gpt4Helper = GPT4Helper(okHttpClient)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    //Cuando se envía una imagen a la API de GPT-4, se espera una respuesta válida
    fun `sendImageToGPT4 should parse response correctly`() = runBlocking {
        val imagePath = "/path/to/sample_image.jpg"

        val mockResponse = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "Etiqueta generada"
                        }
                    }
                ]
            }
        """
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
        )

        var resultLabel: String? = null

        gpt4Helper.sendImageToGPT4(imagePath) { label ->
            resultLabel = label
        }

        // Esperar a que se procese la solicitud (simulación)
        Thread.sleep(1000)

        // Verificar que la respuesta se procesó correctamente
        assertEquals("Etiqueta generada", resultLabel)
    }

    @Test
    //sendImageToGPT4 debería manejar errores de la API
    fun `sendImageToGPT4 should handle API errors`() = runBlocking {
        // Configurar una respuesta de error para el servidor
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        var resultLabel: String? = null

        gpt4Helper.sendImageToGPT4("path/to/mock/image.jpg") { label ->
            resultLabel = label
        }

        // Esperar a que se procese la solicitud (simulación)
        Thread.sleep(1000)

        // Verificar que no se obtuvo ninguna etiqueta
        assertNull(resultLabel)
    }

    @Test
    //sendImageToGPT4 debe manejar una respuesta no válida
    fun `sendImageToGPT4 should handle invalid JSON response`() = runBlocking {
        // Configurar una respuesta con JSON no válido
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("Invalid JSON")
        )

        var resultLabel: String? = null

        gpt4Helper.sendImageToGPT4("path/to/mock/image.jpg") { label ->
            resultLabel = label
        }

        // Esperar a que se procese la solicitud (simulación)
        Thread.sleep(1000)

        // Verificar que no se obtuvo ninguna etiqueta
        assertNull(resultLabel)
    }
}
