# GTR 2e Companion

A privacy-focused, open-source Android companion app for the Amazfit GTR 2e smartwatch, enabling seamless data synchronization and advanced device management.

## Overview

GTR 2e Companion provides a robust alternative to official applications for managing the Amazfit GTR 2e. By implementing the Huami Bluetooth Low Energy (BLE) protocol, the app offers core synchronization and control features with a commitment to simplicity, performance, and user privacy.

## Key Features

- **Automated Authentication**: Integrated Zepp cloud login to retrieve your device's unique `authKey` automatically, making the initial setup seamless.
- **Real-time Health Monitoring**: Synchronize and display live data including:
    - Step count and progress tracking.
    - Continuous or manual Heart Rate monitoring.
    - Battery status and charging indicators.
- **Advanced Device Control**:
    - Toggle "Lift Wrist to Wake" and customize sensitivity.
    - Remote "Find Watch" functionality.
    - Manage "Do Not Disturb" (DND) modes.
    - Time and Date synchronization.
- **Media & Notification Integration**:
    - **Music Control**: Full synchronization of media metadata (Artist, Album, Track) and playback state from your phone to the watch.
    - **Call Management**: Receive incoming call notifications on your wrist with the ability to reject or mute calls directly.
- **Reliable Background Sync**: A dedicated foreground service ensures a persistent connection for notifications and data updates without being killed by the OS.
- **In-App Updates**: Built-in update mechanism to ensure you are always running the latest version with improved stability and features.

> **Note**: This is an active project. **More features will be added one by one with each update**, expanding the capabilities of the app to provide a full-featured companion experience.

## Technical Details

The application is built using modern Android development practices:
- **BLE Stack**: A robust queue-based BLE operation manager to handle reliable communication with the watch.
- **Foreground Service**: Ensures long-running operations and connectivity are maintained.
- **Huami Protocol**: Full implementation of the 2021 variant of the Huami chunked transfer protocol for complex data types like music metadata.
- **Security**: Secure handling of authentication challenges and AES-based encryption for the pairing process.

## Acknowledgements & Credits

This project stands on the shoulders of giants in the open-source community. Special thanks to:

- **[Gadgetbridge](https://gadgetbridge.org/)**: For their invaluable work in reverse-engineering the Huami protocol. Significant portions of the BLE handling logic and protocol definitions are inspired by or adapted from the Gadgetbridge codebase and its contributors.
- **[HuaToken](https://codeberg.org/argrento/huami-token/)**: For providing the methodology and inspiration for the Zepp cloud authentication and key retrieval logic. Check out the project for more technical details.

## License

This project incorporates code from Gadgetbridge, which is licensed under the [GNU Affero General Public License v3.0](https://www.gnu.org/licenses/agpl-3.0.en.html). Accordingly, this project is also subject to the terms of the AGPL-3.0.

---
*Disclaimer: This project is not affiliated with, authorized, maintained, sponsored, or endorsed by Zepp Health or Amazfit.*
