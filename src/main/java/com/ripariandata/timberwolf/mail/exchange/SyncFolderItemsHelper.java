/**
 * Copyright 2012 Riparian Data
 * http://www.ripariandata.com
 * contact@ripariandata.com
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
package com.ripariandata.timberwolf.mail.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.messages.SyncFolderItemsResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.SyncFolderItemsResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.SyncFolderItemsType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.SyncFolderItemsCreateOrUpdateType;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Contains helper methods for SyncFolderItems requests. */
public final class SyncFolderItemsHelper
{
    private static final Logger LOG = LoggerFactory.getLogger(SyncFolderItemsHelper.class);
    /**
     * If you try to request a number of changes greater than this, exchange
     * faults.
     */
    public static final int MAX_SYNC_COUNT = 512;

    /** Enforces not being able to create an instance. */
    private SyncFolderItemsHelper()
    {

    }

    /**
     * Creates a SyncFolderItemsType to request all the new items under the
     * given folder context.
     *
     * @param config The configuration for this instance of Timberwolf.
     * @param folder The current folder being searched.
     * @return The SyncFolderItemsType necessary to request new items from
     *         the given folder since the given sync state.
     */
    public static SyncFolderItemsType getSyncFolderItemsRequest(final Configuration config, final FolderContext folder)
    {
        SyncFolderItemsType syncFolderItems = SyncFolderItemsType.Factory.newInstance();
        syncFolderItems.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);
        syncFolderItems.setSyncFolderId(folder.getTargetFolder());
        syncFolderItems.setSyncState(folder.getSyncStateToken());
        syncFolderItems.setMaxChangesReturned(Math.min(MAX_SYNC_COUNT, config.getIdPageSize()));
        return syncFolderItems;
    }

    /**
     * Gets a list of all the new ids for the given folder of the current user.
     *
     * @param exchangeService The actual service to use when requesting ids.
     * @param config The configuration for this instance of Timberwolf.
     * @param folder The folder to sync.
     * @return The SyncFolderItems result returned from Exchange.
     */
    public static SyncFolderItemsResult syncFolderItems(final ExchangeService exchangeService,
                                                        final Configuration config,
                                                        final FolderContext folder)
            throws ServiceCallException, HttpErrorException
    {
        return syncFolderItems(exchangeService, getSyncFolderItemsRequest(config, folder), folder.getUser(), folder);
    }

    /**
     * Gets a list of all the new ids for the given folder of the current user.
     *
     * @param exchangeService The actual service to use when requesting ids.
     * @param syncFolderItemsRequest The request to send to exchange.
     * @param targetUser The user to impersonate for this request.
     * @param folder The folder to sync.
     * @return The SyncFolderItems result return from Exchange.
     */
    private static SyncFolderItemsResult syncFolderItems(final ExchangeService exchangeService,
                                                         final SyncFolderItemsType syncFolderItemsRequest,
                                                         final String targetUser, final FolderContext folder)
            throws ServiceCallException, HttpErrorException
    {
        SyncFolderItemsResponseType response = exchangeService.syncFolderItems(syncFolderItemsRequest, targetUser);
        if (response == null)
        {
            LOG.debug("Exchange service returned null sync folder items response for request:\n{}",
                      syncFolderItemsRequest);
            throw new ServiceCallException(ServiceCallException.Reason.OTHER, "Null response from Exchange service.");
        }
        ArrayOfResponseMessagesType array = response.getResponseMessages();
        SyncFolderItemsResult result = new SyncFolderItemsResult(folder.getSyncStateToken());
        boolean hasMessages = false;
        for (SyncFolderItemsResponseMessageType message : array.getSyncFolderItemsResponseMessageArray())
        {
            hasMessages = true;
            ResponseCodeType.Enum errorCode = message.getResponseCode();
            if (errorCode != null && errorCode != ResponseCodeType.NO_ERROR)
            {
                LOG.debug(errorCode.toString());
                throw new ServiceCallException(errorCode, "SOAP response contained an error.");
            }
            if (message.isSetSyncState())
            {
                result.setSyncState(message.getSyncState());
            }
            if (message.isSetIncludesLastItemInRange())
            {
                result.setIncludesLastItem(message.getIncludesLastItemInRange());
            }

            if (message.isSetChanges())
            {
                // There's also Update and Delete arrays, but we're not dealing with them yet
                for (SyncFolderItemsCreateOrUpdateType create : message.getChanges().getCreateArray())
                {
                    if (create.isSetMessage() && create.getMessage().isSetItemId())
                    {
                        result.getIds().add(create.getMessage().getItemId().getId());
                    }
                }
            }
        }
        if (!hasMessages)
        {
            LOG.debug("Exchange responded without any messages");
            // so that we don't keep calling over and over again
            result.setIncludesLastItem(true);
        }
        return result;
    }

    /** The result returned from syncing a folder's items. */
    public static final class SyncFolderItemsResult
    {
        private final Vector<String> ids;
        private boolean includesLastItem;
        private String syncState;

        private SyncFolderItemsResult(final String oldSyncState)
        {
            ids = new Vector<String>();
            syncState = oldSyncState;
        }

        /** The ids returned by the sync request. */
        public Vector<String> getIds()
        {
            return ids;
        }

        /**
         * The sync state returned by the request.
         * If there was a problem, this will be the same sync state
         * as the one passed in.
         *
         * @return The sync state returned by the request.
         */
        public String getSyncState()
        {
            return syncState;
        }

        /**
         * Whether or not the sync response included
         * the last item to be synced.
         *
         * @return true if all items have been returned.
         */
        public boolean includesLastItem()
        {
            return includesLastItem;
        }

        private void setIncludesLastItem(final boolean includesLastItemFromResponse)
        {
            includesLastItem = includesLastItemFromResponse;
        }

        private void setSyncState(final String newSyncState)
        {
            syncState = newSyncState;
        }

    }

}
