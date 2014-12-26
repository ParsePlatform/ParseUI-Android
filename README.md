# ParseUI
## Overview
This project contains the `ParseLoginUI` library for building login and signup flows with the Parse Android SDK.
You can easily configure the look and feel of the login screens by either specifying XML configurations or constructing an Intent in code.
To use this project with your app, you should import it as a library project in Android Studio.

![sample screens](http://parseui-android.parseapp.com/images/parse_login_sample_screens.png)

### Getting Started
We built several sample apps demonstrating how to use the `ParseLoginUI` library.  Before importing
this library into your app, we recommend that you run these sample apps to become familiar with its
functionality and customizations.  In this section, we describe Android Studio instructions for
running the sample apps (as a standalone project), and importing the `ParseLoginUI` library into
your own app.  These instructions were last tested on Android Studio 1.0.2.

#### Running Sample Projects
To run our sample apps, you need to import this repo as a standalone Gradle project:

1. Clone this repository onto your machine.
2. Import this repository's project with Android Studio (File > Import Project > `ParseUI-Android` folder). The project has Maven dependencies on the Facebook SDK and the Bolts framework.  Android Studio automatically resolves these via Gradle.
3. Specify the following in `res/values/strings.xml` of each sample project:
  * <code>parse_app_id</code> and <code>parse_client_key</code>
  * <code>facebook_app_id</code>
  * <code>twitter_consumer_key</code> and <code>twitter_consumer_secret<code>
4. Build (Tools > Android > Sync Project with Gradle Files) and run the sample apps using Android Studio.

#### Importing into Your App
1. Clone this repository onto your machine.
2. Import `ParseLoginUI` as a module into your app's Android Studio Project
  * File > Import Module in Android Studio
  * In the New Module pop-up, set the source directory to the `ParseUI-Android/ParseLoginUI` folder within the cloned repo.
3. Add the following to the `dependencies` section of app's build.gradle.  This adds a Maven dependency to the Facebook SDK and a module dependency to `ParseLoginUI`.

        compile 'com.facebook.android:facebook-android-sdk:3.21.0'
        compile project(':ParseLoginUI')

4. Add the following to your `AndroidManifest.xml` within the `<application></application>` section.  You can see a complete example in our [sample app](https://github.com/ParsePlatform/ParseUI-Android/blob/master/ParseLoginSampleBasic/AndroidManifest.xml).

        <activity
            android:name="com.parse.ui.ParseLoginActivity"
            android:label="@string/app_name"
            android:launchMode="singleTop">
            <!-- For more options, see https://www.parse.com/docs/android_guide#ui-login -->
            <meta-data
                android:name="com.parse.ui.ParseLoginActivity.PARSE_LOGIN_ENABLED"
                android:value="true"/>
            <meta-data
                android:name="com.parse.ui.ParseLoginActivity.PARSE_LOGIN_EMAIL_AS_USERNAME"
                android:value="true"/>
            <meta-data
                android:name="com.parse.ui.ParseLoginActivity.FACEBOOK_LOGIN_ENABLED"
                android:value="true"/>
            <meta-data
                android:name="com.parse.ui.ParseLoginActivity.TWITTER_LOGIN_ENABLED"
                android:value="true"/>
        </activity>
        <activity
            android:name="com.facebook.LoginActivity"
            android:label="@string/app_name"
            android:launchMode="singleTop"/>
        <meta-data
            android:name="com.facebook.sdk.ApplicationId"
            android:value="@string/facebook_app_id"/>

5. Specify the following in `res/values/strings.xml` of your app

        <string name="parse_app_id">YOUR_PARSE_APP_ID</string>
        <string name="parse_client_key">YOUR_PARSE_CLIENT_KEY</string>
        <string name="facebook_app_id">YOUR_FACEBOOK_APP_ID</string>
        <string name="twitter_consumer_key">YOUR_TWITTER_CONSUMER_KEY</string>
        <string name="twitter_consumer_secret">YOUR_TWITTER_CONSUMER_SECRET</string>
        <string name="app_name">YOUR_APP_NAME</string>

In the above instructions, you can skip sections related to Facebook and Twitter if you just want Parse Login.

## Documentation
For complete details about this library project, please see our [documentation](https://www.parse.com/docs/android_guide#ui-login) on the Parse website.
We'll discuss some highlights here.

To start the login flow from your own activity, you launch the `ParseLoginActivity` with two lines of code:

```java
ParseLoginBuilder builder = new ParseLoginBuilder(MyActivity.this);
startActivityForResult(builder.build(), 0);
```

`ParseLoginActivity` will guide the user through the login experience, where the user can also sign up or reset a forgotten password.
Each screen in the login experience is implemented by a fragment hosted within this activity.
When `ParseLoginActivity` finishes, you can check `ParseUser.getCurrentUser()` in your own activity to see whether the user actually logged in.

This library is ultra-customizable, allowing you to configure the login experience through either XML or code.
As shown in the Getting Started section, you can directly configure the login experience through the activity
meta-data in `AndroidManifest.xml`.

Please see the [Parse website](http://www.parse.com/docs/android_guide#ui-login) for additional documentation.

## Contributing
See the CONTRIBUTING file for how to help out.

## License
Copyright (c) 2014, Parse, LLC. All rights reserved.

You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
copy, modify, and distribute this software in source code or binary form for use
in connection with the web services and APIs provided by Parse.

As with any software that integrates with the Parse platform, your use of
this software is subject to the Parse Terms of Service
[https://www.parse.com/about/terms]. This copyright notice shall be
included in all copies or substantial portions of the software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.