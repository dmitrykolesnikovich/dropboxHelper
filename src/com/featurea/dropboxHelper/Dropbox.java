package com.featurea.dropboxHelper;

import android.content.SharedPreferences;
import android.util.Log;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.RESTUtility;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;

import java.io.*;
import java.util.*;

public class Dropbox {

  private static final String TAG = "Dropbox";
  public static final String ID = "com.dropbox.android";
  private static final String APP_KEY = "p2e2ilowzjsm009";
  private static final String APP_SECRET = "mg0zkm6mxzd428o";
  private static final String ACCOUNT_PREFS_NAME = "prefs";
  private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
  private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";
  private DropboxAPI<AndroidAuthSession> mApi;

  public Dropbox() {
    AndroidAuthSession session = buildSession();
    mApi = new DropboxAPI<AndroidAuthSession>(session);
  }

  public void updateAccountStatus() {
    if (isLogin()) {
      logout();
    } else {
      login();
    }
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

  public void onResume() {
    AndroidAuthSession session = mApi.getSession();
    if (session.authenticationSuccessful()) {
      try {
        session.finishAuthentication();
        storeAuth(session);
      } catch (IllegalStateException e) {
        Log.i(TAG, "Error authenticating", e);
      }
    }
  }

  public void update() {
    if (isLogin()) {
      List<DropboxAPI.Entry> files = getFiles();
      System.out.println(TAG + " size: " + files.size());
      for (DropboxAPI.Entry entry : files) {
        System.out.println(entry.fileName());
        updateEntry(entry);
      }
      UpdaterService.updated();
    }
  }

  private void updateEntry(final DropboxAPI.Entry entry) {
    if (!isLogin()) {
      return;
    }
    if (entry.isDeleted) {
      deleteFile(entry);
    } else {
      if (!entry.isDir) {
        downloadFile(entry);
      }
    }
  }

  private void deleteFile(DropboxAPI.Entry entry) {
    File file = new File(DropboxHelperApp.getRoot() + "/" + entry.path);
    String message = file.getName();
    if (file.exists()) {
      /*UpdaterService.updating(message);*/
      file.delete();
      System.out.println(TAG + message);
    }
  }

  private void downloadFile(DropboxAPI.Entry entry) {
    try {
      File file = new File(DropboxHelperApp.getRoot() + "/" + entry.path);
      File dir = file.getParentFile();
      dir.mkdirs();
      long remoteFileTime = RESTUtility.parseDate(entry.modified).getTime();
      long myFileTime = file.lastModified();
      if (!file.exists() || myFileTime < remoteFileTime || file.length() != entry.bytes) {
        file.delete();
        file.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        mApi.getFile(entry.path, null, fileOutputStream, null);
        file.setLastModified(remoteFileTime);
        System.out.println(TAG + " UPDATE: " + file.getAbsolutePath());
        UpdaterService.updating(file.getName());
      } else {
        /*System.out.println(TAG + " not updated: " + file.getAbsolutePath() + ", revision: " + entry.rev);*/
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
      if (entries == null) {
        return;
      }
      for (DropboxAPI.Entry entry : entries.contents) {
        result.add(entry);
        if (entry.isDir) {
          inflateFilesRecursively(result, entry.path);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Shows keeping the access keys returned from Trusted Authenticator in a local
   * store, rather than storing user name & password, and re-authenticating each
   * time (which is not to be done, ever).
   */
  private void loadAuth(AndroidAuthSession session) {
    SharedPreferences prefs = DropboxHelperApp.instance.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
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
      SharedPreferences prefs = DropboxHelperApp.instance.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
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
      SharedPreferences prefs = DropboxHelperApp.instance.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
      SharedPreferences.Editor edit = prefs.edit();
      edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
      edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
      edit.commit();
      return;
    }
  }

  private void clearKeys() {
    SharedPreferences prefs = DropboxHelperApp.instance.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
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

  public boolean isLogin() {
    return mApi.getSession().isLinked();
  }

  private void logout() {
    mApi.getSession().unlink();
    clearKeys();
    UpdaterService.instance.stop();

  }

  private void login() {
    mApi.getSession().startOAuth2Authentication(DropboxHelperApp.instance);
  }

  public String getAccount() {
    try {
      return mApi.accountInfo().email;
    } catch (DropboxException e) {
      return null;
    }
  }

}
