package com.example.learnme.helper

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.io.IOException


class GPT4HelperTest {

    private lateinit var gpt4Helper: GPT4Helper
    private lateinit var mockClient: OkHttpClient
    private lateinit var mockCallback: (String) -> Unit

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Crear un mock de OkHttpClient y un callback
        mockClient = mock(OkHttpClient::class.java)
        mockCallback = mock<(String) -> Unit>()
        gpt4Helper = GPT4Helper(mockClient)
    }

    @Test
    fun `sendImageToGPT4 calls callback with generated label on successful response`() {
        val base64Image = "base64ImageString"
        val mockResponse = mock(Response::class.java)
        val responseBody = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "Etiqueta generada"
                        }
                    }
                ]
            }
        """.trimIndent()
        val mockResponseBody = responseBody.toResponseBody("application/json".toMediaTypeOrNull())

        // Configurar el comportamiento del cliente HTTP para devolver una respuesta simulada
        `when`(mockResponse.body).thenReturn(mockResponseBody)
        `when`(mockResponse.isSuccessful).thenReturn(true)

        val call = mock(Call::class.java)
        `when`(call.execute()).thenReturn(mockResponse)
        `when`(mockClient.newCall(any(Request::class.java))).thenReturn(call)

        gpt4Helper.sendImageToGPT4(base64Image) { label ->
            assertEquals("Etiqueta generada", label)
        }

        // Verificar que se llamó al callback con la etiqueta correcta
        verify(mockCallback).invoke("Etiqueta generada")
    }

    @Test
    fun `sendImageToGPT4 handles API failure`() {
        val base64Image = "base64ImageString"
        val call = mock(Call::class.java)

        // Configurar el cliente HTTP para simular una falla
        `when`(mockClient.newCall(any())).thenReturn(call)
        doAnswer {
            val callback = it.getArgument<Callback>(0)
            callback.onFailure(call, IOException("Network error"))
        }.`when`(call).enqueue(any())

        gpt4Helper.sendImageToGPT4(base64Image, mockCallback)

        // Verificar que no se invocó el callback en caso de falla
        verify(mockCallback, never()).invoke(any())
    }

    @Test
    fun `sendImageToGPT4 handles malformed JSON response`() {
        val base64Image = "base64ImageString"
        val malformedResponse = "{ invalid JSON }".toResponseBody("application/json".toMediaTypeOrNull())
        val mockResponse = mock(Response::class.java)

        // Configurar el cliente HTTP para devolver una respuesta con JSON malformado
        `when`(mockResponse.body).thenReturn(malformedResponse)
        `when`(mockResponse.isSuccessful).thenReturn(true)

        val call = mock(Call::class.java)
        `when`(call.execute()).thenReturn(mockResponse)
        `when`(mockClient.newCall(any())).thenReturn(call)

        gpt4Helper.sendImageToGPT4(base64Image, mockCallback)

        // Verificar que el callback no se invoca debido al error de parseo
        verify(mockCallback, never()).invoke(any())
    }
}
