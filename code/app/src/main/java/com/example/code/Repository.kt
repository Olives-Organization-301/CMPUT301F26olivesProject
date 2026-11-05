package com.example.code
/**
 * has the functions that will be shared across each feature and
 * to which FireBase repository implements
 * **/

// Interface was built with the help of Claude

interface Repository<T : Any>{
    suspend fun add(item: T): String
    suspend fun update(id: String, item: T)
    suspend fun delete(id: String)
}