package fzoli.crypto.database;

import java.io.Serializable;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "list_entry")
public class ListEntry implements Serializable {
    
	private static final long serialVersionUID = 1L;

	@DatabaseField(generatedId = true)
	private Integer id;
	
	@DatabaseField(canBeNull = false, unique = true)
    private String name;
    
    @DatabaseField(canBeNull = false)
    private String filePath, mountPath;
    
    public ListEntry() {
    }
    
    public ListEntry(String name, String filePath, String mountPath) {
        this.name = name;
        this.filePath = filePath;
        this.mountPath = mountPath;
    }
    
    public ListEntry(Integer id, String name, String filePath, String mountPath) {
    	this(name, filePath, mountPath);
    	this.id = id;
    }
    
    public Integer getId() {
    	return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public String getMountPath() {
        return mountPath;
    }
    
}