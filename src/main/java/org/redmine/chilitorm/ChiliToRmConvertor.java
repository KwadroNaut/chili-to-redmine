package org.redmine.chilitorm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.annotation.Transactional;

public class ChiliToRmConvertor {

	
	private DataSource dataSource;

	/**
	 * @return the dataSource
	 */
	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	
	class Change {
		
		String type;
		String oldValue;
		String newValue;
		String property = "attr";

		public String toString() {
			return "type=" + type+", from: " + oldValue + ", to: " + newValue;
		}

		public void setType(String s) {
			if(s.startsWith("attachments")) {
				this.type = s.substring(11);
				this.property = "attachment";
			} else {
				this.type = s;
			}
		}
	}
	
	private Map<String, Change> toData(String data) {
		Map<String, Change> map = new TreeMap<String, ChiliToRmConvertor.Change>();
		if(data != null && data.trim().length() > 0) {
			String[] as = data.split("\n-");
			Change lastChange = new Change();
			for(int i = 0; i < as.length; i++) {
				String s = as[i].trim();
				if(s.endsWith("{}")) {
					continue;
				}
				if(s.startsWith("---")) {
					lastChange = new Change();
					lastChange.setType(s.substring(3, s.lastIndexOf(':')).trim());
					map.put(lastChange.type, lastChange);
				} else {
					if(lastChange.oldValue == null) {
						lastChange.oldValue = toVal(s);
					} else {
						if(as.length > i + 1 && s.endsWith(":")) {
							if(s.lastIndexOf('\n') < 0) {
								lastChange.newValue = "(unknown)";
								lastChange = new Change();
								lastChange.setType(s);
								map.put(lastChange.type, lastChange);
							} else {
								lastChange.newValue = toVal(s.substring(0, s.lastIndexOf('\n') < 0 ? 0 : s.lastIndexOf('\n')));
								lastChange = new Change();
								lastChange.setType(s.substring(s.lastIndexOf('\n') + 1, s.lastIndexOf(':')));
								map.put(lastChange.type, lastChange);
							}
						} else {
							lastChange.newValue = toVal(s);
						}
					}
				}
			}
		}
		return map;
	}

	private String toVal(String s) {
		if(s == null) {
			return null;
		}
		if(s.trim().length() == 0) {
			return "";
		}
		if(s.length() < 32) {
			return s;
		}
		s = s.replace("\\r", "\r").replace("\\n", "\n").replace("\\t", "\t");
		StringBuilder sb = new StringBuilder();
		char ac[] = s.toCharArray();
		for(int i= 0; i < ac.length; i++) {
			if(ac[i] == '\\' && i + 3 <ac.length && ac[i + 1] == 'x') {
				sb.append((char)Integer.parseInt(ac[i + 2] + "" + ac[i + 3], 16));
			}
			sb.append(ac[i]);
		}
		
		return null;
	}

	@Transactional
	public void doConvert() {
		NamedParameterJdbcTemplate tpl = new NamedParameterJdbcTemplate(dataSource);
		
		List<Map<String, Object>> allRows = tpl.queryForList("select id, journalized_id, changes_chili from journals where journalized_type='Issue'", new TreeMap());
		for(Map<String, Object> m : allRows) {
			Integer id = (Integer) m.get("id");

			System.out.println("\njournal.id " + id);
			Map<String, Change> data = toData((String) m.get("changes_chili"));
			if(isNewRecord(data)) {
				System.out.println("  skipped due new record.");
			} else {
				for(String key : data.keySet()) {
					System.out.println("  " + data.get(key).property + " " + data.get(key).type + ": " + val(data.get(key).oldValue) + " => " + val(data.get(key).newValue));
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("journal_id", m.get("id"));
					map.put("property", data.get(key).property);
					map.put("prop_key", data.get(key).type);
					map.put("old_value", data.get(key).oldValue);
					map.put("value", data.get(key).newValue);
					tpl.update("insert into journal_details (journal_id, property, prop_key, old_value, value) values(:journal_id, :property, :prop_key, :old_value, :value)", map);
				}
			}
		}
		
	}

	private String val(String s) {
		return s == null || s.trim().length() == 0 ? "''" : s;
	}

	private boolean isNewRecord(Map<String, Change> data) {
		if(data.get("project_id") == null || !data.get("project_id").oldValue.trim().equals("0")) {
			return false;
		}
		if(data.get("author_id") == null || !data.get("author_id").oldValue.trim().equals("0")) {
			return false;
		}
		if(data.get("created_on") == null || !isEmpty(data.get("created_on").oldValue)) {
			return false;
		}
		return true;
	}

	private boolean isEmpty(String s) {
		
		return s == null || s.trim().length() == 0 ? true : false;
	}
	
	
}
