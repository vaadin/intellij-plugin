//
// Ampli - A strong typed wrapper for your Analytics
//
// This file is generated by Amplitude.
// To update run 'ampli pull intellij'
//
// Required dependencies: com.amplitude:java-sdk:[1.8.0,2.0), org.json:json:20201115
// Tracking Plan Version: 1
// Build: 1.0.0
// Runtime: jre:kotlin-ampli
//
// [View Tracking Plan](https://data.amplitude.com/vaadin/IDE%20Plugins/events/main/latest)
//
// [Full Setup
// Instructions](https://data.amplitude.com/vaadin/IDE%20Plugins/implementation/intellij)
//

package com.amplitude.ampli

import com.amplitude.Amplitude
import com.amplitude.MiddlewareExtra
import com.amplitude.Plan
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

abstract class Event<E : Event<E>>(
    val eventType: String,
    val eventProperties: Map<String, Any?>?,
    val options: EventOptions?,
    private val eventFactory: (eventProperties: Map<String, Any?>?, options: EventOptions?) -> E
) {
    fun options(userId: String? = null, deviceId: String? = null): E {
        return this.options(
            EventOptions(
                userId = userId,
                deviceId = deviceId,
            ))
    }

    fun options(options: EventOptions): E {
        return this.eventFactory(this.eventProperties?.toMap(), options)
    }
}

class LoadOptions(
    val environment: Ampli.Environment? = null,
    val disabled: Boolean? = null,
    val client: LoadClientOptions? = null
)

class EventOptions(
    val userId: String? = null,
    val deviceId: String? = null,
    val timestamp: Long? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val appVersion: String? = null,
    val versionName: String? = null,
    val platform: String? = null,
    val osName: String? = null,
    val osVersion: String? = null,
    val deviceBrand: String? = null,
    val deviceManufacturer: String? = null,
    val deviceModel: String? = null,
    val carrier: String? = null,
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val dma: String? = null,
    val idfa: String? = null,
    val idfv: String? = null,
    val adid: String? = null,
    val androidId: String? = null,
    val language: String? = null,
    val partnerId: String? = null,
    val ip: String? = null,
    val price: Double? = null,
    val quantity: Int? = null,
    val revenue: Double? = null,
    val productId: String? = null,
    val revenueType: String? = null,
    val eventId: Int? = null,
    val sessionId: Long? = null,
    val insertId: String? = null,
    val plan: Plan? = null
)

class LoadClientOptions(val apiKey: String? = null, val instance: Amplitude? = null, val plan: Plan? = null)

val ampli = Ampli()

open class Ampli {
    companion object {
        val API_KEY: Map<Environment, String> = mapOf(Environment.IDEPLUGINS to "5332f8777b8ce7f12dcbf6c9d749488d")

        private val observePlan: Plan =
            Plan()
                .setBranch("main")
                .setSource("intellij")
                .setVersion("1")
                .setVersionId("ac62c3f9-e92b-4502-8a50-3fd8f13c4103")
    }

    enum class Environment {
        IDEPLUGINS
    }

    private var disabled: Boolean = false

    val isLoaded: Boolean
        get() {
            return this._client != null
        }

    private var _client: Amplitude? = null
    val client: Amplitude
        get() {
            this.isInitializedAndEnabled()
            return this._client!!
        }

    /** Options should have 'environment', 'client.api_key' or 'client.instance' */
    open fun load(options: LoadOptions) {
        this.disabled = options.disabled ?: false
        if (this.isLoaded) {
            System.err.println(
                "Warning: Ampli is already initialized. ampli.load() should be called once at application start up.")
            return
        }

        var apiKey = ""
        if (options.client?.apiKey != null) {
            apiKey = options.client.apiKey
        }
        if (options.environment != null) {
            apiKey = API_KEY[options.environment].toString()
        }

        when {
            options.client?.instance != null -> {
                this._client = options.client.instance
            }
            apiKey != "" -> {
                this._client = Amplitude.getInstance()
                this._client?.init(apiKey)
            }
            else -> {
                System.err.println("ampli.load() requires 'environment', 'client.apiKey', or 'client.instance'")
                return
            }
        }

        this._client?.setPlan(options.client?.plan ?: observePlan)

        // set IngestionMetadata with backwards compatibility, min Java SDK version 1.10.1.
        try {
            val clazz = Class.forName("com.amplitude.IngestionMetadata")
            val setSourceNameMethod = clazz.getMethod("setSourceName", String::class.java)
            val setSourceVersionMethod = clazz.getMethod("setSourceVersion", String::class.java)
            val ingestionMetadata = clazz.newInstance()
            setSourceNameMethod.invoke(ingestionMetadata, "jre-kotlin-ampli")
            setSourceVersionMethod.invoke(ingestionMetadata, "1.0.0")
            val setIngestionMetadata = Amplitude::class.java.getMethod("setIngestionMetadata", clazz)
            setIngestionMetadata.invoke(this._client, ingestionMetadata)
        } catch (e: ClassNotFoundException) {
            println("com.amplitude.IngestionMetadata is available starting from Java SDK 1.10.1 version")
        } catch (e: NoSuchMethodException) {
            println("com.amplitude.IngestionMetadata is available starting from Java SDK 1.10.1 version")
        } catch (e: SecurityException) {
            println("com.amplitude.IngestionMetadata is available starting from Java SDK 1.10.1 version")
        } catch (e: Exception) {
            System.err.println("Unexpected error when setting IngestionMetadata")
        }
    }

    open fun track(userId: String?, event: Event<*>, options: EventOptions? = null, extra: MiddlewareExtra? = null) {
        if (!isInitializedAndEnabled()) {
            return
        }
        val amplitudeEvent = this.createAmplitudeEvent(event.eventType, event.options, options, userId)
        amplitudeEvent.eventProperties = this.getEventPropertiesJson(event)

        this._client?.logEvent(amplitudeEvent, extra)
    }

    open fun identify(userId: String?, options: EventOptions? = null, extra: MiddlewareExtra? = null) {
        if (!this.isInitializedAndEnabled()) {
            return
        }
        val amplitudeEvent = this.createAmplitudeEvent("Identify", null, options, userId)

        this._client?.logEvent(amplitudeEvent, extra)
    }

    open fun flush() {
        if (!this.isInitializedAndEnabled()) {
            return
        }
        this._client?.flushEvents()
    }

    private fun createAmplitudeEvent(
        eventType: String,
        options: EventOptions?,
        overrideOptions: EventOptions?,
        overrideUserId: String?
    ): com.amplitude.Event {
        val event =
            com.amplitude.Event(
                eventType,
                overrideUserId ?: overrideOptions?.userId ?: options?.userId,
                overrideOptions?.deviceId ?: options?.deviceId)
        (overrideOptions?.timestamp ?: options?.timestamp)?.let { event.timestamp = it }
        (overrideOptions?.locationLat ?: options?.locationLat)?.let { event.locationLat = it }
        (overrideOptions?.locationLng ?: options?.locationLng)?.let { event.locationLng = it }
        (overrideOptions?.appVersion ?: options?.appVersion)?.let { event.appVersion = it }
        (overrideOptions?.versionName ?: options?.versionName)?.let { event.versionName = it }
        (overrideOptions?.platform ?: options?.platform)?.let { event.platform = it }
        (overrideOptions?.osName ?: options?.osName)?.let { event.osName = it }
        (overrideOptions?.osVersion ?: options?.osVersion)?.let { event.osVersion = it }
        (overrideOptions?.deviceBrand ?: options?.deviceBrand)?.let { event.deviceBrand = it }
        (overrideOptions?.deviceManufacturer ?: options?.deviceManufacturer)?.let { event.deviceManufacturer = it }
        (overrideOptions?.deviceModel ?: options?.deviceModel)?.let { event.deviceModel = it }
        (overrideOptions?.carrier ?: options?.carrier)?.let { event.carrier = it }
        (overrideOptions?.country ?: options?.country)?.let { event.country = it }
        (overrideOptions?.region ?: options?.region)?.let { event.region = it }
        (overrideOptions?.city ?: options?.city)?.let { event.city = it }
        (overrideOptions?.dma ?: options?.dma)?.let { event.dma = it }
        (overrideOptions?.idfa ?: options?.idfa)?.let { event.idfa = it }
        (overrideOptions?.idfv ?: options?.idfv)?.let { event.idfv = it }
        (overrideOptions?.adid ?: options?.adid)?.let { event.adid = it }
        (overrideOptions?.androidId ?: options?.androidId)?.let { event.androidId = it }
        (overrideOptions?.language ?: options?.language)?.let { event.language = it }
        (overrideOptions?.partnerId ?: options?.partnerId)?.let { event.partnerId = it }
        (overrideOptions?.ip ?: options?.ip)?.let { event.ip = it }
        (overrideOptions?.price ?: options?.price)?.let { event.price = it }
        (overrideOptions?.quantity ?: options?.quantity)?.let { event.quantity = it }
        (overrideOptions?.revenue ?: options?.revenue)?.let { event.revenue = it }
        (overrideOptions?.productId ?: options?.productId)?.let { event.productId = it }
        (overrideOptions?.revenueType ?: options?.revenueType)?.let { event.revenueType = it }
        (overrideOptions?.eventId ?: options?.eventId)?.let { event.eventId = it }
        (overrideOptions?.sessionId ?: options?.sessionId)?.let { event.sessionId = it }
        (overrideOptions?.insertId ?: options?.insertId)?.let { event.insertId = it }
        (overrideOptions?.plan ?: options?.plan)?.let { event.plan = it }
        return event
    }

    private fun isInitializedAndEnabled(): Boolean {
        if (!this.isLoaded) {
            System.err.println("Ampli is not yet initialized. Have you called `ampli.load()` on app start?")
            return false
        }
        return !this.disabled
    }

    private fun getEventPropertiesJson(event: Event<*>?): JSONObject? {
        if (event?.eventProperties == null) {
            return null
        }

        val json = JSONObject()

        event.eventProperties.entries.forEach { eventPropertyEntry ->
            val key = eventPropertyEntry.key
            val value = eventPropertyEntry.value

            try {
                value?.let { json.put(key, if (value.javaClass.isArray) getJsonArray(value) else value) }
                    ?: run { json.put(key, JSONObject.NULL) }
            } catch (e: JSONException) {
                System.err.println("Error converting properties to JSONObject: ${e.message}")
            }
        }

        return json
    }

    private fun getJsonArray(value: Any): JSONArray {
        return try {
            JSONArray(value)
        } catch (e: JSONException) {
            System.err.printf("Error converting value to JSONArray: %s%n", e.message)
            JSONArray()
        }
    }
}