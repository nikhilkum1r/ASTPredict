package com.astpredict.app

import android.app.Application

class ASTApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ASTApplication
            private set
    }
}
