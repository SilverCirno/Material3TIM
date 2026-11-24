package com.material3tim.xposed

import android.app.Activity
import android.content.Intent
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.OnBackCompletedFallback
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import java.util.Collections
import java.util.WeakHashMap

/**
 * TIM 的 Activity 既不是 LifecycleOwner 也不是 ViewModelStoreOwner（实测在 SplashActivity
 * 上 `activity is LifecycleOwner == false`），而 Compose 的 WindowRecomposer / 我们 UI 里的
 * `collectAsStateWithLifecycle` / NavigationBackHandler 都依赖这些 owner。
 *
 * 所以这里为宿主 Activity 造一套 owner，并挂到 decorView 的 ViewTree 上。
 */
internal class HostOwners(private val activity: Activity) :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    OnBackPressedDispatcherOwner,
    NavigationEventDispatcherOwner,
    ActivityResultRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)

    /** 把 ActivityResultContract 的启动转给宿主 Activity，结果由 hook 到的 onActivityResult 派发回来。 */
    private val resultRegistry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            val intent = contract.createIntent(activity, input)
            // TIM 的 QBaseActivity 重写了 startActivityForResult，会走「即将离开 TIM」安全确认弹窗，
            // 无法直接拿到结果；优先调用它提供的 startActivityForResultOriginal，
            // 直接走 Activity 原生实现（照片/拍照/文件选择器需要这个路径）。
            // TIM 会对外跳弹「即将离开 TIM」确认框；标记一下，由 TimUiHook 直接放行这一次。
            UiHost.expectExternalLaunch()
            if (options != null) {
                activity.startActivityForResult(intent, requestCode, options.toBundle())
            } else {
                activity.startActivityForResult(intent, requestCode)
            }
        }
    }

    override val activityResultRegistry: ActivityResultRegistry get() = resultRegistry

    /** 宿主 Activity 的 onActivityResult/doOnActivityResult 被 hook 后调用这里派发结果。 */
    fun dispatchActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        runCatching { resultRegistry.dispatchResult(requestCode, resultCode, data) }
    }
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    /** 只在 UI 未处理返回键时兜底调用；正常情况下不会走到这里。 */
    private val dispatcher = OnBackPressedDispatcher { activity.finish() }

    /** 用它在 Activity.onBackPressed 里手动派发一次返回事件。 */
    private val directInput = DirectNavigationEventInput()

    /** 没有任何 Compose 返回处理器时，dispatcher 会走 fallback —— 据此判断是否消费了返回键。 */
    private val backFallbackUsed = java.util.concurrent.atomic.AtomicBoolean(false)

    /** 平台预测性返回手势是否已经开始（决定 onBackInvoked 是「完成手势」还是「按键返回」）。 */
    private var platformBackInProgress = false

    /** Compose 的 NavigationBackHandler / NavDisplay 依赖它。 */
    override val navigationEventDispatcher: NavigationEventDispatcher =
        NavigationEventDispatcher(OnBackCompletedFallback { backFallbackUsed.set(true) })

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val onBackPressedDispatcher: OnBackPressedDispatcher get() = dispatcher

    private var initialized = false
    private var destroyed = false

    fun init() {
        if (initialized) return
        initialized = true
        runCatching { savedStateController.performRestore(null) }
        runCatching { navigationEventDispatcher.addInput(directInput) }
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun onPause() {
        if (!initialized || destroyed) return
        // 若用户在手势返回进行中切后台，取消这次预测性返回，避免恢复后卡在半路。
        if (platformBackInProgress) {
            platformBackInProgress = false
            runCatching { directInput.backCancelled() }
        }
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun onStop() {
        if (!initialized || destroyed) return
        if (platformBackInProgress) {
            platformBackInProgress = false
            runCatching { directInput.backCancelled() }
        }
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onResume() {
        if (!initialized || destroyed) return
        if (lifecycleRegistry.currentState == Lifecycle.State.RESUMED) return
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        android.util.Log.i("Material3Tim", "compose lifecycle -> RESUMED")
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        if (initialized) {
            runCatching { lifecycleRegistry.currentState = Lifecycle.State.DESTROYED }
            runCatching { navigationEventDispatcher.removeInput(directInput) }
        }
        runCatching { store.clear() }
    }

    // ---- 平台预测性返回（OnBackAnimationCallback）的进度转接 ----

    fun platformBackStarted(progress: Float, swipeEdge: Int, touchX: Float, touchY: Float) {
        if (!initialized || destroyed) return
        platformBackInProgress = true
        backFallbackUsed.set(false)
        runCatching {
            directInput.backStarted(
                NavigationEvent(swipeEdge, progress, touchX, touchY, android.os.SystemClock.uptimeMillis()),
            )
        }
    }

    fun platformBackProgressed(progress: Float, touchX: Float, touchY: Float) {
        if (!initialized || destroyed) return
        runCatching {
            directInput.backProgressed(
                NavigationEvent(NavigationEvent.EDGE_NONE, progress, touchX, touchY, android.os.SystemClock.uptimeMillis()),
            )
        }
    }

    /** 系统手势完成：让 Compose 返回处理器真正 pop；没人处理则自己退到后台。 */
    fun platformBackInvoked() {
        if (!initialized || destroyed) return
        if (!platformBackInProgress) {
            // 按键（三键导航 / 实体返回键）也会走 OnBackInvoked，但没有 started/progressed 阶段。
            platformBackInProgress = false
            dispatchBack()
            return
        }
        platformBackInProgress = false
        backFallbackUsed.set(false)
        runCatching { directInput.backCompleted() }
        val handled = !backFallbackUsed.get()
        android.util.Log.i("Material3Tim", "platformBackInvoked handled=$handled")
        if (!handled) {
            runCatching { activity.moveTaskToBack(true) }
        }
    }

    fun platformBackCancelled() {
        if (!initialized || destroyed) return
        platformBackInProgress = false
        runCatching { directInput.backCancelled() }
    }

    /** 平台没有预测性返回回调（老系统/未 opt-in）时的兜底入口。 */
    private fun legacyDispatchBack(): Boolean {
        return try {
            backFallbackUsed.set(false)
            directInput.backStarted(NavigationEvent(NavigationEvent.EDGE_NONE, 0f))
            directInput.backProgressed(NavigationEvent(NavigationEvent.EDGE_NONE, 1f))
            directInput.backCompleted()
            !backFallbackUsed.get()
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * 处理一次返回：先交给 Compose 的返回处理器；没人处理时（比如已经在根页面）
     * 我们**自己**把任务退到后台（等价于普通 App 在首页按返回），并且始终返回 true，
     * 避免 TIM 自己的隐藏 Fragment 逻辑接管后直接把 Activity finish 掉。
     */
    fun dispatchBack(): Boolean {
        if (!initialized || destroyed) return false
        val handled = legacyDispatchBack()
        android.util.Log.i("Material3Tim", "dispatchBack handled=$handled")
        if (!handled) {
            runCatching { activity.moveTaskToBack(true) }
        }
        return true
    }

    companion object {
        private val owners: MutableMap<Activity, HostOwners> =
            Collections.synchronizedMap(WeakHashMap<Activity, HostOwners>())

        fun get(activity: Activity): HostOwners? = owners[activity]

        fun obtain(activity: Activity): HostOwners =
            owners.getOrPut(activity) { HostOwners(activity) }

        fun remove(activity: Activity) {
            owners.remove(activity)?.destroy()
        }
    }
}
