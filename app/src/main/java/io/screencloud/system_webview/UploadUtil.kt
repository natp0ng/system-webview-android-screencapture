package io.screencloud.system_webview

import okhttp3.*
import java.io.File
import java.io.IOException

object UploadUtil {
    private var client = OkHttpClient()

    class UploadParams {
        var url: String? = null
        var headers: Array<Array<String>>? = null
    }

    fun uploadFile(params: UploadParams, file: File, mediaType: String) {

        val mediaTypeL = MediaType.parse(mediaType)

        val builder = Request.Builder()
        builder.url(params.url!!)

        val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("photo", "screenshot.png", RequestBody.create(mediaTypeL, file))
            .build()
        builder.post(body)

        // builder.post(RequestBody.create(mediaTypeL, file))

        for (hh in params.headers!!) {
            builder.addHeader(hh[0], hh[1])
        }

        val request = builder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                file.delete()
                println("Upload of file $file failed, error: ${e.message}")
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                file.delete()
                println("Upload of file $file completed, response: ${response.body()!!.string()}")
            }
        })
    }
}