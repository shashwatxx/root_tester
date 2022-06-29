package me.shashwatmishra.root_tester;


import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import android.content.Context;
import android.content.ContextWrapper;
import io.flutter.plugin.common.BinaryMessenger;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import me.shashwatmishra.root_tester.ConstantCollections;

/** RootTesterPlugin */
public class RootTesterPlugin implements FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private Context applicationContext;
  

  


  @Override
  public void onAttachedToEngine( FlutterPluginBinding binding) {
    onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
  }

  private void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
    this.applicationContext = applicationContext;
    channel = new MethodChannel(messenger, "root_tester");
    
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    applicationContext = null;
    channel.setMethodCallHandler(null);
    channel = null;
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("isDeviceRooted")) {
      if (isEmulator()) {
        result.success(false);
        return;
      }
      
      result.success(
        isPathExist("su")
        || isSUExist()
        || isTestBuildKey()
        || isHaveDangerousApps()
        || isHaveRootManagementApps()
        || isHaveDangerousProperties()
        || isHaveReadWritePermission()
      );
    } else {
      result.notImplemented();
    }
  }

  private boolean isEmulator() {
    return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            || Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || Build.PRODUCT.contains("sdk_google")
            || Build.PRODUCT.contains("google_sdk")
            || Build.PRODUCT.contains("sdk")
            || Build.PRODUCT.contains("sdk_x86")
            || Build.PRODUCT.contains("sdk_gphone64_arm64")
            || Build.PRODUCT.contains("vbox86p")
            || Build.PRODUCT.contains("emulator")
            || Build.PRODUCT.contains("simulator");
  }

  private boolean isPathExist(String ext){
    for(String path : ConstantCollections.superUserPath){
      String joinPath = path + ext;
      File file = new File(path, ext);
      if(file.exists()){
        Log.e("ROOT_CHECKER","Path is exist : "+joinPath);
        return true;
      }
    }
    return false;
  }

  private boolean isSUExist(){
    Process process = null;
    try{
      process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which","su"});
      BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
      if(in.readLine() != null){
        Log.e("ROOT_CHECKER","cammand executed");
        return true;
      }
      return false;
    }catch (Exception e){
      return false;
    }finally {
      if(process != null){
        process.destroy();
      }
    }
  }
  

  private boolean isTestBuildKey(){
    String buildTags = Build.TAGS;
    if(buildTags != null && buildTags.contains("test-keys")){
      Log.e("ROOT_CHECKER","devices buid with test key");
      return true;
    }
    return false;
  }

  private boolean isHaveDangerousApps(){
    ArrayList<String> packages = new ArrayList<String>();
    packages.addAll(Arrays.asList(ConstantCollections.dangerousListApps));
    return isAnyPackageFromListInstalled(packages);
  }

  private boolean isHaveRootManagementApps(){
    ArrayList<String> packages = new ArrayList<>();
    packages.addAll(Arrays.asList(ConstantCollections.rootsAppPackage));
    return isAnyPackageFromListInstalled(packages);
  }


  //check dangerous properties
  private boolean isHaveDangerousProperties(){
    final Map<String,String> dangerousProps = new HashMap<>();
    dangerousProps.put("ro.debuggable", "1");
    dangerousProps.put("ro.secure","0");

    boolean result = false;
    String[] lines = commander("getprop");
    if(lines == null){
      return false;
    }
    for(String line : lines){
      for(String key : dangerousProps.keySet()){
        if(line.contains(key)){
          String badValue = dangerousProps.get(key);
          badValue = "["+badValue+"]";
          if(line.contains(badValue)){
            Log.e("ROOT_CHECKER","Dangerous Properties with key : "+key +" and bad value : "+badValue);
            result =  true;
          }
        }
      }
    }
    return result;
  }

  //canChangePermission
  private boolean isHaveReadWritePermission(){
    Boolean result = false;
    String[] lines = commander("mount");

    for(String line : lines){
      String[] args = line.split(" ");
      if(args.length < 4){
        continue;
      }
      String mountPoint = args[1];
      String mountOptions = args[3];

      for(String path : ConstantCollections.notWritablePath){
        if(mountPoint.equalsIgnoreCase(path)){
          for(String opt : mountOptions.split(",")){
            if(opt.equalsIgnoreCase("rw")){
              Log.e("ROOT_CHECKER","Path : "+path+" is mounted with read write permission"+ line);
              result = true;
              break;
            }
          }
        }
      }
    }

    return result;
  }

  private String[] commander(String command){
    try{
      InputStream inputStream = Runtime.getRuntime().exec(command).getInputStream();
      if(inputStream == null){
        return  null;
      }
      String propVal = new Scanner(inputStream).useDelimiter("\\A").next();
      return propVal.split("\n");
    }catch(Exception e ){
      e.printStackTrace();
      return null;
    }
  }

  private boolean isAnyPackageFromListInstalled(ArrayList<String> pkg){
    boolean result = false;
    PackageManager pm =applicationContext.getPackageManager();
      // .getPackageManager();
    for(String packageName : pkg){
      try{
        pm.getPackageInfo(packageName,0);
        result = true;
      }catch(Exception e){

      }
    }
    return result;
  }

}
