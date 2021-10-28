import 'dart:async';

import 'package:flutter/services.dart';

class RootTester {
  static const MethodChannel _channel = MethodChannel('root_tester');

  static Future<bool> get isDeviceRooted async {
    return await _channel.invokeMethod('isDeviceRooted');
  }
}
