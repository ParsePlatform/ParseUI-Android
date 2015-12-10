/*
 *  Copyright (c) 2014, Parse, LLC. All rights reserved.
 *
 *  You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 *  copy, modify, and distribute this software in source code or binary form for use
 *  in connection with the web services and APIs provided by Parse.
 *
 *  As with any software that integrates with the Parse platform, your use of
 *  this software is subject to the Parse Terms of Service
 *  [https://www.parse.com/about/terms]. This copyright notice shall be
 *  included in all copies or substantial portions of the software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.parse.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseUser;

/**
 * Activity that starts ParseLoginActivity if the user is not logged in.
 * Otherwise, it starts the subclass-defined target activity.
 * 
 * To use this, you should subclass this activity and implement
 * {@link ParseLoginDispatchActivity#getTargetClass} to return the class of the
 * target activity that should be launched after login succeeds. If the user
 * cancels the login, your app will go back to whatever activity it was on before
 * your subclass dispatch activity was launched, or exit the app if your subclass
 * is the first activity in your app's backstack.
 * 
 * You can think of your subclass as a gate keeper for any activities that
 * require a logged-in user to function. You should have one gate keeper per
 * entry path into your app (e.g. launching the app, or entering through push
 * notifications). When your app launches or receives a push notification, you
 * should specify that your gate keeper activity be launched (and the gate
 * keeper will redirect to your target activity upon successful login).
 */
public abstract class ParseLoginDispatchActivity extends Activity {

  protected abstract Class<?> getTargetClass();

  private static final int LOGIN_REQUEST = 0;
  private static final int TARGET_REQUEST = 1;

  private static final String LOG_TAG = "ParseLoginDispatch";

  @Override
  final protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    runDispatch();
  }

  @Override
  final protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    setResult(resultCode);
    if (requestCode == LOGIN_REQUEST && resultCode == RESULT_OK) {
      runDispatch();
    } else {
      finish();
    }
  }

  /**
   * Override this to generate a customized intent for starting ParseLoginActivity.
   * However, the preferred method for configuring Parse Login UI components is by
   * specifying activity options in AndroidManifest.xml, not by overriding this.
   *
   * @return Intent that can be used to start ParseLoginActivity
   */
  protected Intent getParseLoginIntent() {
    ParseLoginBuilder builder = new ParseLoginBuilder(this);
    return builder.build();
  }

  private void runDispatch() {
    if (ParseUser.getCurrentUser() != null) {
      debugLog(getString(R.string.com_parse_ui_login_dispatch_user_logged_in) + getTargetClass());
      Intent targetIntent = new Intent(this, getTargetClass());
      Bundle extras = getIntent().getExtras();
      if (extras != null) {
        targetIntent.putExtras(extras);
      }
      startActivityForResult(targetIntent, TARGET_REQUEST);
    } else {
      debugLog(getString(R.string.com_parse_ui_login_dispatch_user_not_logged_in));
      startActivityForResult(getParseLoginIntent(), LOGIN_REQUEST);
    }
  }

  private void debugLog(String message) {
    if (Parse.getLogLevel() <= Parse.LOG_LEVEL_DEBUG &&
        Log.isLoggable(LOG_TAG, Log.DEBUG)) {
      Log.d(LOG_TAG, message);
    }
  }
}
