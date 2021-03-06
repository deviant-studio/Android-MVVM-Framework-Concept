package ds.prodigy

import android.app.Activity
import android.content.Context
import android.databinding.BaseObservable
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import ds.prodigy.component.IComponent
import ds.prodigy.tools.L
import ds.prodigy.tools.LifecycleEvent
import rx.subjects.BehaviorSubject
import java.util.*

abstract class Presenter<C : IComponent> : BaseObservable() {
    val TAG = "P"

    companion object {
        var idGenerator: Long = 1
            get() = field++
    }

    var id = idGenerator
    open val navigator by lazy { Navigator(this) }
    var component: C? = null
    var dead = false
    var justCreated = true
    val results = mutableListOf<Result<Any>>()
    val lifecycleSignal: BehaviorSubject<LifecycleEvent> = BehaviorSubject.create()

    lateinit internal var context: Context  // app context
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// public api

    inline fun <reified T : Any> setCallback(owner:Presenter<*>,noinline cb: (T?) -> Unit) {
        val cls: Class<T> = T::class.java
        val result: Result<T> = Result(owner, cls, cb)
        results.add(result as Result<Any>)
    }

    inline fun <reified T : Any> setResult(result: T?) {
        try {
            val r: Result<in T> = results.first { T::class.java == it.resultType }
            r.result = result
        } catch (e: NoSuchElementException) {
            L.e(TAG, "Can't set result for null callback")
        }
    }

    fun isAttached() = component != null

    @Deprecated("")
    fun toast(text: String?) {
        Toast.makeText(component?.getContext(), text, Toast.LENGTH_SHORT).show()
    }

    fun getString(@StringRes id: Int) = context.getString(id)

    fun getColor(@ColorRes id: Int) = ContextCompat.getColor(context, id)

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// lifecycle and callbacks

    open fun onCreate(bundle: Bundle?) = Unit

    /** BindingAware has been attached to presenter */
    open fun onAttach() = Unit

    /** BindingAware has been detached from presenter */
    open fun onDetach() = Unit

    /** Presenter has been destroyed and shouldn't be used. dead flag is true from now */
    open fun onDestroy() = Unit

    open fun onStart() = Unit
    open fun onStop() = Unit
    open fun onResume() = Unit
    open fun onPause() = Unit

    // menu stuff
    open fun onOptionsItemSelected(item: MenuItem): Boolean = false

    open fun onPrepareOptionsMenu(menu: Menu): Boolean = true

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    internal fun getActivity(): Activity? {
        return component?.getActivity()
    }

    internal fun fireCallbacks() {
        results.forEach {
            L.d(TAG, "callback to ${it.owner.javaClass.simpleName} id=${it.owner.id} activity=${it.owner.getActivity()} result=${it.result}")
            if (!it.owner.dead /*&& it.owner.getActivity() != null*/) {
                it.callback.invoke(it.result)
            } else {
                L.w(TAG, "callback owner or its activity is dead")
            }
        }
        results.clear()
    }


}