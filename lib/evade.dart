// You have generated a new plugin project without
// specifying the `--platforms` flag. A plugin project supports no platforms is generated.
// To add platforms, run `flutter create -t plugin --platforms <platforms> .` under the same
// directory. You can also find a detailed instruction on how to add platforms in the `pubspec.yaml` at https://flutter.dev/docs/development/packages-and-plugins/developing-packages#plugin-platforms.

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class Evade {
  static const MethodChannel _channel = MethodChannel('evade');

  static Future<bool> evade(
      {bool requiresNetwork = false, Function? evad}) async {
    final bool result = await _channel
        .invokeMethod('evade', {"requiresNetwork": requiresNetwork});
    if (result == true) {
      if (evad != null) {
        evad();
      }
    }
    return result;
  }
}

class EvadeFutureBuilder extends StatefulWidget {
  EvadeFutureBuilder(
      {Key? key,
      required this.onSuccess,
      this.onError = const SizedBox(),
      this.requiresNetwork = false})
      : super(key: key);
  Widget onSuccess;
  Widget onError;
  bool requiresNetwork;

  @override
  State<EvadeFutureBuilder> createState() => _EvadeFutureBuilder();
}

class _EvadeFutureBuilder extends State<EvadeFutureBuilder> {
  @override
  Widget build(BuildContext context) {
    return FutureBuilder(
        future: Evade.evade(requiresNetwork: widget.requiresNetwork),
        builder: (context, snapshot) {
          if (!snapshot.hasData) {
            return Container();
          } else {
            if (snapshot.data == true) {
              return widget.onSuccess;
            } else {
              return widget.onError;
            }
          }
        });
  }
}
