# 🕒 Clock AOD Glyph Toy

## 🚀 Overview

<br />
<p style="padding-left: 15px;">
  <b>UPDATE 2026/06/18 : THE APP KEEPS EVOLVING ! 🚀 
    <br />
    <ul>
      <li>
    🕛 Added a 12h format toggle
      </li>
      <li>
        🎶 Added a Music Visualizer with 2 different styles (still a work in progress regarding its reliability!)
      </li>
    </ul>
  </b>
</p>
<br />

---
<br />
A minimalist and elegant <b>Always-On Display (AOD)</b> toy designed specifically for the <b>Nothing Phone 3</b> device (for now). 
<br /><br />
This application transforms the rear Glyph interface into a stylized digital clock wrapped in either a dynamic circular battery indicator, or an horizontal one. The numbers will slowly slide down as the new one replacing it will come up from the top (like an old animated retro clock).
<br />
<b>You can also disable the battery indicator if you want a cleaner look with just the clock in a 2 lines AOD toy.</b>
<br /><br />

| "Ring" style | "Gauge" style  |
|:---:|:---:|
| <img width="450" src="https://github.com/user-attachments/assets/b1b70ba6-eb40-420e-8c6d-b979af934049" /> | <img width="450" src="https://github.com/user-attachments/assets/052fccbc-0b9d-4d38-8626-a1bc22db9cfd" /> |

<p align="center">
  <a href="https://github.com/Aureliink/Clock-AOD-Glyph-Toy/releases/latest">
    <img src="https://img.shields.io/badge/Download-APK-green?style=for-the-badge&logo=android" alt="Download APK">
  </a>

  <br />
  <br />
  <span>The app is straightforward : it will just add a toy that you can use as an AOD vertical clock, and it comes with an option to enable either a circular battery jauge, or an horizontal one. If you want a cleaner look, you can disable the battery level completely.</span>
  <br />
  <span>An icon will also be shown for 3s when you change your ringer mode too (vibrate, silent, ringer).</span>
  </p>

<br />

## ☕ Support the project
<p align="center">
  <span>If you ever wanna offer me a croissant 🥐 : </span>
  <br />
  <br />
  <a href="https://www.paypal.me/Mxiden">
    <img src="https://img.shields.io/badge/Donate-PayPal-blue.svg?style=for-the-badge&logo=paypal" alt="Donate with PayPal">
  </a>
</p>

---

## ✨ Key Features

### 🕒 Smart Digital Clock
*   **12 or 24h Format**: Clear display of hours and minutes centered on the 25x25 Glyph matrix.
*   **Pixel-Art Font**: Custom-designed digits for maximum legibility on LED hardware.

### 🎶 Music Visualizer
*   🔊 **Waves**: Moving and pulsating waves that mimicks Nothing's style.
*   💿 **Spinning vinyl**: A spinning vinyl that takes full advantage of the whole size of the Glyph Matrix.

### 🔋 Dynamic (optional) Battery Level
*   **Ring style**: A perfect LED ring surrounds the time, empties in a natural **clockwise** motion as your battery drains.
*   **Gauge style**: An horizontal bar depleting from right to left, located between the hours and the minutes, right in the middle of the Matrix
*   **Math-Optimized**: Precise rendering logic ensures a smooth circle without "double pixels" or gaps.

### 📞 Phone call status
Enable an animated phone icon during your phone calls

### 🔔 System Status Icons
The clock automatically reacts to your ringer mode changes for 3 seconds before returning to the time:
*   🔊 **Normal Mode**: Dynamic speaker icon.
*   📳 **Vibrate Mode**: Phone icon with vibration "sparks."
*   🔇 **Silent Mode**: Struck-through speaker icon.

---

## 📸 Visual Layout
The design is rendered on a 25x25 grid:

* Top: Hours (custom retro-digital font).
* Bottom: Minutes.
* Border: Battery level (Circular pixel-art ring) or Horizontal (between hours and minutes)

---

## 🛠️ Installation & Setup
Clone the repository:

Bash
git clone https://github.com/your-username/Clock-AOD-Glyph-Toy.git
* Open the project in Android Studio.
* Ensure you have the Nothing Glyph SDK configured.
* Build and deploy to your Nothing Phone.

---

## 🚀 Technologies Used
* Kotlin: Primary programming language.
* Nothing Glyph SDK: For hardware LED control.
* Canvas & Bitmaps: Dynamic generation of matrix frames.

---

## 📝 Development Notes
The app utilizes a BroadcastReceiver to listen for system audio changes (AudioManager.RINGER_MODE_CHANGED_ACTION), allowing the Glyph interface to react instantly to the phone's physical alert slider or volume settings.

---

### Disclaimer : this README was Generated with Gemini cause I'm too lazy and have too little knowledge about the Github world and what I should include here 😂
