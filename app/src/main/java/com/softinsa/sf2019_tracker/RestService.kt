package com.softinsa.sf2019_tracker

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit


class RestServiceWeb {
    val service: apiService

    init {
        val client = OkHttpClient.Builder()
            .addInterceptor(BasicAuthInterceptor(USER, PASS))
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        service = retrofit.create(apiService::class.java)
    }

    companion object {
        public val URL = "https://smarterfest-dev-diogo-grunho.eu-gb.mybluemix.net/web/"
        public val USER = "webapp"
        public val PASS = "tabuleiros_web_2015"
    }
}

class RestServiceMobile {
    val service: apiService

    init {
        val client = OkHttpClient.Builder()
            .addInterceptor(BasicAuthInterceptor(USER, PASS))
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        service = retrofit.create(apiService::class.java)
    }

    companion object {
        public val URL = "https://smarterfest-dev-diogo-grunho.eu-gb.mybluemix.net/mobile/"
        public val USER = "mobile"
        public val PASS = "mobile_tabuleiros_2015"
    }
}

