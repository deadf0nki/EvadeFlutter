package com.deafo.evade

import android.content.Context
import android.util.Log
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


/** EvadePlugin */
class EvadePlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    lateinit var context: Context
    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "evade")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == "evade") {
            this.context.runCatching {
                var requiresNetwork = call.argument<Boolean>("requiresNetwork")
                    GlobalScope.launch {
                        context.evade(requiresNetwork) {
                            result.success(true)
                        }?.onEscape {
                            result.success(false)
                        }
                    }
                
            }

        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
