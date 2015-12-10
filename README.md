# Parse SDK for Android

This project contains two libraries, `ParseUI-Login` and `ParseUI-Widget`.

The `ParseUI-Login` library is used for building login and signup flows with the Parse Android SDK. You can easily configure the look and feel of the login screens by either specifying XML configurations or constructing an Intent in code.

The `ParseUI-Widget` library provides several useful UI widgets which integrate with [Parse SDK](https://github.com/ParsePlatform/Parse-SDK-Android)  seamlessly.


## Import to your project
1. Configure Parse SDK by following this [tutorial](https://www.parse.com/apps/quickstart#parse_data/mobile/android/native/existing).
2. Add the following to the `dependencies` section of your app's build.gradle.

    ```grovvy
    // Module dependency on ParseUI libraries sources
    compile 'com.parse:parseui-login-android:0.0.1'
    compile 'com.parse:parseui-widget-android:0.0.1'

    // Uncomment if using Facebook or Twitter Login (optional Maven dependency)
    // compile 'com.facebook.android:facebook-android-sdk:4.6.0'
    // compile 'com.parse:parsefacebookutils-v4-android:1.10.3@aar'
    // compile 'com.parse:parsetwitterutils-android:1.10.3'
    ```

## Usage
Please check the tutorial in our [wiki](https://github.com/ParsePlatform/ParseUI-Android/wiki);

## Documentation
For complete details about this library project, please see our [documentation](https://www.parse.com/docs/android/guide#user-interface-parseloginui) on the Parse website.

## Contributing
See the CONTRIBUTING file for how to help out.

## License
Copyright (c) 2014, Parse, LLC. All rights reserved.

You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
copy, modify, and distribute this software in source code or binary form for use
in connection with the web services and APIs provided by Parse.

As with any software that integrates with the Parse platform, your use of
this software is subject to the [Parse Terms of Service]
(https://www.parse.com/about/terms). This copyright notice shall be
included in all copies or substantial portions of the software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
