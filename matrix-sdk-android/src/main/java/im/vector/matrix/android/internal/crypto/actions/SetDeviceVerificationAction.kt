/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.crypto.actions

import im.vector.matrix.android.internal.crypto.crosssigning.DeviceTrustLevel
import im.vector.matrix.android.internal.crypto.keysbackup.DefaultKeysBackupService
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.di.UserId
import timber.log.Timber
import javax.inject.Inject

internal class SetDeviceVerificationAction @Inject constructor(
        private val cryptoStore: IMXCryptoStore,
        @UserId private val userId: String,
        private val defaultKeysBackupService: DefaultKeysBackupService) {

    fun handle(trustLevel: DeviceTrustLevel, userId: String, deviceId: String) {
        val device = cryptoStore.getUserDevice(userId, deviceId)

        // Sanity check
        if (null == device) {
            Timber.w("## setDeviceVerification() : Unknown device $userId:$deviceId")
            return
        }

        if (device.isVerified != trustLevel.isVerified()) {
            if (userId == this.userId) {
                // If one of the user's own devices is being marked as verified / unverified,
                // check the key backup status, since whether or not we use this depends on
                // whether it has a signature from a verified device
                defaultKeysBackupService.checkAndStartKeysBackup()
            }
        }

        if (device.trustLevel != trustLevel) {
            device.trustLevel = trustLevel
            cryptoStore.storeUserDevice(userId, device)
        }
    }
}
