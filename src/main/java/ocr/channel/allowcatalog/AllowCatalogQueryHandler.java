package ocr.channel.allowcatalog;

import ocr.common.handler.SampleDocQueryHandler;
import otocloud.framework.app.function.AppActivityImpl;



/**
 * TODO: 允销目录查询
 * @date 2016年11月15日
 * @author lijing
 */
public class AllowCatalogQueryHandler extends SampleDocQueryHandler {
	
	public static final String ADDRESS = "find_pagination";

	public AllowCatalogQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}
	
}
