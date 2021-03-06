/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.group

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.internal.database.model.GroupEntity
import im.vector.matrix.android.internal.database.model.GroupSummaryEntity
import im.vector.matrix.android.internal.database.query.getOrCreate
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.group.model.GroupRooms
import im.vector.matrix.android.internal.session.group.model.GroupSummaryResponse
import im.vector.matrix.android.internal.session.group.model.GroupUsers
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.awaitTransaction
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import javax.inject.Inject

internal interface GetGroupDataTask : Task<GetGroupDataTask.Params, Unit> {
    sealed class Params {
        object FetchAllActive : Params()
        data class FetchWithIds(val groupIds: List<String>) : Params()
    }
}

internal class DefaultGetGroupDataTask @Inject constructor(
        private val groupAPI: GroupAPI,
        @SessionDatabase private val monarchy: Monarchy,
        private val eventBus: EventBus
) : GetGroupDataTask {

    private data class GroupData(
            val groupId: String,
            val groupSummary: GroupSummaryResponse,
            val groupRooms: GroupRooms,
            val groupUsers: GroupUsers
    )

    override suspend fun execute(params: GetGroupDataTask.Params) {
        val groupIds = when (params) {
            is GetGroupDataTask.Params.FetchAllActive -> {
                getActiveGroupIds()
            }
            is GetGroupDataTask.Params.FetchWithIds   -> {
                params.groupIds
            }
        }
        Timber.v("Fetch data for group with ids: ${groupIds.joinToString(";")}")
        val data = groupIds.map { groupId ->
            val groupSummary = executeRequest<GroupSummaryResponse>(eventBus) {
                apiCall = groupAPI.getSummary(groupId)
            }
            val groupRooms = executeRequest<GroupRooms>(eventBus) {
                apiCall = groupAPI.getRooms(groupId)
            }
            val groupUsers = executeRequest<GroupUsers>(eventBus) {
                apiCall = groupAPI.getUsers(groupId)
            }
            GroupData(groupId, groupSummary, groupRooms, groupUsers)
        }
        insertInDb(data)
    }

    private fun getActiveGroupIds(): List<String> {
        return monarchy.fetchAllMappedSync(
                { realm ->
                    GroupEntity.where(realm, Membership.activeMemberships())
                },
                { it.groupId }
        )
    }

    private suspend fun insertInDb(groupDataList: List<GroupData>) {
        monarchy
                .awaitTransaction { realm ->
                    groupDataList.forEach { groupData ->

                        val groupSummaryEntity = GroupSummaryEntity.getOrCreate(realm, groupData.groupId)

                        groupSummaryEntity.avatarUrl = groupData.groupSummary.profile?.avatarUrl ?: ""
                        val name = groupData.groupSummary.profile?.name
                        groupSummaryEntity.displayName = if (name.isNullOrEmpty()) groupData.groupId else name
                        groupSummaryEntity.shortDescription = groupData.groupSummary.profile?.shortDescription ?: ""

                        groupSummaryEntity.roomIds.clear()
                        groupData.groupRooms.rooms.mapTo(groupSummaryEntity.roomIds) { it.roomId }

                        groupSummaryEntity.userIds.clear()
                        groupData.groupUsers.users.mapTo(groupSummaryEntity.userIds) { it.userId }
                    }
                }
    }
}
