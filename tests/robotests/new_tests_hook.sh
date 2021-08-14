#!/bin/bash

# This script detects the presence of new robolectric java tests within
# commits to be uploaded. If a new file is detected the script will print an
# error message and return an error code. Intended to be used as a repo hook.

new_robolectric_tests=$(
    git diff --name-status $REPO_LREV | grep "^A.*tests/robotests.*\.java")
if [ $new_robolectric_tests != "" ]
then
    echo "New Robolectric unit tests detected. Please submit junit tests" \
    "instead, in the tests/junit directory." \
    "See go/android-platform-robolectric-cleanup."
    echo $new_robolectric_tests
    exit 1
fi
