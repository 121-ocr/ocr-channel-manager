package ocr.channel;

import java.util.ArrayList;
import java.util.List;

import ocr.channel.allowcatalog.AllowCatalogComponent;
import ocr.channel.organization.ChannelOrganizationComponent;
import ocr.channel.pricepolicy.PricePolicyComponent;
import ocr.channel.supplyrelation.SupplyRelationComponent;
import otocloud.framework.app.engine.AppServiceImpl;
import otocloud.framework.app.engine.WebServer;
import otocloud.framework.app.function.AppActivity;
import otocloud.framework.app.function.AppInitActivityImpl;

/**
 * TODO: 渠道管理微服务
 * @date 2016年11月26日
 * @author lijing@yonyou.com
 */
public class ChannelsManagerService extends AppServiceImpl
{

	//创建服务初始化组件
	@Override
	public AppInitActivityImpl createAppInitActivity() {		
		return null;
	}

	//创建租户级web server
	@Override
	public WebServer createWebServer() {
		// TODO Auto-generated method stub
		return null;
	}

	//创建服务内的业务活动组件
	@Override
	public List<AppActivity> createBizActivities() {
		List<AppActivity> retActivities = new ArrayList<>();		
	
		ChannelOrganizationComponent channelOrganizationComponent = new ChannelOrganizationComponent();
		retActivities.add(channelOrganizationComponent);
		
		SupplyRelationComponent supplyRelationComponent = new SupplyRelationComponent();
		retActivities.add(supplyRelationComponent);
		
		PricePolicyComponent pricePolicyComponent = new PricePolicyComponent();
		retActivities.add(pricePolicyComponent);
		
		AllowCatalogComponent allowCatalogComponent = new AllowCatalogComponent();
		retActivities.add(allowCatalogComponent);

		return retActivities;
	}
}
