# AFK Crab Helper

A RuneLite plugin that provides a distraction-reducing overlay when training on crabs, helping you focus on other tasks while AFK training.

## Features

- **Automatic Detection**: Detects when you're interacting with any crab type (Sand Crabs, Rock Crabs, Ammonite Crabs, Gemstone Crabs)
- **Distraction Overlay**: Covers the screen with a customizable overlay to reduce visual distractions
- **Training Timer**: Shows how long you've been training (optional)
- **Crab Name Display**: Shows which type of crab you're currently fighting (optional)
- **Customizable Settings**: Adjust overlay color, opacity, and timing delays

## Configuration Options

### Overlay Settings
- **Enable Overlay**: Toggle the main overlay functionality
- **Overlay Color**: Choose the color of the distraction overlay (default: black)
- **Overlay Opacity**: Adjust transparency (0-255, default: 180)

### Timing Settings
- **Activation Delay**: Seconds to wait before showing overlay (0-30, default: 3)
- **Hide Delay**: Seconds to wait before hiding overlay after combat stops (0-30, default: 5)

### Information Display
- **Show Timer**: Display training duration in top-left corner
- **Show Crab Name**: Display the name of the crab you're fighting

## Supported Crab Types

- Sand Crabs (Hosidius)
- Rock Crabs (Rellekka)
- Ammonite Crabs (Fossil Island)
- Gemstone Crabs (Gemstone Dragons area)

## How It Works

1. The plugin automatically detects when you start interacting with any crab
2. After the configured delay, a semi-transparent overlay covers your screen
3. Training information (timer, crab name) is displayed in the top-left corner if enabled
4. The overlay remains active while you're in combat with crabs
5. When combat ends, the overlay disappears after the configured hide delay

## Installation

This plugin is designed for submission to the RuneLite Plugin Hub. Once approved, it will be available through the standard plugin installation process.

## Usage Tips

- Set a longer activation delay to avoid flickering during brief combat pauses
- Adjust opacity to find the right balance between distraction reduction and visibility
- Use the timer feature to track your training sessions
- The overlay will automatically hide when you need to interact with the game

## Development

This plugin follows RuneLite's standard plugin structure and is ready for Plugin Hub submission.

### Building
```bash
./gradlew build
```

### Testing
Use the core integration approach documented in the RuneLite plugin development workflow for local testing.

## License

This plugin is licensed under the BSD 2-Clause License, same as RuneLite.