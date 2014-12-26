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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;

/**
 * Fragment for the login help screen for resetting the user's password.
 */
public class ParseLoginHelpFragment extends ParseLoginFragmentBase implements OnClickListener {

  public interface ParseOnLoginHelpSuccessListener {
    public void onLoginHelpSuccess();
  }

  private TextView instructionsTextView;
  private EditText emailField;
  private Button submitButton;
  private boolean emailSent = false;
  private ParseOnLoginHelpSuccessListener onLoginHelpSuccessListener;

  private ParseLoginConfig config;

  private static final String LOG_TAG = "ParseLoginHelpFragment";

  public static ParseLoginHelpFragment newInstance(Bundle configOptions) {
    ParseLoginHelpFragment loginHelpFragment = new ParseLoginHelpFragment();
    loginHelpFragment.setArguments(configOptions);
    return loginHelpFragment;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                           Bundle savedInstanceState) {
    config = ParseLoginConfig.fromBundle(getArguments(), getActivity());

    View v = inflater.inflate(R.layout.com_parse_ui_parse_login_help_fragment,
        parent, false);
    ImageView appLogo = (ImageView) v.findViewById(R.id.app_logo);
    instructionsTextView = (TextView) v
        .findViewById(R.id.login_help_instructions);
    emailField = (EditText) v.findViewById(R.id.login_help_email_input);
    submitButton = (Button) v.findViewById(R.id.login_help_submit);

    if (appLogo != null && config.getAppLogo() != null) {
      appLogo.setImageResource(config.getAppLogo());
    }

    submitButton.setOnClickListener(this);
    return v;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    if (activity instanceof ParseOnLoadingListener) {
      onLoadingListener = (ParseOnLoadingListener) activity;
    } else {
      throw new IllegalArgumentException(
          "Activity must implemement ParseOnLoadingListener");
    }

    if (activity instanceof ParseOnLoginHelpSuccessListener) {
      onLoginHelpSuccessListener = (ParseOnLoginHelpSuccessListener) activity;
    } else {
      throw new IllegalArgumentException(
          "Activity must implemement ParseOnLoginHelpSuccessListener");
    }
  }

  @Override
  public void onClick(View v) {
    if (!emailSent) {
      String email = emailField.getText().toString();
      if (email.length() == 0) {
        showToast(R.string.com_parse_ui_no_email_toast);
      } else {
        loadingStart();
        ParseUser.requestPasswordResetInBackground(email,
            new RequestPasswordResetCallback() {
              @Override
              public void done(ParseException e) {
                if (isActivityDestroyed()) {
                  return;
                }

                loadingFinish();
                if (e == null) {
                  instructionsTextView
                      .setText(R.string.com_parse_ui_login_help_email_sent);
                  emailField.setVisibility(View.INVISIBLE);
                  submitButton
                      .setText(R.string.com_parse_ui_login_help_login_again_button_label);
                  emailSent = true;
                } else {
                  debugLog(getString(R.string.com_parse_ui_login_warning_password_reset_failed) +
                      e.toString());
                  if (e.getCode() == ParseException.INVALID_EMAIL_ADDRESS ||
                      e.getCode() == ParseException.EMAIL_NOT_FOUND) {
                    showToast(R.string.com_parse_ui_invalid_email_toast);
                  } else {
                    showToast(R.string.com_parse_ui_login_help_submit_failed_unknown);
                  }
                }
              }
            });
      }
    } else {
      onLoginHelpSuccessListener.onLoginHelpSuccess();
    }
  }

  @Override
  protected String getLogTag() {
    return LOG_TAG;
  }
}
