package fzoli.crypto.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "loopback_log")
public class LoopbackLog {
    
    @DatabaseField(id = true)
    private String mapper;
    
    @DatabaseField(canBeNull = false, unique = true)
    private String loDevice;
    
    public LoopbackLog() {
    }
    
    public LoopbackLog(String mapper, String loDevice) {
        this.mapper = mapper;
        this.loDevice = loDevice;
    }
    
    public String getMapper() {
        return mapper;
    }
    
    public String getLoDevice() {
        return loDevice;
    }
    
}