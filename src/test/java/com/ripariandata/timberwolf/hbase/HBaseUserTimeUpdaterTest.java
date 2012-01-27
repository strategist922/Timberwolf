package com.ripariandata.timberwolf.hbase;

import com.ripariandata.timberwolf.MockHTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

/** Tests the behavior of HBaseUserTimeUpdaters. */
public class HBaseUserTimeUpdaterTest
{
    private HBaseManager manager = new HBaseManager();

    private IHBaseTable mockTable(final HBaseManager hbaseManager, final String tableName)
    {
        MockHTable table = MockHTable.create(tableName);
        HBaseTable hbaseTable = new HBaseTable(table);
        hbaseManager.addTable(hbaseTable);
        return hbaseTable;
    }

    @Test
    public void testLastUpdated()
    {
        String tableName = "testLastUpdated";
        IHBaseTable hbaseTable = mockTable(manager, tableName);

        String userName = "Robert the User";
        Put put = new Put(Bytes.toBytes(userName));
        final long time = 23488902348L;
        put.add(Bytes.toBytes("t"), Bytes.toBytes("d"), Bytes.toBytes(time));
        hbaseTable.put(put);
        hbaseTable.flush();

        HBaseUserTimeUpdater updates = new HBaseUserTimeUpdater(manager, tableName);
        DateTime date = updates.lastUpdated(userName);
        Assert.assertEquals(time, date.getMillis());
    }

    @Test
    public void testLastUpdatedNoUser()
    {
        String tableName = "testLastUpdatedNoUser";
        IHBaseTable hbaseTable = mockTable(manager, tableName);

        HBaseUserTimeUpdater updates = new HBaseUserTimeUpdater(manager, tableName);
        DateTime date = updates.lastUpdated("not actually a username");
        Assert.assertEquals(0L, date.getMillis());
    }

    @Test
    public void testUpdateUser()
    {
        String tableName = "testUpdateUser";
        IHBaseTable hbaseTable = mockTable(manager, tableName);

        HBaseUserTimeUpdater updates = new HBaseUserTimeUpdater(manager, tableName);
        final long time = 1234355L;
        String userName = "A Generic Username";
        updates.setUpdateTime(userName, new DateTime(time));
        Assert.assertEquals(time, updates.lastUpdated(userName).getMillis());
    }

    @Test
    public void testUpdateExistingUser()
    {
        // apparently if you put the code in twice it passes, if you just
        // have the code for this test once, it fails when you run it by
        // itself.
        String tableName = "testUpdateExistingUser";
        IHBaseTable hbaseTable = mockTable(manager, tableName);

        HBaseUserTimeUpdater updates = new HBaseUserTimeUpdater(manager, tableName);
        long time = 3425322L;
        String userName = "Some other username";
        updates.setUpdateTime(userName, new DateTime(time));
        Assert.assertEquals(time, updates.lastUpdated(userName).getMillis());
        updates.setUpdateTime(userName, new DateTime(2 * time));
        Assert.assertEquals(2 * time, updates.lastUpdated(userName).getMillis());

        // Repeat test
        tableName = "testUpdateExistingUser";
        hbaseTable = mockTable(manager, tableName);

        updates = new HBaseUserTimeUpdater(manager, tableName);
        time = 3425322L;
        userName = "Some other username";
        updates.setUpdateTime(userName, new DateTime(time));
        Assert.assertEquals(time, updates.lastUpdated(userName).getMillis());
        updates.setUpdateTime(userName, new DateTime(2 * time));
        Assert.assertEquals(2 * time, updates.lastUpdated(userName).getMillis());
    }
}
