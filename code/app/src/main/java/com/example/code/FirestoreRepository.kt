package com.example.code
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Base class that holds the real Firestore logic for add, update,
 * delete, and get. Subclasses don't implement these functions. They
 * inherit them by passing two things to the constructor:
 * - the collection name, such as "Admins" or "Campsites"
 * - the model class for that collection, such as Admin::class.java
 * The subclasses in this package are DatabaseAdmins, DatabaseCampsiteOwners,
 *  DatabaseCampsites, and DatabaseEntrants.
 * also utilizes the await function, which suspends the coroutine
 * if its task is not finished so the thread is free to run
 * other coroutines and when the task complete
 * coroutine starts at the line after await()
 * **/

// Class was built with the help of Claude
abstract class FirestoreRepository<T : Any>(
    collectionPath: String,
    private val itemClass: Class<T>
) : Repository<T> {

    private val collection = FirebaseFirestore.getInstance().collection(collectionPath)

    override suspend fun add(item: T): String =
        collection.add(item).await().id

    override suspend fun update(id: String, item: T) {
        collection.document(id).set(item).await()
    }

    override suspend fun delete(id: String) {
        collection.document(id).delete().await()
    }
    suspend fun get(id: String): T? =
        collection.document(id).get().await().toObject(itemClass)
}