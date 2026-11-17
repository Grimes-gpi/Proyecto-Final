package com.example.clase18_08zulema

import android.app.Application
import com.example.clase18_08zulema.data.SessionManager
import com.example.clase18_08zulema.data.WorkshopsApi
import com.example.clase18_08zulema.ui.ServerConfig

class TalleresApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
        WorkshopsApi.configure(ServerConfig.read(this))
    }
}
