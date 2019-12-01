/*
 * Copyright 2014 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
 * Copyright 2018 New Vector Ltd
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
package im.vector.matrix.android.internal.auth.login

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.internal.auth.registration.AuthParams

/**
 * Class to pass parameters to reset the password once a email has been validated.
 */
@JsonClass(generateAdapter = true)
internal data class ResetPasswordMailConfirmed(
        // authentication parameters
        @Json(name = "auth")
        val auth: AuthParams? = null,

        // the new password
        @Json(name = "new_password")
        val newPassword: String? = null
) {
    companion object {
        fun create(clientSecret: String, sid: String, newPassword: String): ResetPasswordMailConfirmed {
            return ResetPasswordMailConfirmed(
                    auth = AuthParams.createForResetPassword(clientSecret, sid),
                    newPassword = newPassword
            )
        }
    }
}