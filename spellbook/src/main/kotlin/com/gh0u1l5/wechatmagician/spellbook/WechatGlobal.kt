package com.gh0u1l5.wechatmagician.spellbook

import android.widget.Adapter
import android.widget.BaseAdapter
import com.gh0u1l5.wechatmagician.spellbook.SpellBook.getApplicationVersion
import com.gh0u1l5.wechatmagician.spellbook.base.Version
import com.gh0u1l5.wechatmagician.spellbook.base.WaitChannel
import com.gh0u1l5.wechatmagician.spellbook.util.BasicUtil.tryAsynchronously
import com.gh0u1l5.wechatmagician.spellbook.util.ReflectionUtil
import de.robv.android.xposed.callbacks.XC_LoadPackage
import net.dongliu.apk.parser.ApkFile
import java.lang.ref.WeakReference

/**
 * This singleton is the core part that analyzes and stores critical classes and objects of Wechat.
 * These classes and objects will be used for hooking and tampering with runtime data.
 */
object WechatGlobal {

    /**
     * A [WaitChannel] blocking all the evaluations until [WechatGlobal.init] has finished.
     */
    private val initializeChannel = WaitChannel()

    /**
     * A [Version] holding the version of current Wechat.
     */
    @Volatile var wxVersion: Version? = null
    /**
     * A string holding the package name of current Wechat process.
     */
    @Volatile var wxPackageName: String = ""
    /**
     * A class loader holding the classes provided by the Wechat APK.
     */
    @Volatile var wxLoader: ClassLoader? = null
    /**
     * A list holding a cache of full names for classes provided by the Wechat APK.
     */
    @Volatile var wxClasses: List<ReflectionUtil.ClassName>? = null

    /**
     * A flag indicating whether the codes are running under unit test mode.
     */
    @Volatile var wxUnitTestMode: Boolean = false

    // These are the cache of important global objects
    @Volatile var AddressAdapterObject: WeakReference<BaseAdapter?> = WeakReference(null)
    @Volatile var ConversationAdapterObject: WeakReference<BaseAdapter?> = WeakReference(null)
    @Volatile var SnsUserUIAdapterObject: WeakReference<Adapter?> = WeakReference(null)
    @Volatile var MsgStorageObject: Any? = null
    @Volatile var ImgStorageObject: Any? = null
    @Volatile var MainDatabaseObject: Any? = null
    @Volatile var SnsDatabaseObject: Any? = null

    /**
     * Creates a lazy object for dynamic analyzing. Its evaluation will be blocked by
     * the [initializeChannel] if the initialization is unfinished.
     *
     * @param name the name of the lazy field, which is used to print a helpful error message.
     * @param initializer the callback that actually initialize the lazy object.
     * @return a lazy object that can be used for lazy evaluation.
     */
    fun <T> wxLazy(name: String, initializer: () -> T?): Lazy<T> {
        return if (wxUnitTestMode) {
            UnitTestLazyImpl {
                initializer() ?: throw Error("Failed to evaluate $name")
            }
        } else {
            lazy {
                initializeChannel.wait(4000)
                initializer() ?: throw Error("Failed to evaluate $name")
            }
        }
    }

    class UnitTestLazyImpl<out T>(private val initializer: () -> T): Lazy<T>, java.io.Serializable {
        @Volatile private var lazyValue: Lazy<T> = lazy(initializer)

        fun refresh() {
            lazyValue = lazy(initializer)
        }

        override val value: T
            get() = lazyValue.value

        override fun toString(): String = lazyValue.toString()

        override fun isInitialized(): Boolean = lazyValue.isInitialized()
    }

    /**
     * Loads necessary information for static analysis into [WechatGlobal].
     *
     * @param lpparam the LoadPackageParam object that describes the current process, which should
     * be the same one passed to [de.robv.android.xposed.IXposedHookLoadPackage.handleLoadPackage].
     */
    @JvmStatic fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        tryAsynchronously {
            if (initializeChannel.isDone()) {
                return@tryAsynchronously
            }

            try {
                wxVersion = getApplicationVersion(lpparam.packageName)
                wxPackageName = lpparam.packageName
                wxLoader = lpparam.classLoader

                ApkFile(lpparam.appInfo.sourceDir).use {
                    wxClasses = it.dexClasses.map { clazz ->
                        ReflectionUtil.ClassName(clazz.classType)
                    }
                }
            } catch (t: Throwable) {
                // Ignore this one
            } finally {
                initializeChannel.done()
            }
        }
    }
}