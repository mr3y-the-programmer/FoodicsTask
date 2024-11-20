package com.example.foodicstask.di

import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.foodicstask.data.DefaultBLEClientController
import com.example.foodicstask.data.DefaultBLEServerController
import com.example.foodicstask.data.DefaultBluetoothController
import com.example.foodicstask.domain.bluetooth.BLEClientController
import com.example.foodicstask.domain.bluetooth.BLEServerController
import com.example.foodicstask.domain.bluetooth.BluetoothController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBluetoothManager(@ApplicationContext context: Context): BluetoothManager {
        return context.getSystemService(BluetoothManager::class.java)
    }

    @Provides
    @Singleton
    fun provideBluetoothController(@ApplicationContext context: Context, bluetoothManager: BluetoothManager): BluetoothController {
        return DefaultBluetoothController(context, bluetoothManager)
    }

    @Provides
    @Singleton
    fun provideBLEClientController(@ApplicationContext context: Context, bluetoothManager: BluetoothManager): BLEClientController {
        return DefaultBLEClientController(context, bluetoothManager)
    }

    @Provides
    @Singleton
    fun provideBLEServerController(@ApplicationContext context: Context, bluetoothManager: BluetoothManager): BLEServerController {
        return DefaultBLEServerController(context, bluetoothManager)
    }
}
