package cloud.mindbox.mobile_sdk

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal class ExtensionsTest {

    @Test
    fun `converting zoned date time to string`() {
        val time: ZonedDateTime = ZonedDateTime.now()
        val expectedResult = time.withZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
        val actualResult = time.convertToString()
        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun `converting string to zoned date time`() {
        val time = "2023-01-27T14:13:29"
        val expectedResult: ZonedDateTime =
            LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")).atZone(
                ZoneOffset.UTC
            )
        val actualResult = time.convertToZonedDateTime()
        assertEquals(expectedResult, actualResult)
    }

    private val testPackageName = "com.test.app"
    private val customProcessName = "com.test.app:myprocess"
    private val context = mockk<Context> {
        every { packageName } returns testPackageName
    }

    @Test
    fun `isMainProcess if process resource empty`() {
        every { context.getString(any()) } returns ""
        assertTrue(context.isMainProcess(testPackageName))
    }

    @Test
    fun `isMainProcess if process resource blank`() {
        every { context.getString(any()) } returns " "
        assertTrue(context.isMainProcess(testPackageName))
    }

    @Test
    fun `isMainProcess if process resource testPackageName`() {
        every { context.getString(any()) } returns testPackageName
        assertTrue(context.isMainProcess(testPackageName))
    }

    @Test
    fun `isMainProcess if process resource start with testPackageName`() {
        every { context.getString(any()) } returns testPackageName + "sdfdsf"
        assertTrue(context.isMainProcess(testPackageName + ":" + testPackageName + "sdfdsf"))
    }

    @Test
    fun `isMainProcess if process resource text`() {
        every { context.getString(any()) } returns "myprocess"
        assertTrue(context.isMainProcess(customProcessName))
    }

    @Test
    fun `isMainProcess if process resource text start with two dots `() {
        every { context.getString(any()) } returns ":myprocess"
        assertTrue(context.isMainProcess(customProcessName))
    }

    @Test
    fun `isMainProcess if process resource text start with package name`() {
        every { context.getString(any()) } returns "com.test.app:myprocess"
        assertTrue(context.isMainProcess(customProcessName))
    }

    @Test
    fun `isMainProcess if process resource text start with text dot`() {
        mockkObject(Context::getCurrentProcessName)
        every { context.getString(any()) } returns "test.myprocess"
        assertTrue(context.isMainProcess("test.myprocess"))
    }
}