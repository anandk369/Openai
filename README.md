
# MCQ Automation Android App

An Android app built in Kotlin for automating Multiple Choice Questions (MCQs) by capturing screen content, extracting text via OCR, fetching answers from Gemini API, and auto-tapping the correct option.

## Features

- **Screen Capture**: Capture user-defined screen regions at selectable resolutions (480p, 720p, 1080p)
- **OCR**: Google ML Kit Cloud Text Recognition for fast text extraction
- **AI Integration**: Gemini 2.0 Flash API for intelligent answer selection
- **Auto-Tap**: Accessibility Service and coordinate-based tapping
- **Floating UI**: Draggable floating buttons for easy access
- **Settings Panel**: Configurable screen region, resolution, and option positioning
- **Answer Caching**: Room database for caching answers to improve performance
- **Fixed Position Options**: Optional A, B, C, D buttons for faster tapping

## Requirements

- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Permissions**: Internet, Foreground Service, System Alert Window, Accessibility Service
- **Dependencies**: ML Kit, OkHttp, Room, Coroutines

## Setup Instructions

### 1. API Key Configuration

1. Create a `local.properties` file in the root directory
2. Add your Gemini API key:
   ```
   GEMINI_API_KEY=your_actual_api_key_here
   ```
3. Update `GeminiAPI.kt` line 23 to use the API key from properties:
   ```kotlin
   private const val API_KEY = BuildConfig.GEMINI_API_KEY
   ```

### 2. Build Configuration

1. Add to `app/build.gradle` in the `android` block:
   ```gradle
   buildConfigField "String", "GEMINI_API_KEY", "\"${project.findProperty('GEMINI_API_KEY') ?: ''}\""
   ```

### 3. Permissions Setup

The app requires the following permissions:

1. **Overlay Permission**: 
   - Go to Settings > Apps > MCQ Automation > Display over other apps
   - Enable the permission

2. **Accessibility Service**:
   - Go to Settings > Accessibility > MCQ Automation
   - Enable the service

### 4. First Run

1. Launch the app
2. Tap "Enable Floating Button" to start the service
3. Tap "Enable Accessibility Service" to open accessibility settings
4. Grant the required permissions

## Usage Instructions

### Basic Usage

1. **Enable Services**: Ensure both floating button and accessibility services are enabled
2. **Open MCQ App**: Navigate to your MCQ application (Quizlet, Kahoot, etc.)
3. **Start Automation**: Tap the floating Play button (▶) to begin
4. **View Results**: The button will show processing (spinner), success (✓), or error (✗)

### Settings Configuration

1. **Tap Settings Button**: Use the gear icon below the Play button
2. **Select Screen Region**: Choose the area containing MCQ questions
3. **Set Resolution**: Select capture quality (720p recommended)
4. **Enable Fixed Buttons**: Toggle A, B, C, D buttons for faster response
5. **Position Options**: If using fixed buttons, position them over answer choices

### Performance Optimization

- **Use 720p resolution** for optimal balance of speed and accuracy
- **Enable fixed position buttons** when MCQ layout is consistent
- **Answers are cached** automatically to skip repeated API calls
- **Expected processing time**: 0.6-1.3s (cached: ~0.5s)

## Architecture

### Core Components

- **MainActivity.kt**: Main UI and permission management
- **FloatingButtonService.kt**: Overlay service with floating controls
- **OCRHelper.kt**: Screen capture and ML Kit text recognition
- **GeminiAPI.kt**: API communication and answer caching
- **AutoClickService.kt**: Accessibility service for automated tapping
- **AnswerCache.kt**: Room database for answer persistence

### Data Flow

1. **Screen Capture** → Extract defined region at selected resolution
2. **OCR Processing** → ML Kit extracts and processes text
3. **MCQ Parsing** → Regex identifies question and options
4. **Answer Retrieval** → Check cache or call Gemini API
5. **Auto-Tap** → Accessibility service or coordinate-based tap

## Performance Metrics

The app logs detailed performance metrics:

```
Screen capture: ~150-300ms
OCR processing: ~200-500ms
API call: ~300-800ms (cached: ~50ms)
Total time: ~650-1300ms
```

## Troubleshooting

### Common Issues

1. **App crashes on launch**:
   - Ensure all permissions are granted
   - Check API key configuration

2. **OCR not working**:
   - Verify screen region selection
   - Try different resolution settings
   - Ensure good contrast in MCQ display

3. **Auto-tap not working**:
   - Enable accessibility service
   - For fixed buttons, ensure proper positioning
   - Check app-specific accessibility permissions

4. **API errors**:
   - Verify Gemini API key is valid
   - Check internet connection
   - Monitor API usage limits

### Debug Logs

Enable debug logging to monitor performance:

```bash
adb logcat -s FloatingButtonService OCRHelper GeminiAPI AutoClickService
```

## Security Considerations

- API key stored in local.properties (not in version control)
- Network requests use HTTPS
- Local caching reduces API calls
- No sensitive data stored permanently

## Testing

Tested with:
- Quizlet mobile app
- Kahoot mobile app
- Custom MCQ applications

## Limitations

- Requires Android 8.0+ (API 26)
- No root access required
- Single question per screen
- Requires stable internet connection
- Performance depends on device capabilities

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is for educational purposes only. Use responsibly and in accordance with the terms of service of the applications you're automating.
