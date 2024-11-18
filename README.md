# FoodicsTask
Android app that discovers nearby Bluetooth devices, allows the user to pair with a device, receives connections from other Bluetooth devices, and then enables simple data transferring (text messages) between paired devices.

Additionally, it stores previously paired devices to allow reconnecting again easily, cancels discovery operation after a timeout to avoid draining battery life, and it supports keeping device discovery operation active while the app is in background.
## Screenshots
<p align="center">
  <img src="https://github.com/user-attachments/assets/dd289063-a895-4c81-b4e6-6a6d68f2b48d" width="20%" />
  <img src="https://github.com/user-attachments/assets/355859a1-91f3-4a74-8f24-c49c5ccb217c" width="20%" />
  <img src="https://github.com/user-attachments/assets/a9e761e9-134a-4853-9b7f-4079d10aeb39" width="20%" />
  <img src="https://github.com/user-attachments/assets/1c825ef2-ee7a-4b1b-8ab2-6f6af1ddf6fd" width="20%" />
</p>

## APK for testing

## How to use
This app supports devices running Android 8 (API 26) or later versions.
1. You need 2 physical Android devices.
2. Install & Open the app on 2 devices.
3. Grant app's required permissions.
4. Assuming both devices are unpaired, then to connect device A with device B:
   - Make sure device B is discoverable and click on "Start Server" on device B.
   - On Device A click on "Start Scan" and tab on Device B item (name & mac address) from "Scanned devices" list.
   - Bluetooth pairing dialog with pairing code appears on both devices.
   - Click on "Pair", then you'll be navigated to a new screen where you can start sending & receiving messages between devices.
5. If devices are paired previously, then click on "Start Server" on one device and then click on that device item from "Paired Devices" list on the other device.
