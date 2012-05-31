/*
 * Sonet - Android Social Networking Widget
 * Copyright (C) 2009 Bryan Emmanuel
 * 
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Bryan Emmanuel piusvelte@gmail.com
 */
package com.piusvelte.sonet.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.Sonet.Entities;
import com.piusvelte.sonet.core.Sonet.Statuses;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

public class SonetCrypto {
	private static SonetCrypto instance;
	private SecretKey mSecretKey = null;
	protected static final String ALIAS = "Sonet";
	protected static final String PASSWORD = "password";
	protected static final String KEYSTORE = "sonet.bks";
	private static final String TAG = "SonetCrypto";

	private SonetCrypto(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String password = prefs.getString(PASSWORD, null);
		if (password != null) {
			try {
				KeyStore ks = KeyStore.getInstance("BKS");
				FileInputStream fis = context.openFileInput(KEYSTORE);
				ks.load(fis, password.toCharArray());
				fis.close();
				mSecretKey = (SecretKey) ks.getKey(ALIAS, password.toCharArray());
			} catch (NoSuchAlgorithmException e) {
				Log.e(TAG,e.toString());
			} catch (CertificateException e) {
				Log.e(TAG,e.toString());
			} catch (FileNotFoundException e) {
				Log.e(TAG,e.toString());
			} catch (IOException e) {
				Log.e(TAG,e.toString());
			} catch (KeyStoreException e) {
				Log.e(TAG,e.toString());
			} catch (UnrecoverableKeyException e) {
				Log.e(TAG,e.toString());
			}
		} else {
			try {
				// generate aes key
				KeyGenerator kgen = KeyGenerator.getInstance("AES");
				kgen.init(256);
				mSecretKey = kgen.generateKey();
				// generate password
				SecureRandom sr = new SecureRandom();
				password = new BigInteger(256, sr).toString(8);
				// create a keystore
				KeyStore ks = KeyStore.getInstance("BKS");
				ks.load(null, password.toCharArray());
				ks.setKeyEntry(ALIAS, mSecretKey, password.toCharArray(), null);
				FileOutputStream fos = context.openFileOutput(KEYSTORE, Context.MODE_PRIVATE);
				ks.store(fos, password.toCharArray());
				fos.close();
				// store the password
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString(PASSWORD, password);
				editor.commit();
				// encrypt the oauth data
				Cursor accounts = context.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET, Accounts.SID}, Accounts.SERVICE + "!=?", new String[]{Integer.toString(Sonet.SMS)}, null);
				if (accounts.moveToFirst()) {
					while (!accounts.isAfterLast()) {
						// encryption will occur in the provider, pass unencrypted data back in
						ContentValues values = new ContentValues();
						values.put(Accounts.TOKEN, RemoveUnderscore(accounts.getString(1)));
						values.put(Accounts.SECRET, RemoveUnderscore(accounts.getString(2)));
						values.put(Accounts.SID, RemoveUnderscore(accounts.getString(3)));
						context.getContentResolver().update(Accounts.CONTENT_URI, values, Accounts._ID + "=?", new String[]{Long.toString(accounts.getLong(0))});
						accounts.moveToNext();
					}
				}
				accounts.close();
				// encrypt the SIDs and ESIDs everywhere
				Cursor entities = context.getContentResolver().query(Entities.CONTENT_URI, new String[]{Entities._ID, Entities.ESID}, null, null, null);
				if (entities.moveToFirst()) {
					while (!entities.isAfterLast()) {
						ContentValues values = new ContentValues();
						values.put(Entities.ESID, RemoveUnderscore(entities.getString(1)));
						context.getContentResolver().update(Entities.CONTENT_URI, values, Entities._ID + "=?", new String[]{Long.toString(entities.getLong(0))});
						entities.moveToNext();
					}
				}
				entities.close();
				Cursor statuses = context.getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID, Statuses.SID}, null, null, null);
				if (statuses.moveToFirst()) {
					while (!statuses.isAfterLast()) {
						ContentValues values = new ContentValues();
						values.put(Statuses.SID, RemoveUnderscore(statuses.getString(1)));
						context.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses._ID + "=?", new String[]{Long.toString(statuses.getLong(0))});
						statuses.moveToNext();
					}
				}
				statuses.close();
			} catch (NoSuchAlgorithmException e) {
				Log.e(TAG,e.toString());
			} catch (KeyStoreException e) {
				Log.e(TAG,e.toString());
			} catch (CertificateException e) {
				Log.e(TAG,e.toString());
			} catch (IOException e) {
				Log.e(TAG,e.toString());
			}
		}		
	}

	protected static synchronized SonetCrypto getInstance(Context context) {
		if (instance == null) {
			instance = new SonetCrypto(context);
		}
		return instance;
	}

	protected String Decrypt(String data) {
		if ((mSecretKey != null) && (data != null)) {
			try {
				Cipher cipher = Cipher.getInstance("AES");
				cipher.init(Cipher.DECRYPT_MODE, mSecretKey);
				return new String(cipher.doFinal(Base64.decode(data, Base64.DEFAULT)), "UTF-8");
			} catch (NoSuchAlgorithmException e) {
				Log.e(TAG,e.toString());
			} catch (NoSuchPaddingException e) {
				Log.e(TAG,e.toString());
			} catch (InvalidKeyException e) {
				Log.e(TAG,e.toString());
			} catch (IllegalBlockSizeException e) {
				Log.e(TAG,e.toString());
			} catch (BadPaddingException e) {
				Log.e(TAG,e.toString());
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG,e.toString());
			}
		}
		return null;
	}

	protected String Encrypt(String data) {
		if ((mSecretKey != null) && (data != null)) {
			try {
				Cipher cipher = Cipher.getInstance("AES");
				cipher.init(Cipher.ENCRYPT_MODE, mSecretKey);
				return Base64.encodeToString(cipher.doFinal(data.getBytes("UTF-8")), Base64.DEFAULT);
			} catch (NoSuchAlgorithmException e) {
				Log.e(TAG,e.toString());
			} catch (NoSuchPaddingException e) {
				Log.e(TAG,e.toString());
			} catch (InvalidKeyException e) {
				Log.e(TAG,e.toString());
			} catch (IllegalBlockSizeException e) {
				Log.e(TAG,e.toString());
			} catch (BadPaddingException e) {
				Log.e(TAG,e.toString());
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG,e.toString());
			}
		}
		return null;
	}

	protected static String RemoveUnderscore(String sid) {
		return (sid != null) && (sid.length() != 0) && sid.substring(0, 1).equals("_") ? sid.substring(1) : sid;
	}

}
