package com.zachm.employeelogin.util

data class Employee(val name: String, var embeddings: MutableList<Embedding>, var currentTackID: Int = 0)