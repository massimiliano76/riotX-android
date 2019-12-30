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

package im.vector.riotx.core.error

import im.vector.riotx.BuildConfig
import timber.log.Timber

/**
 * throw in debug, only log in production. As this method does not always throw, next statement should be a return
 */
fun fatalError(message: String) {
    if (BuildConfig.DEBUG) {
        error(message)
    } else {
        Timber.e(message)
    }
}