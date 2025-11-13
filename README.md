# ShredForge

A music practice and training application built with JavaFX, designed to help musicians improve their skills through real-time pitch detection and interactive feedback.

## Overview

ShredForge is a desktop application that provides musicians with tools to practice and improve their playing. The application uses TarsosDSP for real-time pitch detection and offers an intuitive JavaFX-based user interface.

## Features

- Modern JavaFX-based user interface
- Cross-platform support (Windows, macOS, Linux)
- Real-time audio processing capabilities (TarsosDSP integration)
- Modular architecture for easy extension

## Requirements

- Java 17 or higher
- Maven 3.6 or higher
- Internet connection (for initial dependency download)

## Building the Application

### Quick Start

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd ShredForge
   ```

2. Navigate to the project directory:
   ```bash
   cd shredforge
   ```

3. Build with Maven:
   ```bash
   mvn clean install
   ```

### Running the Application

To run the application directly with Maven:

```bash
cd shredforge
mvn javafx:run
```

### Creating a Native Package

#### macOS

```bash
cd shredforge
mvn clean package
```

The application bundle will be created in `target/jpackage/ShredForge.app`

#### Windows

```bash
cd shredforge
mvn clean package
```

The application will be created in `target/jpackage/ShredForge/`

#### Linux

```bash
cd shredforge
mvn clean package
```

The application will be created in `target/jpackage/ShredForge/`

## Project Structure

```
ShredForge/
├── shredforge/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/shredforge/
│   │       │       ├── App.java              # Main application class
│   │       │       ├── PrimaryController.java # Primary view controller
│   │       │       └── SecondaryController.java # Secondary view controller
│   │       └── resources/
│   │           └── com/shredforge/
│   │               ├── primary.fxml          # Primary view layout
│   │               └── secondary.fxml        # Secondary view layout
│   └── pom.xml                               # Maven configuration
├── build.md                                  # Detailed build instructions
└── README.md                                 # This file
```

## Technology Stack

- **Java 17**: Core programming language
- **JavaFX 17**: User interface framework
- **TarsosDSP 2.5**: Real-time audio processing and pitch detection
- **Jackson 2.17**: JSON serialization/deserialization
- **Maven**: Build and dependency management

## Platform Support

The application is configured to build native packages for:

- **macOS**: Apple Silicon (ARM64) and Intel (x64)
- **Windows**: x64
- **Linux**: x64 and ARM64

The build system automatically detects your platform and uses the appropriate JavaFX dependencies.

## Development

### Module System

ShredForge uses the Java Platform Module System (JPMS). The module descriptor is located at:
```
src/main/java/module-info.java
```

Required modules:
- `javafx.controls`
- `javafx.fxml`
- `javafx.web`
- `javafx.graphics`
- `com.fasterxml.jackson.databind`

### Logging

The application uses Java's built-in logging framework (`java.util.logging`). Logs are written to:
- Console output (stdout/stderr)
- Application-specific log files (configurable)

## Troubleshooting

### Build Issues

**Problem**: Maven cannot download dependencies

**Solution**: Ensure you have an active internet connection and Maven can access Maven Central repository.

**Problem**: JavaFX platform not detected correctly

**Solution**: The build system should auto-detect your platform. If it doesn't, you can manually specify it:
```bash
mvn clean install -Djavafx.platform=<platform>
```
Where `<platform>` is one of: `win`, `mac-aarch64`, `mac-x64`, `linux`, or `linux-aarch64`

### Runtime Issues

**Problem**: Application won't start

**Solution**: Ensure Java 17 or higher is installed:
```bash
java -version
```

**Problem**: FXML loading errors

**Solution**: Check that all FXML files are present in `src/main/resources/com/shredforge/`

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

[License information to be added]

## Roadmap

### Planned Features

- [ ] Real-time pitch detection visualization
- [ ] Musical scale trainer
- [ ] Chord recognition
- [ ] Metronome with customizable patterns
- [ ] Recording and playback functionality
- [ ] Practice session tracking and analytics
- [ ] Tablature display
- [ ] Custom tuning support

## Contact

[Contact information to be added]

## Acknowledgments

- [TarsosDSP](https://github.com/JorenSix/TarsosDSP) for audio processing capabilities
- JavaFX community for excellent UI framework
