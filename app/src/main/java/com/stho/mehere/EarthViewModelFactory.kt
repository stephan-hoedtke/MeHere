package com.stho.mehere

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


class EarthViewModelFactory(private val application: Application, private val repository: Repository, private val settings: Settings) :
    ViewModelProvider.AndroidViewModelFactory(application) {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        val viewModel = when {
            modelClass.isAssignableFrom(EarthViewModel::class.java) -> EarthViewModel(application, repository, settings)
            else -> super.create(modelClass)
        }
        @Suppress("UNCHECKED_CAST")
        return viewModel as T
    }
}

fun EarthFragment.createEarthViewModel(): EarthViewModel {
    val settings = Settings(this.requireContext())
    val repository = Repository.requireRepository(this.requireContext())
    val application = this.requireActivity().application
    val viewModelFactory = EarthViewModelFactory(application, repository, settings)
    return ViewModelProvider(this, viewModelFactory).get(EarthViewModel::class.java)
}

