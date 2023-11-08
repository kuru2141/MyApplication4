package com.example.myapplication

import android.os.AsyncTask
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class ChatActivity : AppCompatActivity() {

    private lateinit var chatbotResponseTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val sendButton = findViewById<Button>(R.id.sendButton)
        val inputEditText = findViewById<EditText>(R.id.inputEditText)
        chatbotResponseTextView = findViewById<TextView>(R.id.chatbotResponseTextView)

        sendButton.setOnClickListener {
            val userInput = inputEditText.text.toString()
            if (userInput.isNotBlank()) {
                SendMessageTask().execute(userInput)
                inputEditText.text.clear()
            }
        }
    }

    inner class SendMessageTask : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String?): String {
            val userInput = params[0]
            return sendMessageToChatbot(userInput)
        }

        override fun onPostExecute(result: String?) {
            try {
                if (!result.isNullOrBlank()) {
                    val jsonResponse = JSONObject(result)
                    val bubblesArray = jsonResponse.optJSONArray("bubbles")

                    if (bubblesArray != null && bubblesArray.length() > 0) {
                        val firstBubble = bubblesArray.getJSONObject(0)
                        val data = firstBubble.optJSONObject("data")

                        if (data != null) {
                            val description = data.optString("description")
                            chatbotResponseTextView.text = description ?: "No description found."
                        } else {
                            chatbotResponseTextView.text = "No 'data' field found in the first bubble."
                        }
                    } else {
                        chatbotResponseTextView.text = "No bubbles found in the response."
                    }
                } else {
                    chatbotResponseTextView.text = "Empty response."
                }
            } catch (e: Exception) {
                chatbotResponseTextView.text = "Error extracting description."
            }
        }
    }

    private fun sendMessageToChatbot(userInput: String?): String {
        if (userInput.isNullOrBlank()) {
            return "Error: User input is empty."
        }
        val url = "https://222zgn8i67.apigw.ntruss.com/chat/chatStage/" // 챗봇 주소 입력
        val secretKey = "TVh0aElJUndocVRqUXR3T3FuRWtMQ0tzRlF6YlhhYmY=" // 챗봇 Secret Key 입력
        val timestamp = System.currentTimeMillis() / 1000
        val requestBody = """
    {
        "version": "v2",
        "userId": "U47b00b58c90f8e47428af8b7bddcda3d1111111",
        "timestamp": $timestamp,
        "bubbles": [{"type": "text", "data": {"description": "$userInput"}}],
        "event": "send"
    }
""".trimIndent()

        try {
            val signature = makeSignature(secretKey, timestamp.toString(), requestBody)
            val contentType = "application/json; charset=utf-8".toMediaTypeOrNull()

            val request = Request.Builder()
                .url(url)
                .header("Content-Type", contentType.toString())
                .header("X-NCP-CHATBOT_SIGNATURE", signature)
                .post(requestBody.toRequestBody(contentType))
                .build()

            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            return response.body?.string() ?: "Empty response"
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error occurred: ${e.message}"
        }
    }


    private fun makeSignature(secretKey: String, timestamp: String, message: String): String {
        var encodeBase64String = ""

        try {
            val secreteKeyBytes = secretKey.toByteArray(StandardCharsets.UTF_8)

            val signingKey = SecretKeySpec(secreteKeyBytes, "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(signingKey)

            val rawHmac = mac.doFinal(message.toByteArray(StandardCharsets.UTF_8))
            encodeBase64String = Base64.getEncoder().encodeToString(rawHmac)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return encodeBase64String
    }
}