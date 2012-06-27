package fzoli.crypto.model;

import java.util.ArrayList;
import java.util.List;

import fzoli.crypto.database.ListEntry;

public class ListEntryStorage extends ArrayList<ListEntry> {
	
	private static final long serialVersionUID = 1L;

	public ListEntryStorage() {
		super();
	}
	
	public ListEntryStorage(List<ListEntry> entries) {
		super(entries);
	}
	
	public List<String> getNames() {
		List<String> names = new ArrayList<String>();
		for (ListEntry entry : this) {
			names.add(entry.getName());
		}
		return names;
	}
	
	@Override
	public boolean contains(Object object) {
		if (!(object instanceof ListEntry)) return false;
		return findEntry(((ListEntry) object).getId()) != null;
	}
	
	public int findIndex(ListEntry entry) {
		return findIndex(entry.getId());
	}
	
	private int findIndex(Integer id) {
		return indexOf(findEntry(id));
	}
	
	public ListEntry findEntry(Integer id) {
		for (ListEntry entry : this) {
			if (entry.getId().equals(id)) return entry;
		}
		return null;
	}
	
	public boolean removeEntry(ListEntry entry) {
		if (entry == null) return false;
		ListEntry e = findEntry(entry.getId());
		if (e != null) remove(e);
		return e != null;
	}
	
}