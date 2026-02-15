
### About this fork:
#### Additional Features:
- aims working with android sdk19
- uses LogcatBinaryParser.kt from de.brudaswen.android.logcat.core.parser
- allows customized output format and global library settings.
  See example (1) below

#### Fixes (only dev facing):
- use flow and coroutins
- some typo fixes
- extract common code into LogcatReader

##### Settings example
Define a method to change the default config to your preference

```kotlin
fun setupCustomLogcatSettings() {
    val customLogfileFormat = object : LogFileFormat {
        override suspend fun writeLogs(
            logFileName: String,
            logs: Array<LogItem>,
            writer: BufferedWriter
        ) {
            if (logs.isNotEmpty()) {
                writer.write("Logcat: $logFileName\n")
                writer.write("------------------\n")
                for (log in logs) {
                    writer.write(log.origin + "\n")
                }
            }
        }
    }

    val customLogFilePrefix: LogFilePrefix = object : LogFilePrefix {
        override suspend fun getPrefix(): String {
            val dateFormat =
                SimpleDateFormat("'${BuildConfig.FLAVOR}_'yyyy-MM-dd_HH:mm:ss.SSS", Locale.ROOT)
            return dateFormat.format(Date())
        }
    }

    // overwrite parts of the LogFileShareDefault implementation
    val customLogFileShare = object : LogFileShareDefault() {
        override fun launchIntent(context: Context, shareIntent: Intent): Boolean {
            shareIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
            YourShareUtils.openIntent(context, shareIntent)
            return true // we want to handle error messaging ourselves in YourShareUtils.openIntent()
        }
    }

    Settings.update { current ->
        current.copy(
            logfileFormat = customLogfileFormat,
            logFilePrefix = customLogFilePrefix,
            logFileShare = customLogFileShare
        )
    }
}
```

#### Repository
Releases are distributed via jitpack. Make sure you include jitpack in
your `build.gradle` or `settings.gradle.kts`:

```gradle
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenCentral()
		maven { url = uri("https://jitpack.io") }
	}
}
```

```gradle
dependencies {
        implementation("com.github.evermind-zz:logcatviewer:Tag")
}
```

---
original README:
# LogcatViewer library
### Feature:

- Priority filter
- Clear logcat
- Export as file 
- Floating window

### Integrate guide

1. Clone this library as a project module, add module dependence.

2. Add launch code in your code:

    - Start logcat viewer
    ```kotlin
    LogcatActivity.start(getContext())
    ```
   
   - Start logcat viewer with log exclude rule
   ```kotlin
   val logcatExcludeRules = listOf(
       Pattern.compile(".*]: processMotionEvent MotionEvent \\{ action=ACTION_.*"),
       Pattern.compile(".*]: dispatchPointerEvent handled=true, event=MotionEvent \\{ action=ACTION_.*")
   )
   LogcatActivity.start(getContext(), logcatExcludeRules)
   ```

### Screenshot

<img src="https://raw.githubusercontent.com/kyze8439690/logcatviewer/master/screenshot/1.jpg" width="360">
<img src="https://raw.githubusercontent.com/kyze8439690/logcatviewer/master/screenshot/2.jpg" width="360">
<img src="https://raw.githubusercontent.com/kyze8439690/logcatviewer/master/screenshot/3.jpg" width="360">
<img src="https://raw.githubusercontent.com/kyze8439690/logcatviewer/master/screenshot/4.jpg" width="360">
