package database

import org.junit.Assert
import org.junit.Test

open class A
class B : A()
class KotlinTests {
    @Test
    fun testListSubs() {
        Assert.assertTrue(B() is A)
    }
}