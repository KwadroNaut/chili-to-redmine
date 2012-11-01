package org.redmine.chilitorm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Driver;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

public class DataSourceCreator {

	
	private File cfgFile;
	
	private String cfgType;

	public DataSourceCreator(File cfgfile, String cfgType) {
		setCfgFile(cfgfile);
		setCfgType(cfgType);
	}

	private class Cfg {

		String name;
		String databaseType;
		String database;
		String host;
		String username;
		String password;
		String encoding;
		
		
		String getUrl() {
			if(databaseType.equals("mysql")) {
				return String.format("jdbc:mysql://%s/%s", host, database);
			} else if(databaseType.equals("postgres")) {
				return String.format("jdbc:postgresql://%s/%s", host, database);
			} else {
				throw new IllegalStateException(String.format("Database type '%s' is not supported", databaseType));
				
			}
		}
		
		public String getDriver() {
			if(databaseType.equals("mysql")) {
				return "com.mysql.jdbc.Driver";
			} else if(databaseType.equals("postgres")) {
				return "org.postgresql.Driver";
			} else {
				throw new IllegalStateException(String.format("Database type '%s' is not supported", databaseType));
				
			}
		}
		
		public String toString() {
			return getClass() + ": databaseType="+databaseType+"";
		}
		
	}
	
	private Map<String, Cfg> loadConfigs() {
		Map<String, Cfg> map = new HashMap<String, DataSourceCreator.Cfg>();
		try {
			BufferedReader brdr = new BufferedReader(new FileReader(cfgFile));
			
			String row;
			Cfg lastCfg = null;
			while((row = brdr.readLine()) != null) {
				if(row.trim().length() > 0 && !row.trim().startsWith("#")) {
					if(Character.isWhitespace(row.charAt(0))) {
						String row2 = getClerarRow(row);
						if(lastCfg != null) {
							if(row2.trim().startsWith("adapter:")) {
								lastCfg.databaseType = row2.trim().substring(8).trim().toLowerCase();
							} else if(row2.trim().startsWith("database:")) {
								lastCfg.database = row2.trim().substring(9).trim().toLowerCase();
							} else if(row2.trim().startsWith("host:")) {
								lastCfg.host = row2.trim().substring(5).trim().toLowerCase();
							} else if(row2.trim().startsWith("username:")) {
								lastCfg.username = row2.trim().substring(9).trim().toLowerCase();
							} else if(row2.trim().startsWith("password:")) {
								lastCfg.password = row2.trim().substring(9).trim().toLowerCase();
							} else if(row2.trim().startsWith("encoding:")) {
								lastCfg.encoding = row2.trim().substring(9).trim().toLowerCase();
							}
						}
					} else {
						lastCfg = new Cfg();
						lastCfg.name = row.trim().substring(0, row.length() - 1);
						map.put(lastCfg.name, lastCfg);
					}
				}
			}
			brdr.close();
			return map;
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	private String getClerarRow(String row) {
		String nrow;
		if(row != null && (nrow = row.trim()).length() > 0) {
			if(nrow.indexOf('#') > 0) {
				return nrow.substring(0, nrow.indexOf('#'));
			} else {
				return nrow;
			}
		}
		return "";
	}

	@SuppressWarnings("unchecked")
	public DataSource getDataSource() {
		Map<String, Cfg> map = loadConfigs();
		
		SimpleDriverDataSource ds= new SimpleDriverDataSource();
		Cfg cfg = map.get(cfgType);
		if(cfg == null) {
			throw new IllegalArgumentException(String.format("Configuration '%s' does not exist in file '%s'", cfgType, cfgFile));
		}
		
		try {
			ds.setDriverClass((Class<? extends Driver>) Class.forName(cfg.getDriver()));
			ds.setUrl(cfg.getUrl());
			ds.setUsername(cfg.username);
			ds.setPassword(cfg.password);
			if(cfg.encoding != null && cfg.encoding.trim().length() > 0) {
				Properties pp = new Properties();
				pp.setProperty("characterEncoding ", cfg.encoding);
				ds.setConnectionProperties(pp);
			}
		} catch (Exception ex) {
			throw ex instanceof RuntimeException ? (RuntimeException)ex : new IllegalStateException(ex);
		}

		return ds;
	}

	/**
	 * @return the cfgFil
	 */
	public File getCfgFile() {
		return cfgFile;
	}

	/**
	 * @param cfgFil the cfgFil to set
	 */
	public void setCfgFile(File cfgFil) {
		this.cfgFile = cfgFil;
	}

	/**
	 * @return the cfgType
	 */
	public String getCfgType() {
		return cfgType;
	}

	/**
	 * @param cfgType the cfgType to set
	 */
	public void setCfgType(String cfgType) {
		this.cfgType = cfgType;
	}
	
}
