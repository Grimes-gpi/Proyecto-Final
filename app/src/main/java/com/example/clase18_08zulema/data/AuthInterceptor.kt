package com.example.clase18_08zulema.data

import okhttp3.Interceptor
import okhttp3.Response

/** Adjunta "Authorization: Bearer" a cada petición y cierra la sesión si el servidor responde 401. */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val isPublic = original.url.encodedPath.let { it.endsWith("/api/auth/login") || it.endsWith("/api/auth/register") }
        val token = SessionManager.token
        val request = if (token != null && !isPublic) {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        } else original

        val response = chain.proceed(request)
        if (response.code == 401 && token != null && !isPublic) SessionManager.expire()
        return response
    }
}
