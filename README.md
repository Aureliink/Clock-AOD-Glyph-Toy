🕒 Clock AOD Glyph Toy
A minimalist and elegant Always-On Display (AOD) toy designed specifically for Nothing Phone devices. This application transforms the rear Glyph interface into a stylized digital clock wrapped in a dynamic circular battery indicator.

✨ Features
24h Digital Clock: Displays hours and minutes centered on the 25x25 Glyph matrix.

Circular Battery Indicator: A ring of LEDs surrounds the time. It depletes clockwise based on your phone's actual battery percentage.

Real-time System Status: The clock temporarily switches to dedicated icons for 3 seconds when changing ringer modes:

🔊 Normal Mode: Speaker icon.

📳 Vibrate Mode: Phone icon with vibration "sparks."

🔇 Silent Mode: A struck-through speaker icon.

Optimized Geometry: Uses precise mathematical scanning to ensure a perfect ring without "double pixels" in the corners, maintaining a clean pixel-art aesthetic.

📸 Visual Layout
The design is rendered on a 25x25 grid:

Top: Hours (custom retro-digital font).

Bottom: Minutes.

Border: Battery level (Circular pixel-art ring).

🛠️ Installation & Setup
Clone the repository:

Bash
git clone https://github.com/your-username/Clock-AOD-Glyph-Toy.git
Open the project in Android Studio.

Ensure you have the Nothing Glyph SDK configured.

Build and deploy to your Nothing Phone.

🚀 Technologies Used
Kotlin: Primary programming language.

Nothing Glyph SDK: For hardware LED control.

Canvas & Bitmaps: Dynamic generation of matrix frames.

📝 Development Notes
The app utilizes a BroadcastReceiver to listen for system audio changes (AudioManager.RINGER_MODE_CHANGED_ACTION), allowing the Glyph interface to react instantly to the phone's physical alert slider or volume settings.
