# GTR 2e Battery App

A simple Android app to connect to the Amazfit GTR 2e smartwatch, authenticate using the Huami protocol, and fetch battery status.

## How It Works

This app connects to the GTR 2e, performs the Huami authentication challenge/response, and reads the battery information characteristic. The authentication protocol is based on the challenge/response method (no key upload after initial pairing).

### BLE Authentication Flow

1. **Connect to Device**
   - The app connects to the GTR 2e using its MAC address.

2. **Request MTU**
   - Requests MTU size 247 for optimal BLE throughput.

3. **Service Discovery**
   - Discovers all services and characteristics.

4. **Enable Notifications on Auth Characteristic**
   - Enables notifications on the Auth characteristic:
     - **Service UUID:** `0000fee1-0000-1000-8000-00805f9b34fb`
     - **Characteristic UUID:** `00000009-0000-3512-2118-0009af100700`
     - **Descriptor UUID (CCCD):** `00002902-0000-1000-8000-00805f9b34fb`

5. **Send Auth Challenge Request**
   - Write the following bytes to the Auth characteristic:
     - `[0x82, 0x00, 0x02, 0x01, 0x00]`
   - This requests a random challenge from the device.

6. **Handle Challenge Notification**
   - The device sends a notification containing a 16-byte random challenge.

7. **Encrypt and Respond**
   - The app encrypts the challenge using the default key (AES/ECB/NoPadding).
   - It writes the response to the Auth characteristic:
     - `[0x83, 0x00, <16 bytes encrypted>]`

8. **Authentication Result**
   - The device sends a notification with the result.
   - If successful, the app proceeds to read battery info.

### Battery Information Fetch

- After authentication, the app reads the battery info characteristic:
  - **Service UUID:** `0000fee0-0000-1000-8000-00805f9b34fb`
  - **Characteristic UUID:** `00000006-0000-3512-2118-0009af100700`
- The value is parsed as:
  - `value[1]` = battery percentage (0-100)
  - Other bytes may contain additional status (e.g., charging)

## Usage

1. Launch the app.
2. Grant Bluetooth permissions when prompted.
3. Tap "Connect" to start scanning for GTR 2e devices.
4. The app will automatically:
   - Connect to the device
   - Perform authentication (challenge/response)
   - Read battery information
5. Battery status will be displayed on screen.

## Technical Details

### UUIDs Used
- **Auth Service:** `0000fee1-0000-1000-8000-00805f9b34fb`
- **Auth Characteristic:** `00000009-0000-3512-2118-0009af100700`
- **Battery Service:** `0000fee0-0000-1000-8000-00805f9b34fb`
- **Battery Characteristic:** `00000006-0000-3512-2118-0009af100700`

### Authentication Key
- The app uses a default authentication key (hardcoded in the code).
- In production, this key should be device-specific and securely stored.

### Dependencies
- Android Bluetooth LE API
- AES encryption for authentication
- Android permissions for Bluetooth and location

## Notes
- The app requires location permission for Bluetooth scanning.
- Authentication must complete before battery data can be read.
- The device must be in pairing mode for the initial connection.
- The app does not upload the key after initial pairing; it only performs challenge/response using the stored key.
