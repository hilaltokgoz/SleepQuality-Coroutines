/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.launch

/**
 * ViewModel for SleepTrackerFragment.
 */
//Class AndroidViewModel'ı extend almıştır.Bu sınıf Vm ile aynı fakat application context'i  yapıcı parametresi olarak alır.
class SleepTrackerViewModel(
    val database: SleepDatabaseDao,
    application: Application
) : AndroidViewModel(application) {

    private var tonight = MutableLiveData<SleepNight?>()
    private val nights = database.getAllNights() //tüm geceleri nights e ata.
    //LiveData & encapsulation
    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()  //navigation' ı tetikleyen değişken.
    val navigateToSleepQuality: LiveData<SleepNight> get() = _navigateToSleepQuality
    //Resetleme
    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    val nightsString = Transformations.map(nights) { nights ->   //nights i Stringe çevir.
        formatNights(nights, application.resources)
    }
    init {
        initializeTonight()
    }
    private fun initializeTonight() {
        viewModelScope.launch {    //ViewModelScope'ta Coroutine başlatmak için kullanılır. //CoroutineBuilder a lambda yollanıyor{}
            tonight.value = getTonightDatabase()
        }
    }
    private suspend fun getTonightDatabase(): SleepNight? {
        var night = database.getTonight()  //VT'nından en yeni geceyi alır.
        if (night?.endTimeMilli != night?.startTimeMilli) {
            night = null  //gece tamamlanmışsa null döndür.
        }
        return night  //hala o gecedeyse night döndür
    }
    ///CLİCK HANDLERS
    fun onStartTracking() {
        viewModelScope.launch {//Coroutine için başlatıldı
            val newNight = SleepNight()
            insert(newNight) //newNight VT nına kaydetmek için
            tonight.value = getTonightDatabase() //tonight güncelle
        }
    }
    private suspend fun insert(night: SleepNight) {
        database.insert(night)// night eklemek için DAO kullanın.
    }
    fun onStopTracking() {
        viewModelScope.launch {
            val oldNight = tonight.value ?: return@launch
            oldNight.endTimeMilli = System.currentTimeMillis()
            update(oldNight)
            _navigateToSleepQuality.value = oldNight
        }
    }
    private suspend fun update(night: SleepNight) {
        database.update(night)
    }
    fun onClear() {
        viewModelScope.launch {
            clear()
            tonight.value = null
        }
    }
    suspend fun clear() {
        database.clear()
    }
}

