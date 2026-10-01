import 'package:audioplayers/audioplayers.dart';
import 'package:vibration/vibration.dart';

/// Signal instantané "bam" : carillon + vibration.
class NotifySignal {
  static final AudioPlayer _player = AudioPlayer();

  static Future<void> bam() async {
    try {
      await _player.stop();
      await _player.play(AssetSource('sounds/notify.wav'));
    } catch (_) {}
    try {
      if (await Vibration.hasVibrator()) {
        await Vibration.vibrate(duration: 180);
      }
    } catch (_) {}
  }
}
