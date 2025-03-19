package com.zachm.employeelogin.util

data class Employee(val name: String, val id: Int, var embeddings: MutableList<Embedding>, var lastTracked: Long = 0, var framesTracked: Int = 0) {
    init {
        //require(id in 10000 until 99999) { "Employee ID must be 6 digits." }
    }
}