package com.material3tim.ui.screen.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QZoneWebFeedTest {
    @Test
    fun buildsOfficialUrlOnlyFromAValidNumericIdentity() {
        assertEquals("https://user.qzone.qq.com/123456789", qZoneWebEntry(" 123456789 "))
        assertEquals("https://i.qq.com/", qZoneWebEntry(""))
        assertEquals("https://i.qq.com/", qZoneWebEntry("uid_123456789"))
        assertEquals("https://i.qq.com/", qZoneWebEntry("12/34"))
    }

    @Test
    fun ownSpaceUsesMainWhileOtherProfilesKeepTheirDirectEntry() {
        assertEquals("https://user.qzone.qq.com/123/main", qZoneWebEntry("123", ownProfile = true))
        assertEquals("https://user.qzone.qq.com/123", qZoneWebEntry("123"))
        assertEquals("https://i.qq.com/", qZoneWebEntry("uid_123", ownProfile = true))
        assertEquals("https://user.qzone.qq.com/123/main", qZoneUpgradeProfileUrl("http://user.qzone.qq.com/123/main"))
    }

    @Test
    fun upgradesOnlyTheOfficialAuthorProfileUrl() {
        assertEquals("https://user.qzone.qq.com/123", qZoneUpgradeProfileUrl("http://user.qzone.qq.com/123?tracking=1"))
        assertEquals("123", qZoneProfileTarget("https://user.qzone.qq.com/123/main"))
        assertNull(qZoneUpgradeProfileUrl("http://user.qzone.qq.com.example.com/123"))
        assertNull(qZoneUpgradeProfileUrl("http://user.qzone.qq.com:8080/123"))
        assertNull(qZoneUpgradeProfileUrl("http://user.qzone.qq.com/cgi-bin/test"))
    }

    @Test
    fun upgradesLegacyQZoneAvatarHostsButRejectsOtherHosts() {
        assertEquals("https://qlogo3.store.qq.com/qzone/123/123/30", qZoneAvatarUrl("http://qlogo3.store.qq.com/qzone/123/123/30"))
        assertEquals("https://q1.qlogo.cn/g?s=100", qZoneAvatarUrl("https://q1.qlogo.cn/g?s=100"))
        assertNull(qZoneAvatarUrl("http://qlogo3.store.qq.com.evil.test/qzone/123"))
        assertNull(qZoneAvatarUrl("https://qlogo1.store.qq.com@evil.test/qzone/123"))
        assertNull(qZoneAvatarUrl("https://qlogo1.store.qq.com:8080/qzone/123"))
    }

    @Test
    fun trustsOnlyRecognizedHttpsMediaHosts() {
        assertEquals("https://a1.qpic.cn/p?ticket=x", qZoneMediaUrl("https://a1.qpic.cn/p?ticket=x", false))
        assertEquals("https://photovideo.photo.qq.com/v", qZoneMediaUrl("https://photovideo.photo.qq.com/v", true))
        assertNull(qZoneMediaUrl("https://a1.qpic.cn/v", true))
        assertNull(qZoneMediaUrl("https://a1.qpic.cn.example.com/p", false))
        assertNull(qZoneMediaUrl("http://a1.qpic.cn/p", false))
        assertNull(qZoneMediaUrl("blob:https://user.qzone.qq.com/x", true))
    }

    @Test
    fun permitsOfficialHttpsSpaceAndLoginOnly() {
        assertTrue(qZoneAllowsNavigation("https://user.qzone.qq.com/123/main"))
        assertTrue(qZoneAllowsNavigation("https://xui.ptlogin2.qq.com/cgi-bin/xlogin"))
        assertTrue(qZoneAllowsNavigation("https://i.qq.com/"))
        assertFalse(qZoneAllowsNavigation("http://user.qzone.qq.com/123/main"))
        assertFalse(qZoneAllowsNavigation("https://qzone.qq.com.example.com/"))
        assertFalse(qZoneAllowsNavigation("https://user.qzone.qq.com@example.com/"))
        assertFalse(qZoneAllowsNavigation("https://x.ptlogin2.qq.com:8080/"))
        assertFalse(qZoneAllowsNavigation("file:///sdcard/whatever"))
    }
}
