package com.material3tim.xposed

import com.material3tim.data.model.chatVideoStreamUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatVideoSourceTest {
    @Test
    fun selectsCompleteHttpsUrlAndPreservesItsParameters() {
        val expected = "https://video.example.test/path/file.mp4?key=abc%2F123&expires=42"
        assertEquals(expected, chatVideoStreamUrl("http://video.example.test/path/file.mp4", expected))
        assertEquals(expected, chatVideoStreamUrl(expected, "https://other.example.test/video.mp4"))
    }

    @Test
    fun ignoresBareDomainPlaintextAndInvalidNativeResponse() {
        assertNull(chatVideoStreamUrl(null, "video.example.test", "http://video.example.test/v"))
        assertNull(chatVideoStreamUrl("https://", "file:///sdcard/test.mp4", "https://user@example.test/v"))
    }
}
