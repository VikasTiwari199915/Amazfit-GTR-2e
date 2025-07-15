# GTR 2e Battery App

A simple Android app to connect to Amazfit GTR 2e smartwatch and fetch battery status.

## Authentication System

The Amazfit GTR 2e uses a 3-step authentication process:

### Step 1: Send Secret Key
- Send a 16-byte secret key to the device
- Command: `[0x01, 0x08, <16-byte-key>]`
- The device responds with success/failure

### Step 2: Request Random Number
- Request a random 16-byte number from the device
- Command: `[0x02, 0x08]`
- Device responds with the random number

### Step 3: Send Encrypted Response
- Encrypt the random number with the secret key using AES-ECB
- Send the encrypted data back to the device
- Command: `[0x03, 0x08, <16-byte-encrypted-data>]`
- Device responds with authentication success/failure

## Battery Information

After successful authentication, the app can request battery information:

- **Request Command**: `[0x10, 0x01, 0x01]`
- **Response Format**: `[0x10, <level>, <state>, ...]`
  - `level`: Battery percentage (0-100)
  - `state`: 0 = normal, 1 = charging

## Key Features

- Automatic device discovery and connection
- 3-step authentication process
- Battery level and charging status
- Real-time status updates
- Error handling and logging

## Usage

1. Launch the app
2. Grant Bluetooth permissions when prompted
3. Tap "Connect" to start scanning for GTR 2e devices
4. The app will automatically:
   - Connect to the device
   - Perform authentication
   - Request battery information
5. Battery status will be displayed on screen

## Technical Details

### UUIDs Used
- **Service UUID**: `0000fee0-0000-1000-8000-00805f9b34fb`
- **Auth Characteristic**: `00000009-0000-3512-2118-0009af100700`
- **Write Characteristic**: `00000001-0000-3512-2118-0009af100700`
- **Battery Characteristic**: `00000006-0000-3512-2118-0009af100700`

### Authentication Key
The app uses a default authentication key. In a production app, this should be device-specific and securely stored.

## Dependencies

- Android Bluetooth LE API
- AES encryption for authentication
- Android permissions for Bluetooth and location

## Notes

- The app requires location permission for Bluetooth scanning
- Authentication must complete before battery data can be requested
- The device must be in pairing mode for initial connection
