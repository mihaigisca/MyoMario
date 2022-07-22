# MyoMario
Project represents an Android Studio application that connects to a Myo Armband named My Myo via a BLE foreground service.
The application is a 2D game that consists of 2 fragments:
- game fragment, objects move and the Y position of main character is dictated by the muscle contraction level
- data fragment, band-specific data is displayed

Project was tested with Android 6 (API 23) and 9 (API 28).

Much of the BLE service was encapsulated in this project: https://github.com/gmishka96/MyoAndroidBlePlugin

A project also targeting Myo Armband: https://github.com/gmishka96/MyoArmband3DMobile
