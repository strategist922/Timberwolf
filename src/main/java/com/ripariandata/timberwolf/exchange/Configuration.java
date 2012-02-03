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
package com.ripariandata.timberwolf.exchange;

import com.ripariandata.timberwolf.InMemoryUserFolderSyncStateStorage;
import com.ripariandata.timberwolf.UserFolderSyncStateStorage;

/**
 * This class contains any configurable settings
 * that will effect the exchange service calls.
 */
public class Configuration
{
    private final int idPageSize;
    private final int itemPageSize;
    private UserFolderSyncStateStorage syncStateStorage;

    /**
     * @param idPageSize Must be greater than or equal to 1.
     * @param itemPageSize Must be greater than or equal to 1.
     * @param userFolderSyncStateStorage The storage that maintains sync states
     * for all the folders for all the users.
     */
    public Configuration(final int idPageSize, final int itemPageSize,
                         final UserFolderSyncStateStorage userFolderSyncStateStorage)
    {
        // Asking for negative or zero max items is nonsensical.
        this.idPageSize = Math.max(idPageSize, 1);
        this.itemPageSize = Math.max(itemPageSize, 1);
        syncStateStorage = userFolderSyncStateStorage;
    }

    /**
     * Creates a configuration with the startDate set to the beginning of the epoch.
     *
     * @param idPageSize Must be greater than or equal to 1.
     * @param itemPageSize Must be greater than or equal to 1.
     */
    public Configuration(final int idPageSize, final int itemPageSize)
    {
        this(idPageSize, itemPageSize, new InMemoryUserFolderSyncStateStorage());
    }

    public int getIdPageSize()
    {
        return idPageSize;
    }

    public int getItemPageSize()
    {
        return itemPageSize;
    }

    public UserFolderSyncStateStorage getSyncStateStorage()
    {
        return syncStateStorage;
    }

    public Configuration withSyncStateStorage(final UserFolderSyncStateStorage userFolderSyncStateStorage)
    {
        return new Configuration(idPageSize, itemPageSize, userFolderSyncStateStorage);
    }
}
