# Running Settings Robolectric tests


## The full suite
```
$ croot
$ make RunSettingsRoboTests
```

## Running a single test class

```
$ croot
$ make RunSettingsRoboTests ROBOTEST_FILTER=<ClassName>
```

For example:

```
make RunSettingsRoboTests ROBOTEST_FILTER=CodeInspectionTest
```

You can also use partial class name in ROBOTEST_FILTER. If the partial class name matches
multiple file names, all of them will be executed.
