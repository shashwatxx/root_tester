import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:root_tester/root_tester.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool _isRooted = false;

  @override
  void initState() {
    super.initState();
    initRootTester();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initRootTester() async {
    bool isRooted;
    try {
      isRooted = await RootTester.isDeviceRooted;
    } on PlatformException {
      isRooted = false;
    }
    if (!mounted) return;

    setState(() {
      _isRooted = isRooted;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('root_tester'),
        ),
        body: Center(
          child: Text('This Device Root Status is: $_isRooted\n'),
        ),
      ),
    );
  }
}
