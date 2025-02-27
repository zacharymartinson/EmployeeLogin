package com.zachm.employeelogin

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel : ViewModel() {

    val login: MutableLiveData<Boolean> by lazy {MutableLiveData<Boolean>(false)}
}