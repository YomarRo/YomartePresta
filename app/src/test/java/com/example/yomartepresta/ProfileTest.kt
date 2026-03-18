package com.example.yomartepresta

import com.example.yomartepresta.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ProfileTest {

    @Test
    fun `verificar que el usuario tiene datos cargados`() {
        val user = User(
            uid = "user123",
            firstName = "Yomar",
            lastName = "Perez",
            phone = "04141234567",
            bank = "Banesco",
            pagoMovilId = "04141234567"
        )
        
        // Verificamos que los campos no estén vacíos
        assertNotEquals("", user.phone)
        assertNotEquals("", user.bank)
        assertEquals("04141234567", user.phone)
        assertEquals("Banesco", user.bank)
    }
}
