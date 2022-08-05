package com.stho.mehere

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NewPointDialogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Repository.requireRepository(application)

    fun create(point: PointOfInterest) {
        repository.create(point)
    }

    fun update(id: String, point: PointOfInterest) {
        repository.update(id, point);
    }

    fun delete(id: String) {
        repository.delete(id);
    }

}


