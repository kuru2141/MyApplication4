package com.example.myapplication

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityAiBinding
import java.io.DataOutputStream
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

class AiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAiBinding
    private val boundary = "*****YOUR_BOUNDARY_STRING*****"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.homeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        binding.chatButton.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        binding.uploadButton.setOnClickListener {
            // 이미지 파일의 경로를 적절히 수정하세요.
            val filePath = "/path/to/your/image.jpg"
            UploadImageTask().execute(filePath)
        }
    }

    private inner class UploadImageTask : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
            val filePath = params[0]

            return try {
                val url = URL(SERVER_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Connection", "Keep-Alive")
                connection.setRequestProperty("ENCTYPE", "multipart/form-data")
                connection.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data;boundary=$boundary"
                )
                val outputStream = DataOutputStream(connection.outputStream)
                val fileInputStream = FileInputStream(filePath)
                val fileName = "image.jpg" // 이미지 파일 이름
                val lineEnd = "\r\n"
                val twoHyphens = "--"

                // 파일 파트 시작
                val delimiter = "$twoHyphens$boundary$lineEnd"
                outputStream.writeBytes(delimiter)
                outputStream.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"$fileName\"$lineEnd")
                outputStream.writeBytes(lineEnd)

                // 파일 본문 전송
                val bufferSize = 1024
                val buffer = ByteArray(bufferSize)
                var bytesRead: Int
                while (fileInputStream.read(buffer, 0, bufferSize).also { bytesRead = it } > 0) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.writeBytes(lineEnd)
                outputStream.writeBytes("$twoHyphens$boundary$twoHyphens$lineEnd")

                fileInputStream.close()
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    "Image uploaded successfully"
                } else {
                    "Failed to upload image. Response code: $responseCode"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Error occurred: ${e.message}"
            }
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            // UI 업데이트 코드를 여기에 추가 (예: 결과를 토스트 메시지로 보여주기)
            // 예: Toast.makeText(applicationContext, result, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val SERVER_URL = "http://your-flask-server/upload"
    }
}