package com.featurea.dropboxHelper;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

public class Dropbox {

  private static final String TAG = "Dropbox";
  private static final String ID = "com.dropbox.android";
  private static final String APP_KEY = "p2e2ilowzjsm009";
  private static final String APP_SECRET = "mg0zkm6mxzd428o";
  private static final String ACCOUNT_PREFS_NAME = "prefs";
  private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
  private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";
  private Activity myActivity;
  private DropboxAPI<AndroidAuthSession> mApi;
  private Button loginButton;
  private TextView accountTextView;

  public Dropbox(Activity myActivity) {
    this.myActivity = myActivity;
    AndroidAuthSession session = buildSession();
    mApi = new DropboxAPI<AndroidAuthSession>(session);
    loginButton = (Button) myActivity.findViewById(R.id.login);
    accountTextView = (TextView) myActivity.findViewById(R.id.accout);
    loginButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (isLogin()) {
          logout();
        } else {
          login();
        }
      }
    });
    updateUI();
  }

  public void resume() {
    AndroidAuthSession session = mApi.getSession();


    // The next part must be inserted in the onResume() method of the
    // activity from which session.startAuthentication() was called, so
    // that Dropbox authentication completes properly.
    if (session.authenticationSuccessful()) {
      try {
        // Mandatory call to complete the auth
        session.finishAuthentication();

        // Store it locally in our app for later use
        storeAuth(session);
      } catch (IllegalStateException e) {
        showToast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
        Log.i(TAG, "Error authenticating", e);
      }
    }

    updateUI();
  }

  public void install() {
    myActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + ID)));
  }

  /*private API*/

  private void updateUI() {
    boolean isInstalled = isInstalled();
    myActivity.findViewById(R.id.install).setVisibility(isInstalled ? View.GONE : View.VISIBLE);
    myActivity.findViewById(R.id.workspace).setVisibility(isInstalled ? View.VISIBLE : View.GONE);
    loginButton.setText(isLogin() ? "Detach" : "Attach");
    myActivity.findViewById(R.id.dropbox).setVisibility(isLogin() ? View.VISIBLE : View.GONE);
    accountTextView.setVisibility(isLogin() ? View.VISIBLE : View.GONE);
    try {
      if (isLogin()) {
        String email = mApi.accountInfo().email;
        accountTextView.setText(email);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void showToast(String msg) {
    Toast error = Toast.makeText(myActivity, msg, Toast.LENGTH_LONG);
    error.show();
  }

  /**
   * Shows keeping the access keys returned from Trusted Authenticator in a local
   * store, rather than storing user name & password, and re-authenticating each
   * time (which is not to be done, ever).
   */
  private void loadAuth(AndroidAuthSession session) {
    SharedPreferences prefs = myActivity.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
    String key = prefs.getString(ACCESS_KEY_NAME, null);
    String secret = prefs.getString(ACCESS_SECRET_NAME, null);
    if (key == null || secret == null || key.length() == 0 || secret.length() == 0) return;

    if (key.equals("oauth2:")) {
      // If the key is set to "oauth2:", then we can assume the token is for OAuth 2.
      session.setOAuth2AccessToken(secret);
    } else {
      // Still support using old OAuth 1 tokens.
      session.setAccessTokenPair(new AccessTokenPair(key, secret));
    }
  }

  /**
   * Shows keeping the access keys returned from Trusted Authenticator in a local
   * store, rather than storing user name & password, and re-authenticating each
   * time (which is not to be done, ever).
   */
  private void storeAuth(AndroidAuthSession session) {
    // Store the OAuth 2 access token, if there is one.
    String oauth2AccessToken = session.getOAuth2AccessToken();
    if (oauth2AccessToken != null) {
      SharedPreferences prefs = myActivity.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
      SharedPreferences.Editor edit = prefs.edit();
      edit.putString(ACCESS_KEY_NAME, "oauth2:");
      edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
      edit.commit();
      return;
    }
    // Store the OAuth 1 access token, if there is one.  This is only necessary if
    // you're still using OAuth 1.
    AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
    if (oauth1AccessToken != null) {
      SharedPreferences prefs = myActivity.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
      SharedPreferences.Editor edit = prefs.edit();
      edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
      edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
      edit.commit();
      return;
    }
  }

  private void clearKeys() {
    SharedPreferences prefs = myActivity.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
    SharedPreferences.Editor edit = prefs.edit();
    edit.clear();
    edit.commit();
  }

  private AndroidAuthSession buildSession() {
    AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
    AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
    loadAuth(session);
    return session;
  }

  private boolean isLogin() {
    return mApi.getSession().isLinked();
  }

  private boolean isInstalled() {
    try {
      myActivity.getPackageManager().getPackageInfo(ID, PackageManager.GET_ACTIVITIES);
      return true;
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
  }

  private void logout() {
    mApi.getSession().unlink();
    clearKeys();
    updateUI();
  }

  private void login() {
    mApi.getSession().startOAuth2Authentication(myActivity);
  }

}
