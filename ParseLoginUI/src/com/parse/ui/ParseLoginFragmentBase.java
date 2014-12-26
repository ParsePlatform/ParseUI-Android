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

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

import com.parse.Parse;

/**
 * Base class with helper methods for fragments in ParseLoginUI.
 */
public class ParseLoginFragmentBase extends Fragment {
  protected ParseOnLoadingListener onLoadingListener;

  protected String getLogTag() {
    return null;
  }

  protected void showToast(int id) {
    showToast(getString(id));
  }

  protected void showToast(CharSequence text) {
    Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
  }

  protected void loadingStart() {
    loadingStart(true);
  }

  protected void loadingStart(boolean showSpinner) {
    if (onLoadingListener != null) {
      onLoadingListener.onLoadingStart(showSpinner);
    }
  }

  protected void loadingFinish() {
    if (onLoadingListener != null) {
      onLoadingListener.onLoadingFinish();
    }
  }

  protected void debugLog(int id) {
    debugLog(getString(id));
  }

  protected void debugLog(String text) {
    if (Parse.getLogLevel() <= Parse.LOG_LEVEL_DEBUG &&
        Log.isLoggable(getLogTag(), Log.WARN)) {
      Log.w(getLogTag(), text);
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  protected boolean isActivityDestroyed() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      return getActivity().isDestroyed();
    } else {
      return ((ParseLoginActivity) getActivity()).isDestroyed();
    }
  }
}
