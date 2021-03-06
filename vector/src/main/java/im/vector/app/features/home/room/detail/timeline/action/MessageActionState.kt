/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.action

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.core.extensions.canReact
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Quick reactions state
 */
data class ToggleState(
        val reaction: String,
        val isSelected: Boolean
)

data class ActionPermissions(
        val canSendMessage: Boolean = false,
        val canReact: Boolean = false,
        val canRedact: Boolean = false
)

data class MessageActionState(
        val roomId: String,
        val eventId: String,
        val informationData: MessageInformationData,
        val timelineEvent: Async<TimelineEvent> = Uninitialized,
        val messageBody: CharSequence = "",
        // For quick reactions
        val quickStates: Async<List<ToggleState>> = Uninitialized,
        // For actions
        val actions: List<EventSharedAction> = emptyList(),
        val expendedReportContentMenu: Boolean = false,
        val actionPermissions: ActionPermissions = ActionPermissions()
) : MvRxState {

    constructor(args: TimelineEventFragmentArgs) : this(roomId = args.roomId, eventId = args.eventId, informationData = args.informationData)

    private val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm", Locale.getDefault())

    fun senderName(): String = informationData.memberName?.toString() ?: ""

    fun time(): String? = timelineEvent()?.root?.originServerTs?.let { dateFormat.format(Date(it)) } ?: ""

    fun canReact() = timelineEvent()?.canReact() == true && actionPermissions.canReact
}
