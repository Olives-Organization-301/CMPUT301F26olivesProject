package com.example.code
/**
 * has the functions that will be shared across each feature and
 * to which FireBase repository implements
 * **/

// Interface was built with the help of Claude : https://claude.ai/chat/6b55aa79-e6e7-49f3-bdb6-21e1a4a4dd95

interface Repository<T : Any>{
    suspend fun add(item: T): String
    suspend fun update(id: String, item: T)
    suspend fun delete(id: String)
}