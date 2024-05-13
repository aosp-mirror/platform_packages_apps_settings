# Running Settings Robolectric tests


## The full suite
```
$ croot
$ atest SettingsRoboTests
```

## Running a single test class

With a filter

```
$ croot
$ atest SettingsRoboTests:com.android.settings.display.AdaptiveSleepPreferenceControllerTest
```

You can also run any single test class with atest (it will try to find the correct path)

```
$ atest AdaptiveSleepPreferenceControllerTest
```
