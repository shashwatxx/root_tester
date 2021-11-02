import 'dart:async';

import 'package:flutter/services.dart';

class RootTester {
  static const MethodChannel _channel = MethodChannel('root_tester');

  /// Returns true if the current app is running in rooted android deviced or iOS jailbreak.
  static Future<bool> get isDeviceRooted async {
    return await _channel.invokeMethod('isDeviceRooted');
  }
}
