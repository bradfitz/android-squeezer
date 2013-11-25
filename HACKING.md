HACKING
=======

This guide assumes you have already downloaded and installed the Android
SDK, in to a directory referred to as $SDK.

Fetch the code
--------------


Android Studio
--------------

*   Install the Android Build Tools revision 17.0.0 if not already installed.

    Run $SDK/tools/android and select "Android SDK Build-tools",

*   Run Android Studio

*   If you have no projects open then choose "Import project" from the dialog
    that appears.

    If you already have a project open then choose File > Import Project...

*   In the "Select File or Directory to Import" dialog that appears, navigate
    to the directory that you fetched the Squeezer source code in to and
    select the build.gradle file that ships with Squeezer.

*   In the "Import Project from Gradle" dialog tick "Use auto-import" and
    make sure that "Use gradle wrapper (recommended)" is selected.

*   Copy ide/intellij/codestyles/AndroidStyle.xml to Android Studio's config
    directory.

    Linux: ~/.AndroidStudioPreview/config/codestyles
    OS X: ~/Library/Preferences/AndroidStudioPreview/codestyles
    Windows: TBD

*   Go to Settings (or Preferences in Mac OS X) > Code Style > Java, select
    "AndroidStyle", as well as Code Style > XML and select "AndroidStyle".
