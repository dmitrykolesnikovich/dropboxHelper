package com.featurea.dropboxHelper;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.RESTUtility;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;

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
  private DropboxAnimation dropboxAnimation;


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
    dropboxAnimation = new DropboxAnimation(myActivity);
    updateUI();
  }

  public DropboxAPI.Entry metadataWithDeleted(String path, int fileLimit, String hash, boolean list, String rev) throws DropboxException {
    if (isLogin()) {
      Session session = mApi.getSession();
      if (fileLimit <= 0) {
        fileLimit = 25000;
      }
      String[] params = new String[]{
          "file_limit", String.valueOf(fileLimit),
          "include_deleted", String.valueOf(true),
          "hash", hash,
          "list", String.valueOf(list),
          "rev", rev,
          "locale", session.getLocale().toString()

      };
      String url_path = "/metadata/" + session.getAccessType() + path;
      Map dirinfo = (Map) RESTUtility.request(RESTUtility.RequestMethod.GET, session.getAPIServer(), url_path, 1, params, session);
      return new DropboxAPI.Entry(dirinfo);
    } else {
      return null;
    }
  }

  public void resume() {
    AndroidAuthSession session = mApi.getSession();
    if (session.authenticationSuccessful()) {
      try {
        session.finishAuthentication();
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

  private int counter;

  public void update() {
    if (isLogin()) {
      List<DropboxAPI.Entry> files = getFiles();
      System.out.println(TAG + " size: " + files.size());
      for (DropboxAPI.Entry entry : files) {
        System.out.println(entry.fileName());
        download(entry);
      }
    }
  }

  private void download(final DropboxAPI.Entry entry) {
    new AsyncTask() {

      @Override
      protected void onPreExecute() {
        super.onPreExecute();
        counter++;
      }

      @Override
      protected Object doInBackground(Object[] params) {
        if (entry.isDeleted) {
          deleteFile(entry);
        } else {
          updateFile(entry);
        }

        return null;
      }

      @Override
      protected void onProgressUpdate(Object[] values) {
        super.onProgressUpdate(values);
        updateUI();
      }

      @Override
      protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        counter--;
        updateUI();
      }
    }.execute();
  }

  private void deleteFile(DropboxAPI.Entry entry) {
    File file = new File(getRoot() + "/" + entry.path);
    file.delete();
    System.out.println(TAG + " updated DELETE: " + file.getAbsolutePath() + ", revision: " + entry.rev);
  }

  private void updateFile(DropboxAPI.Entry entry) {
    try {
      File file = new File(getRoot() + "/" + entry.path);
      File dir = file.getParentFile();
      dir.mkdirs();
      long remoteFileTime = RESTUtility.parseDate(entry.modified).getTime();
      long myFileTime = file.lastModified();
      if (!file.exists() || myFileTime < remoteFileTime) {
        file.delete();
        file.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        mApi.getFile(entry.path, null, fileOutputStream, null);
        file.setLastModified(remoteFileTime);
        System.out.println(TAG + " updated: " + file.getAbsolutePath() + ", revision: " + entry.rev);
      } else {
        System.out.println(TAG + " not updated: " + file.getAbsolutePath() + ", revision: " + entry.rev);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private List<DropboxAPI.Entry> getFiles() {
    List<DropboxAPI.Entry> files = new ArrayList<DropboxAPI.Entry>();
    inflateFilesRecursively(files, "/" + getAccount() + "/Dropbox");
    return files;
  }

  private void inflateFilesRecursively(List<DropboxAPI.Entry> result, String path) {
    try {
      DropboxAPI.Entry entries = metadataWithDeleted(path, -1, null, true, null);
      for (DropboxAPI.Entry entry : entries.contents) {
        if (entry.isDir) {
          inflateFilesRecursively(result, entry.path);
        } else {
          result.add(entry);
        }
      }
    } catch (DropboxException e) {
      e.printStackTrace();
    }
  }

  /*private API*/

  private void updateUI() {
    boolean isInstalled = isInstalled();
    myActivity.findViewById(R.id.install).setVisibility(isInstalled ? View.GONE : View.VISIBLE);
    myActivity.findViewById(R.id.workspace).setVisibility(isInstalled ? View.VISIBLE : View.GONE);
    loginButton.setText(isLogin() ? "Detach" : "Attach");
    myActivity.findViewById(R.id.dropbox).setVisibility(isLogin() ? View.VISIBLE : View.GONE);
    accountTextView.setVisibility(isLogin() ? View.VISIBLE : View.GONE);
    if (isLogin()) {
      accountTextView.setText(getAccount());
    }
    if (counter == 0) {
      dropboxAnimation.updated();
    } else {
      dropboxAnimation.updating();
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

  private String getAccount() {
    try {
      return mApi.accountInfo().email;
    } catch (DropboxException e) {
      return null;
    }
  }

  //

  private String getRoot() {
    String packageName = myActivity.getPackageName();
    File dir;
    if (isExternalStorageAvailable()) {
      File sdDir = android.os.Environment.getExternalStorageDirectory();
      dir = new File(sdDir, "/Android/data/" + packageName);
    } else {
      dir = new File("/data/data/" + packageName);
    }
    if (!dir.exists()) {
      dir.mkdirs();
    }
    return dir.getAbsolutePath() + "/Dropbox/";
  }

  private static boolean isExternalStorageAvailable() {
    boolean mExternalStorageAvailable;
    boolean mExternalStorageWriteable;
    String state = android.os.Environment.getExternalStorageState();
    if (android.os.Environment.MEDIA_MOUNTED.equals(state)) {
      // We can read and write the media
      mExternalStorageAvailable = mExternalStorageWriteable = true;
    } else if (android.os.Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
      // We can only read the media
      mExternalStorageAvailable = true;
      mExternalStorageWriteable = false;
    } else {
      // Something else is wrong. It may be one of many other states,
      // but all we need to know is we can neither read nor write
      mExternalStorageAvailable = mExternalStorageWriteable = false;
    }
    return mExternalStorageAvailable && mExternalStorageWriteable;
  }

}
