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
package com.piusvelte.sonet;

import static com.piusvelte.sonet.SonetProvider.DATABASE_NAME;
import static com.piusvelte.sonet.SonetCrypto.KEYSTORE;
import static com.piusvelte.sonet.Sonet.sDatabaseLock;

import java.io.IOException;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;

public class SonetBackupAgent extends BackupAgentHelper {
	
	public void onCreate() {
		SharedPreferencesBackupHelper sharedPrefs = new SharedPreferencesBackupHelper(this, getPackageName() + "_preferences");
		addHelper(getPackageName() + "_preferences", sharedPrefs);
		
		FileBackupHelper keystore = new FileBackupHelper(this, KEYSTORE);
		addHelper(KEYSTORE, keystore);
		
		FileBackupHelper database = new FileBackupHelper(this, "../databases/" + DATABASE_NAME);
		addHelper(DATABASE_NAME, database);
	}
	
	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
		synchronized (sDatabaseLock) {
			super.onBackup(oldState, data, newState);
		}
	}
	
	@Override
	public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
		synchronized (sDatabaseLock) {
			super.onRestore(data, appVersionCode, newState);
		}
	}

}
