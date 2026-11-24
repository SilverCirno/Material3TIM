package com.material3tim.xposed

import android.content.SharedPreferences

/**
 * 宿主（TIM 进程）里的设置存储门面。
 *
 * - **读**：优先读模块 App 的远端偏好（LSPosed `getRemotePreferences`，只读）；
 *   本次进程内改过的 key 以本地覆盖为准。
 * - **写**：宿主侧拿不到可写的远端偏好，写进 TIM 进程本地的 `material3tim_host`
 *   偏好文件（并通知监听者），当前进程内立即生效；重启 TIM 后以模块 App 的设置为准。
 *
 * 这样第二步先保证「界面能跑、设置页能切主题」，跨进程写回模块 App 留到后面的步骤
 * 用广播补上。
 */
internal class HostSettingsPrefs(
    private val remote: SharedPreferences?,
    private val local: SharedPreferences,
) : SharedPreferences {

    /** 本次进程内写过的 key：读的时候优先于远端。 */
    private val overridden = HashSet<String>()

    private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): Map<String, *> {
        val merged = LinkedHashMap<String, Any?>()
        remote?.all?.let { merged.putAll(it) }
        merged.putAll(local.all)
        return merged
    }

    override fun getString(key: String, defValue: String?): String? =
        if (readLocal(key)) local.getString(key, defValue) else remote?.getString(key, defValue) ?: defValue

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        if (readLocal(key)) local.getStringSet(key, defValues) else remote?.getStringSet(key, defValues) ?: defValues

    override fun getInt(key: String, defValue: Int): Int =
        if (readLocal(key)) local.getInt(key, defValue) else remote?.getInt(key, defValue) ?: defValue

    override fun getLong(key: String, defValue: Long): Long =
        if (readLocal(key)) local.getLong(key, defValue) else remote?.getLong(key, defValue) ?: defValue

    override fun getFloat(key: String, defValue: Float): Float =
        if (readLocal(key)) local.getFloat(key, defValue) else remote?.getFloat(key, defValue) ?: defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        if (readLocal(key)) local.getBoolean(key, defValue) else remote?.getBoolean(key, defValue) ?: defValue

    override fun contains(key: String): Boolean =
        local.contains(key) || remote?.contains(key) == true

    /**
     * 宿主内写过的设置优先读 TIM 本地副本（模块 App ↔ TIM 的跨进程同步留到后续步骤）；
     * 没写过才回落到模块 App 的远端设置。
     */
    private fun readLocal(key: String): Boolean = local.contains(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        listeners.remove(listener)
    }

    private fun notifyChanged(keys: Collection<String>) {
        if (keys.isEmpty()) return
        val snapshot = synchronized(listeners) { listeners.toList() }
        snapshot.forEach { listener -> keys.forEach { listener.onSharedPreferenceChanged(this, it) } }
    }

    private inner class Editor : SharedPreferences.Editor {

        private val pending = LinkedHashMap<String, Any?>()
        private var clearRequested = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor =
            put(key, value)

        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor =
            put(key, values)

        override fun putInt(key: String, value: Int): SharedPreferences.Editor = put(key, value)

        override fun putLong(key: String, value: Long): SharedPreferences.Editor = put(key, value)

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = put(key, value)

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = put(key, value)

        private fun put(key: String, value: Any?): SharedPreferences.Editor {
            pending[key] = value
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            pending[key] = REMOVE
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            return this
        }

        override fun commit(): Boolean {
            val localEditor = local.edit()
            if (clearRequested) {
                localEditor.clear()
                overridden.clear()
            }
            pending.forEach { (key, value) ->
                if (value === REMOVE) {
                    localEditor.remove(key)
                    overridden.remove(key)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    when (value) {
                        null -> localEditor.putString(key, null)
                        is String -> localEditor.putString(key, value)
                        is Int -> localEditor.putInt(key, value)
                        is Long -> localEditor.putLong(key, value)
                        is Float -> localEditor.putFloat(key, value)
                        is Boolean -> localEditor.putBoolean(key, value)
                        is Set<*> -> localEditor.putStringSet(key, value as Set<String>)
                    }
                    overridden.add(key)
                }
            }
            val ok = localEditor.commit()
            notifyChanged(pending.keys.filter { pending[it] !== REMOVE })
            return ok
        }

        override fun apply() {
            commit()
        }
    }

    private companion object {
        val REMOVE = Any()
    }
}
