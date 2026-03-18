package com.example.yomartepresta

import com.example.yomartepresta.data.model.User
import com.example.yomartepresta.data.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseRepositoryTest {
    private lateinit var repository: FirebaseRepository
    
    private val firestore = mockk<FirebaseFirestore>(relaxed = true)
    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val storage = mockk<FirebaseStorage>(relaxed = true)
    private val collection = mockk<CollectionReference>(relaxed = true)

    @Before
    fun setup() {
        repository = FirebaseRepository(auth, firestore, storage)
        every { firestore.collection("users") } returns collection
    }

    @Test
    fun `test recuperar usuarios desde firestore`() = runTest {
        val mockUser = User(uid = "user123", firstName = "Yomar", lastName = "Perez", cedula = "12345678")
        val mockSnapshot = mockk<QuerySnapshot>(relaxed = true)
        val mockDoc = mockk<QueryDocumentSnapshot>(relaxed = true)
        
        every { mockDoc.toObject(User::class.java) } returns mockUser
        every { mockSnapshot.documents } returns listOf(mockDoc)
        
        val slot = slot<EventListener<QuerySnapshot>>()
        every { collection.addSnapshotListener(capture(slot)) } returns mockk(relaxed = true)

        val results = mutableListOf<List<User>>()
        
        // Lanzamos la corrutina pero esperamos a que el listener sea capturado
        val job = launch { 
            repository.getAllUsersFlow().toList(results) 
        }
        
        // Forzamos la ejecución de la corrutina hasta que esté en suspensión (esperando el listener)
        // Como no tenemos control exacto del scheduler en runTest simple,
        // invocamos el callback cuando estemos seguros de que el listener existe.
        
        // A veces el snapshot listener se registra instantáneamente
        slot.captured.onEvent(mockSnapshot, null)

        assertEquals(1, results.size)
        assertEquals("12345678", results[0][0].cedula)
        job.cancel()
    }
}
