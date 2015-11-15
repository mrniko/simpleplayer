#!/bin/bash

# Fix the CircleCI path
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$PATH"

DEPS="$ANDROID_HOME/installed-dependencies"

if [ ! -e $DEPS ]; then
  echo y | android update sdk -u -a -t android-23 &&
  echo y | android update sdk -u -a -t platform-tools &&
  echo y | android update sdk -u -a -t extra &&
  echo y | android update sdk -u -a -t build-tools-23.0.1 &&
  echo y | android update sdk -u -a -t addon-google_apis-google-23 &&
  touch $DEPS
fi