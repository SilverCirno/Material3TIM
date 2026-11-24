package com.material3tim.data.repository

import com.material3tim.data.model.ContactItem
import com.material3tim.data.source.AppDataSources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 真实好友列表（宿主里由 TIM BuddyService 填充；独立 App 用示例数据）。 */
object LiveContacts {

    @Volatile
    private var live: List<ContactItem>? = null

    private val _flow = MutableStateFlow<List<ContactItem>?>(null)
    val flow: StateFlow<List<ContactItem>?> = _flow.asStateFlow()

    val isLive: Boolean
        get() = live != null

    val current: List<ContactItem>
        get() = live ?: ChatRepository.sampleContacts

    fun peek(): List<ContactItem>? = live

    fun update(contacts: List<ContactItem>?) {
        live = contacts
        _flow.value = contacts
    }

    fun refresh(): List<ContactItem>? {
        val source = AppDataSources.contactSource ?: return null
        val contacts = runCatching { source.loadContacts() }.getOrNull() ?: return null
        update(contacts)
        return contacts
    }

    /** 按首字母分组（A-Z 优先，其它归到 #）。 */
    fun grouped(contacts: List<ContactItem> = current): List<Pair<Char, List<ContactItem>>> =
        contacts
            .groupBy { it.letter }
            .toSortedMap(compareBy { if (it in 'A'..'Z') it else 'Z' + 1 })
            .map { (letter, list) -> letter to list.sortedBy { it.name } }
}
