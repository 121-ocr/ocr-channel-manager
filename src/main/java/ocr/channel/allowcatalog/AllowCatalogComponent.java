package ocr.channel.allowcatalog;


import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * TODO:允销目录管理
 * @date 2016年11月15日
 * @author lijing
 */
public class AllowCatalogComponent extends AppActivityImpl {

	//业务活动组件名
	@Override
	public String getName() {
		return "allowcatalog-mgr";
	}
	
	//业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {		
		return "bs_allow_catalog";
	}

	//发布此业务活动对外暴露的业务事件
	@Override
	public List<OtoCloudEventDescriptor> exposeOutboundBizEventsDesc() {
		// TODO Auto-generated method stub
		return null;
	}


	//业务活动组件中的业务功能
	@Override
	public List<OtoCloudEventHandlerRegistry> registerEventHandlers() {

		List<OtoCloudEventHandlerRegistry> ret = new ArrayList<OtoCloudEventHandlerRegistry>();

		AllowCatalogQueryHandler allowCatalogQueryHandler = new AllowCatalogQueryHandler(this);
		ret.add(allowCatalogQueryHandler);
		
		return ret;
	}

}
