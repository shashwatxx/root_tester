package me.shashwatmishra.root_tester

import android.content.Context
import android.os.Build
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*

/** RootTesterPlugin  */
class RootTesterPlugin : FlutterPlugin, MethodCallHandler {

    /// The MethodChannel that will the communication between Flutter and native Android
    /// This local reference serves to register the plugin with the Flutter Engine
    // and unregister it when the Flutter Engine is detached from the Activity
    private var channel: MethodChannel? = null
    private var applicationContext: Context? = null

    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "root_tester")
        channel?.setMethodCallHandler(this)
        applicationContext = binding.applicationContext
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (call.method == "isDeviceRooted") {
            result.success(
                isPathExist
                        || isSUExist
                        || isTestBuildKey
                        || isHaveDangerousApps
                        || isHaveRootManagementApps
                        || isHaveDangerousProperties
                        || isHaveReadWritePermission
            )
        } else {
            result.notImplemented()
        }
    }

    private val isPathExist: Boolean
    get() {
        for (path in ConstantCollections.superUserPath) {
            val su = "su"
            val joinPath = path + su
            val file = File(path, su)
            if (file.exists()) {
                Log.i("ROOT_CHECKER", "Path is exist : $joinPath")
                return true
            }
        }
        return false
    }

    private val isSUExist: Boolean
        get() {
            var process: Process? = null
            return try {
                process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
                val `in` = BufferedReader(InputStreamReader(process.inputStream))
                if (`in`.readLine() != null) {
                    Log.i("ROOT_CHECKER", "command executed")
                    return true
                }
                false
            } catch (e: Exception) {
                false
            } finally {
                process?.destroy()
            }
        }

    private val isTestBuildKey: Boolean
        get() {
            val buildTags = Build.TAGS
            if (buildTags != null && buildTags.contains("test-keys")) {
                Log.i("ROOT_CHECKER", "devices build with test key")
                return true
            }
            return false
        }

    private val isHaveDangerousApps: Boolean
        get() {
            val packages = ArrayList<String>()
            packages.addAll(listOf(*ConstantCollections.dangerousListApps))
            return isAnyPackageFromListInstalled(packages)
        }

    private val isHaveRootManagementApps: Boolean
        get() {
            val packages = ArrayList<String>()
            packages.addAll(listOf(*ConstantCollections.rootsAppPackage))
            return isAnyPackageFromListInstalled(packages)
        }

    // Check dangerous properties
    private val isHaveDangerousProperties: Boolean
        get() {
            val dangerousProps: MutableMap<String, String> = HashMap()
            dangerousProps["ro.debuggable"] = "1"
            dangerousProps["ro.secure"] = "0"
            var result = false
            val lines = commander("getprop") ?: return false
            for (line in lines) {
                for (key in dangerousProps.keys) {
                    if (line.contains(key)) {
                        var badValue = dangerousProps[key]
                        badValue = "[$badValue]"
                        if (line.contains(badValue)) {
                            Log.e(
                                "ROOT_CHECKER",
                                "Dangerous Properties with key : $key and bad value : $badValue"
                            )
                            result = true
                        }
                    }
                }
            }
            return result
        }

    // Can change permission
    private val isHaveReadWritePermission: Boolean
        get() {
            var result = false
            commander("mount")?.let {
                for (line in it) {
                    val args = line.split(" ").toTypedArray()
                    if (args.size < 4) {
                        continue
                    }
                    val mountPoint = args[1]
                    val mountOptions = args[3]
                    for (path in ConstantCollections.notWritablePath) {
                        if (mountPoint.equals(path, ignoreCase = true)) {
                            for (opt in mountOptions.split(",").toTypedArray()) {
                                if (opt.equals("rw", ignoreCase = true)) {
                                    Log.e(
                                        "ROOT_CHECKER",
                                        "Path : $path is mounted with read write permission$line"
                                    )
                                    result = true
                                    break
                                }
                            }
                        }
                    }
                }
            }
            return result
        }

    private fun commander(command: String): Array<String>? {
        return try {
            Runtime.getRuntime().exec(command).inputStream?.let {
                Scanner(it)
                    .useDelimiter("\\A")
                    .next()
                    .split("\n").toTypedArray()
            }
        } catch (e: Exception) {
            Log.e("ROOT_CHECKER", e.stackTraceToString())
            null
        }
    }

    private fun isAnyPackageFromListInstalled(pkg: ArrayList<String>): Boolean {
        var result = false
        for (packageName in pkg) {
            try {
                applicationContext?.packageManager?.let {
                    it.getPackageInfo(packageName, 0)
                    result = true
                }
            } catch (e:Exception) {
                Log.e("ROOT_CHECKER", e.stackTraceToString())
            }
        }
        return result
    }
}