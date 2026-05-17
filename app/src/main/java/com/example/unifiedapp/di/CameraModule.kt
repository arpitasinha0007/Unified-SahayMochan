package com.example.unifiedapp.di

import android.content.Context
import com.example.unifiedapp.ui.Controller.CameraController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    fun provideCameraController(
        @ApplicationContext context: Context
    ): CameraController {
        return CameraController(context)
    }
}