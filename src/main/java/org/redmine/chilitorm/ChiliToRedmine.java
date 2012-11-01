package org.redmine.chilitorm;

import java.io.File;

public class ChiliToRedmine {

	
	public static void main(String[] args) {
		
		if(args == null || args.length == 0 || args[0] == null || args[0].trim().length() == 0) {
			System.err.println("Path to redmine's \"database.yml\" must be passed as an argument");
			System.exit(-1);
		}
		File cfgfile = new File(args[0].trim());
		String cfgType = "production";
		if(args.length > 1 && args[1] != null && args[1].trim().length() > 0) {
			cfgType = args[1].trim();
		}

		System.out.println(String.format("\nConvertring '%s' from file '%s'", cfgType, cfgfile));

		ChiliToRmConvertor cnv = new ChiliToRmConvertor();
		cnv.setDataSource(new DataSourceCreator(cfgfile, cfgType).getDataSource());
		
		cnv.doConvert();
		
	}
}
