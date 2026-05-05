package com.horizon.keyboard

import android.content.Context
import com.horizon.keyboard.data.SecureKeyStore as DataSecureKeyStore

/**
 * Compatibility shim — delegates to [com.horizon.keyboard.data.SecureKeyStore].
 *
 * Existing code keeps working unchanged. Will be deleted when all
 * references are updated to import directly from the data package.
 */
object SecureKeyStore {

    fun init(context: Context) = DataSecureKeyStore.init(context)

    fun putString(context: Context, key: String, value: String) =
        DataSecureKeyStore.putString(context, key, value)

    fun getString(context: Context, key: String, default: String = "") =
        DataSecureKeyStore.getString(context, key, default)

    fun hasKey(context: Context, key: String) =
        DataSecureKeyStore.hasKey(context, key)

    fun removeKey(context: Context, key: String) =
        DataSecureKeyStore.removeKey(context, key)

    fun setGroqKey(context: Context, key: String) =
        DataSecureKeyStore.setGroqKey(context, key)

    fun getGroqKey(context: Context) =
        DataSecureKeyStore.getGroqKey(context)

    fun hasGroqKey(context: Context) =
        DataSecureKeyStore.hasGroqKey(context)

    fun setGemmaKey(context: Context, key: String) =
        DataSecureKeyStore.setGemmaKey(context, key)

    fun getGemmaKey(context: Context) =
        DataSecureKeyStore.getGemmaKey(context)

    fun hasGemmaKey(context: Context) =
        DataSecureKeyStore.hasGemmaKey(context)
}
