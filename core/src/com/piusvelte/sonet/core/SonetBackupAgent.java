/*
 * Sonet - Android Social Networking Widget
 * Copyright (C) 2012 Bryan Emmanuel
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

import java.io.IOException;

import android.annotation.TargetApi;
import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;

@TargetApi(8)
public class SonetBackupAgent extends BackupAgentHelper {

	@Override
	public void onCreate() {
		addHelper(SonetProvider.DATABASE_NAME, new FileBackupHelper(this, "../databases/" + SonetProvider.DATABASE_NAME));
		addHelper(getString(R.string.key_preferences), new SharedPreferencesBackupHelper(this, getString(R.string.key_preferences)));
	}
	
	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
	          ParcelFileDescriptor newState) throws IOException {
	    synchronized (Sonet.sDatabaseLock) {
	        super.onBackup(oldState, data, newState);
	    }
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode,
	        ParcelFileDescriptor newState) throws IOException {
	    synchronized (Sonet.sDatabaseLock) {
	        super.onRestore(data, appVersionCode, newState);
	    }
	}
}
