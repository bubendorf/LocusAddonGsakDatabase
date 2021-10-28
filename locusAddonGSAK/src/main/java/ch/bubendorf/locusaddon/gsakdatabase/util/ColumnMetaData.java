/*
 * DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 * Version 2, December 2004
 *
 * Copyright (C) 2004 Sam Hocevar <sam@hocevar.net>
 *
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document, and changing it is allowed as long
 * as the name is changed.
 *
 * DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 * TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 * 0. You just DO WHAT THE FUCK YOU WANT TO.
 */

package ch.bubendorf.locusaddon.gsakdatabase.util;

import java.util.Comparator;

public class ColumnMetaData {

    public static final Comparator<ColumnMetaData> COLUMN_NAME_COMPARATOR = Comparator.comparing(ColumnMetaData::getColumnName);
    public static final Comparator<ColumnMetaData> TABLE_NAME_COMPARATOR = Comparator.comparing(ColumnMetaData::getTableName);
    public static final Comparator<ColumnMetaData> COMPARATOR = TABLE_NAME_COMPARATOR.reversed().thenComparing(COLUMN_NAME_COMPARATOR);

    private final String tableName;
    private final String columnName;
    private final String type;

    public ColumnMetaData(final String tableName, final String columnName, final String type) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.type = type;
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getType() {
        return type;
    }

}
